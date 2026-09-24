// Package wacheck expone a Kotlin (vía gomobile bind) lo mínimo de whatsmeow que necesita WhatsCheck:
// vincular con código de emparejamiento, consultar si un número tiene WhatsApp, cerrar y desvincular.
//
// Solo cruzan el puente tipos simples (string, int64, bool, error). La traducción de la respuesta a
// HAS_WHATSAPP / NO_WHATSAPP / UNKNOWN la hace Kotlin con toCheckResult (mismo contrato que baileys-cli).
//
// Configuración obligatoria de la sesión (tech-stack §2):
//   - nunca se llama a SendPresence → no aparece "en línea" y los recibos de entrega son "inactive";
//   - ManualHistorySyncDownload = true → no se descarga el historial de chats;
//   - no hay manejador de mensajes: lo que llega se descarta; nunca se llama a MarkRead.
package wacheck

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"

	"go.mau.fi/whatsmeow"
	waCompanionReg "go.mau.fi/whatsmeow/proto/waCompanionReg"
	"go.mau.fi/whatsmeow/store"
	"go.mau.fi/whatsmeow/store/sqlstore"
	"go.mau.fi/whatsmeow/types/events"
	waLog "go.mau.fi/whatsmeow/util/log"
	_ "modernc.org/sqlite" // SQLite en Go puro: no necesita cgo para compilar a Android
)

// PairDisplayName es el nombre que se envía al vincular por código. WhatsApp exige el formato
// "Navegador (Sistema)" con valores comunes y rechaza los demás (ver PairPhone en whatsmeow e IF-009).
const PairDisplayName = "Chrome (Linux)"

// Estados que lee Kotlin (coinciden con SessionState de engine-api).
const (
	StateUnlinked   = "UNLINKED"
	StateConnecting = "CONNECTING"
	StateReady      = "READY"
	StateLoggedOut  = "LOGGED_OUT"
	StateBanned     = "BANNED"
	StateClosed     = "CLOSED"
)

func init() {
	// Identidad del dispositivo en el registro: el teléfono lo muestra como "Google Chrome (WhatsCheck)",
	// también al vincular por código (PairDisplayName solo se valida, no se muestra; ver IF-009).
	store.SetOSInfo("WhatsCheck", [3]uint32{0, 1, 0})
	store.DeviceProps.PlatformType = waCompanionReg.DeviceProps_CHROME.Enum()
}

// Engine envuelve un cliente de whatsmeow con la sesión guardada en un SQLite dentro de dataDir.
type Engine struct {
	dbPath    string
	db        *sql.DB
	container *sqlstore.Container
	client    *whatsmeow.Client

	mu        sync.Mutex
	state     string
	lastError string
}

// NewEngine abre (o crea) la sesión guardada en dataDir. Es la parte de "arranque del motor" que mide tEngine.
func NewEngine(dataDir string) (*Engine, error) {
	if err := os.MkdirAll(dataDir, 0o700); err != nil {
		return nil, err
	}
	dbPath := filepath.Join(dataDir, "whatsmeow.db")
	db, err := sql.Open("sqlite", "file:"+dbPath+"?_pragma=foreign_keys(1)&_pragma=busy_timeout(5000)&_pragma=journal_mode(WAL)")
	if err != nil {
		return nil, err
	}
	ctx := context.Background()
	container := sqlstore.NewWithDB(db, "sqlite3", waLog.Noop)
	if err := container.Upgrade(ctx); err != nil {
		_ = db.Close()
		return nil, fmt.Errorf("upgrade del store: %w", err)
	}
	device, err := container.GetFirstDevice(ctx) // crea uno nuevo si no hay sesión
	if err != nil {
		_ = db.Close()
		return nil, err
	}
	// Minimización de datos: ni agenda, ni ajustes de chats, ni secretos de mensajes (nostore.go)
	minimizeStore(device)
	if err := purgeTables(ctx, db, purgedTables); err != nil {
		_ = db.Close()
		return nil, err
	}
	e := &Engine{dbPath: dbPath, db: db, container: container}
	e.client = e.newClient(device)
	if device.ID == nil {
		e.state = StateUnlinked
	} else {
		e.state = StateClosed
	}
	return e, nil
}

func (e *Engine) newClient(device *store.Device) *whatsmeow.Client {
	client := whatsmeow.NewClient(device, waLog.Noop)
	client.ManualHistorySyncDownload = true
	client.AddEventHandler(e.handleEvent)
	return client
}

func (e *Engine) handleEvent(evt any) {
	e.mu.Lock()
	defer e.mu.Unlock()
	switch v := evt.(type) {
	case *events.Connected:
		e.state = StateReady
	case *events.PairSuccess:
		e.state = StateConnecting
	case *events.PairError:
		e.lastError = "pair error: " + v.Error.Error()
	case *events.LoggedOut:
		e.state = StateLoggedOut
		e.lastError = fmt.Sprintf("logged out: %v", v.Reason)
	case *events.TemporaryBan:
		e.state = StateBanned
		e.lastError = v.String()
	case *events.ConnectFailure:
		e.lastError = fmt.Sprintf("connect failure: %v %s", v.Reason, v.Message)
		if v.Reason.IsLoggedOut() {
			e.state = StateLoggedOut
		}
	case *events.Disconnected:
		if e.state == StateReady {
			e.state = StateClosed
		}
	}
}

// State devuelve el estado actual de la sesión (ver constantes State*).
func (e *Engine) State() string {
	e.mu.Lock()
	defer e.mu.Unlock()
	return e.state
}

// LastError devuelve el último error informado por whatsmeow (vacío si no hubo).
func (e *Engine) LastError() string {
	e.mu.Lock()
	defer e.mu.Unlock()
	return e.lastError
}

func (e *Engine) setState(s string) {
	e.mu.Lock()
	e.state = s
	e.mu.Unlock()
}

// IsLinked indica si hay una sesión emparejada guardada.
func (e *Engine) IsLinked() bool {
	return e.client.Store.ID != nil
}

// LinkedPhone devuelve los dígitos del número de la cuenta vinculada (p. ej. "51987654321"), o "" si no hay sesión.
func (e *Engine) LinkedPhone() string {
	if id := e.client.Store.ID; id != nil {
		return id.User
	}
	return ""
}

// IsConnected indica si la conexión está abierta y con sesión iniciada.
func (e *Engine) IsConnected() bool {
	return e.client.IsConnected() && e.client.IsLoggedIn()
}

// RequestPairingCode conecta sin sesión, espera a que WhatsApp esté listo y pide el código de emparejamiento.
// La vinculación termina cuando State() pasa a READY (el usuario escribe el código en el teléfono).
func (e *Engine) RequestPairingCode(phoneDigits string, timeoutMs int64) (string, error) {
	if e.IsLinked() {
		return "", errors.New("ya hay una sesión vinculada")
	}
	qrChan, err := e.client.GetQRChannel(context.Background())
	if err != nil {
		return "", err
	}
	e.setState(StateConnecting)
	if err := e.client.Connect(); err != nil {
		return "", err
	}
	select {
	case evt := <-qrChan:
		if evt.Event != whatsmeow.QRChannelEventCode {
			return "", fmt.Errorf("evento inesperado antes del código: %s", evt.Event)
		}
	case <-time.After(time.Duration(timeoutMs) * time.Millisecond):
		return "", errors.New("timeout esperando la conexión para vincular")
	}
	// Seguir leyendo el canal para que whatsmeow no se bloquee; el resultado llega por handleEvent.
	go func() {
		for evt := range qrChan {
			if evt.Event == "timeout" {
				e.mu.Lock()
				e.lastError = "el código de emparejamiento expiró"
				e.mu.Unlock()
			}
		}
	}()
	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(timeoutMs)*time.Millisecond)
	defer cancel()
	return e.client.PairPhone(ctx, "+"+phoneDigits, true, whatsmeow.PairClientChrome, PairDisplayName)
}

// Connect abre la sesión guardada (conexión en frío) y espera a que esté lista para consultar.
func (e *Engine) Connect(timeoutMs int64) error {
	if !e.IsLinked() {
		return errors.New("no hay ninguna sesión vinculada")
	}
	if e.IsConnected() { // p. ej. justo después de vincular
		e.setState(StateReady)
		return nil
	}
	e.setState(StateConnecting)
	if err := e.client.Connect(); err != nil {
		return err
	}
	if !e.client.WaitForConnection(time.Duration(timeoutMs) * time.Millisecond) {
		if last := e.LastError(); last != "" {
			return errors.New(last)
		}
		return errors.New("timeout: la sesión no quedó lista")
	}
	e.setState(StateReady)
	return nil
}

type presenceEntry struct {
	Jid    string `json:"jid"`
	Exists bool   `json:"exists"`
}

// Check consulta si el número (E.164 sin '+') tiene WhatsApp. Devuelve JSON con las entradas,
// p. ej. [{"jid":"51987654321@s.whatsapp.net","exists":true}]. Un error que empieza con "timeout"
// indica que la consulta no respondió a tiempo.
func (e *Engine) Check(phoneDigits string, timeoutMs int64) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(timeoutMs)*time.Millisecond)
	defer cancel()
	resp, err := e.client.IsOnWhatsApp(ctx, []string{"+" + phoneDigits})
	if err != nil {
		if errors.Is(err, context.DeadlineExceeded) {
			return "", fmt.Errorf("timeout: %w", err)
		}
		return "", err
	}
	entries := make([]presenceEntry, 0, len(resp))
	for _, r := range resp {
		entries = append(entries, presenceEntry{Jid: r.JID.String(), Exists: r.IsIn})
	}
	out, err := json.Marshal(entries)
	return string(out), err
}

// Close cierra la conexión sin cerrar la sesión en WhatsApp.
func (e *Engine) Close() {
	e.client.Disconnect()
	if e.IsLinked() {
		e.setState(StateClosed)
	}
}

// Unlink cierra la sesión en WhatsApp (el dispositivo desaparece de "Dispositivos vinculados")
// y borra la base de datos local.
func (e *Engine) Unlink(timeoutMs int64) error {
	if e.IsLinked() {
		if !e.IsConnected() {
			_ = e.Connect(timeoutMs) // si ya la cerraron desde el teléfono, igual se borra lo local
		}
		ctx, cancel := context.WithTimeout(context.Background(), time.Duration(timeoutMs)*time.Millisecond)
		defer cancel()
		_ = e.client.Logout(ctx)
	}
	e.client.Disconnect()
	_ = e.db.Close()
	for _, suffix := range []string{"", "-wal", "-shm"} {
		_ = os.Remove(e.dbPath + suffix)
	}
	e.setState(StateUnlinked)
	return nil
}

// Hold conecta la sesión guardada, la mantiene abierta durante seconds y la cierra, sin consultar nada.
// Sirve para observar desde otro WhatsApp si la sesión altera algo (presencia, lecturas, notificaciones).
// Devuelve JSON con la cantidad de eventos recibidos por tipo, sin su contenido: {"*events.Message": 1, ...}.
func (e *Engine) Hold(seconds int64) (string, error) {
	var mu sync.Mutex
	counts := map[string]int{}
	id := e.client.AddEventHandler(func(evt any) {
		mu.Lock()
		counts[fmt.Sprintf("%T", evt)]++
		mu.Unlock()
	})
	defer e.client.RemoveEventHandler(id)

	if err := e.Connect(15_000); err != nil {
		return "", err
	}
	time.Sleep(time.Duration(seconds) * time.Second)
	e.Close()

	mu.Lock()
	defer mu.Unlock()
	out, err := json.Marshal(counts)
	return string(out), err
}

// TableCounts devuelve JSON con la cantidad de filas de cada tabla de la base, sin su contenido.
// Sirve para verificar qué guarda whatsmeow en el teléfono (minimización de datos).
func (e *Engine) TableCounts() (string, error) {
	rows, err := e.db.Query(`SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name`)
	if err != nil {
		return "", err
	}
	var tables []string
	for rows.Next() {
		var name string
		if err := rows.Scan(&name); err != nil {
			rows.Close()
			return "", err
		}
		tables = append(tables, name)
	}
	rows.Close()

	counts := make(map[string]int64, len(tables))
	for _, t := range tables {
		var n int64
		// Los nombres salen de sqlite_master, no de una entrada externa.
		if err := e.db.QueryRow(`SELECT COUNT(*) FROM "` + t + `"`).Scan(&n); err != nil {
			return "", fmt.Errorf("contar %s: %w", t, err)
		}
		counts[t] = n
	}
	out, err := json.Marshal(counts)
	return string(out), err
}

// purgeTables vacía las tablas indicadas si existen (sin tocar las que falten).
func purgeTables(ctx context.Context, db *sql.DB, tables []string) error {
	for _, t := range tables {
		var exists int
		if err := db.QueryRowContext(ctx, `SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?`, t).Scan(&exists); err != nil {
			return err
		}
		if exists == 0 {
			continue
		}
		// Los nombres vienen de purgedTables (constantes del código), no de una entrada externa.
		if _, err := db.ExecContext(ctx, `DELETE FROM "`+t+`"`); err != nil {
			return fmt.Errorf("vaciar %s: %w", t, err)
		}
	}
	return nil
}

// Release libera la base de datos. Llamar al terminar con el motor.
func (e *Engine) Release() {
	e.client.Disconnect()
	_ = e.db.Close()
}
