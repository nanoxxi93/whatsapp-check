package wacheck

import (
	"strings"
	"testing"
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

func TestNombreDeVinculacionConFormatoNavegadorSistema(t *testing.T) {
	// WhatsApp exige "Navegador (Sistema)" con valores comunes (IF-009).
	if !strings.HasSuffix(PairDisplayName, ")") || !strings.Contains(PairDisplayName, " (") {
		t.Fatalf("PairDisplayName con formato inválido: %q", PairDisplayName)
	}
}
