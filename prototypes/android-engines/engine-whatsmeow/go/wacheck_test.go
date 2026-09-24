package wacheck

import (
	"context"
	"database/sql"
	"encoding/json"
	"path/filepath"
	"strings"
	"testing"

	"go.mau.fi/whatsmeow/types"
)

func TestNewEngineSinSesion(t *testing.T) {
	e, err := NewEngine(t.TempDir())
	if err != nil {
		t.Fatalf("NewEngine: %v", err)
	}
	defer e.Release()
	if e.IsLinked() {
		t.Fatal("un store nuevo no debería tener sesión")
	}
	if got := e.State(); got != StateUnlinked {
		t.Fatalf("estado = %q, se esperaba %q", got, StateUnlinked)
	}
	if got := e.LinkedPhone(); got != "" {
		t.Fatalf("sin sesión, LinkedPhone debería ser vacío, dio %q", got)
	}
	if err := e.Connect(1000); err == nil || !strings.Contains(err.Error(), "no hay ninguna sesión") {
		t.Fatalf("Connect sin sesión debería fallar con un mensaje claro, dio: %v", err)
	}
}

func TestNewEngineReabreElMismoStore(t *testing.T) {
	dir := t.TempDir()
	e1, err := NewEngine(dir)
	if err != nil {
		t.Fatal(err)
	}
	e1.Release()
	e2, err := NewEngine(dir)
	if err != nil {
		t.Fatalf("reabrir el store: %v", err)
	}
	e2.Release()
}

func TestTableCountsStoreVacio(t *testing.T) {
	e, err := NewEngine(t.TempDir())
	if err != nil {
		t.Fatal(err)
	}
	defer e.Release()
	raw, err := e.TableCounts()
	if err != nil {
		t.Fatalf("TableCounts: %v", err)
	}
	var counts map[string]int64
	if err := json.Unmarshal([]byte(raw), &counts); err != nil {
		t.Fatalf("JSON inválido: %v (%s)", err, raw)
	}
	for _, tabla := range []string{"whatsmeow_device", "whatsmeow_contacts", "whatsmeow_event_buffer", "whatsmeow_sessions"} {
		n, ok := counts[tabla]
		if !ok {
			t.Fatalf("falta la tabla %s en %s", tabla, raw)
		}
		if n != 0 {
			t.Fatalf("un store nuevo debería tener %s vacía, tiene %d filas", tabla, n)
		}
	}
}

func TestHoldSinSesionFalla(t *testing.T) {
	e, err := NewEngine(t.TempDir())
	if err != nil {
		t.Fatal(err)
	}
	defer e.Release()
	if _, err := e.Hold(1); err == nil {
		t.Fatal("Hold sin sesión vinculada debería fallar")
	}
}

func TestNombreDeVinculacionConFormatoNavegadorSistema(t *testing.T) {
	// WhatsApp exige "Navegador (Sistema)" con valores comunes (IF-009).
	if !strings.HasSuffix(PairDisplayName, ")") || !strings.Contains(PairDisplayName, " (") {
		t.Fatalf("PairDisplayName con formato inválido: %q", PairDisplayName)
	}
}

func TestPurgeTablesVaciaSoloLasIndicadas(t *testing.T) {
	db, err := sql.Open("sqlite", "file:"+filepath.Join(t.TempDir(), "p.db"))
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	for _, q := range []string{
		`CREATE TABLE whatsmeow_contacts (their_jid TEXT)`,
		`CREATE TABLE whatsmeow_sessions (their_id TEXT)`,
		`INSERT INTO whatsmeow_contacts VALUES ('a'), ('b')`,
		`INSERT INTO whatsmeow_sessions VALUES ('s')`,
	} {
		if _, err := db.Exec(q); err != nil {
			t.Fatal(err)
		}
	}
	// whatsmeow_chat_settings no existe: no debe fallar
	if err := purgeTables(context.Background(), db, purgedTables); err != nil {
		t.Fatalf("purgeTables: %v", err)
	}
	count := func(table string) (n int) {
		if err := db.QueryRow(`SELECT COUNT(*) FROM "` + table + `"`).Scan(&n); err != nil {
			t.Fatal(err)
		}
		return
	}
	if n := count("whatsmeow_contacts"); n != 0 {
		t.Fatalf("contacts debería quedar vacía, tiene %d", n)
	}
	if n := count("whatsmeow_sessions"); n != 1 {
		t.Fatalf("sessions no debe tocarse, tiene %d", n)
	}
}

func TestStoresMinimizadosNoGuardanNada(t *testing.T) {
	e, err := NewEngine(t.TempDir())
	if err != nil {
		t.Fatal(err)
	}
	defer e.Release()
	ctx := context.Background()
	jid := types.NewJID("51987654321", types.DefaultUserServer)
	s := e.client.Store
	if err := s.Contacts.PutContactName(ctx, jid, "Nombre", "Nombre Completo"); err != nil {
		t.Fatal(err)
	}
	if info, _ := s.Contacts.GetContact(ctx, jid); info.Found || info.FullName != "" {
		t.Fatalf("el store de contactos no debería devolver nada: %+v", info)
	}
	if err := s.MsgSecrets.PutMessageSecret(ctx, jid, jid, "ID1", []byte("secreto")); err != nil {
		t.Fatal(err)
	}
	if secret, _, _ := s.MsgSecrets.GetMessageSecret(ctx, jid, jid, "ID1"); secret != nil {
		t.Fatal("el store de secretos no debería devolver nada")
	}
}
