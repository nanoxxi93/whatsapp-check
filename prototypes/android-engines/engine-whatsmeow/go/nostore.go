package wacheck

import (
	"context"
	"time"

	"go.mau.fi/whatsmeow/store"
	"go.mau.fi/whatsmeow/types"
)

// Minimización de datos (spec 2026-09-23-efectos-whatsapp-principal, T5).
//
// Al vincular, el teléfono principal comparte las llaves del "estado de la app" y whatsmeow descarga
// y guarda la agenda (nombres de contactos), los ajustes de cada chat y un secreto por cada mensaje
// recibido. WhatsCheck solo necesita saber si un número tiene WhatsApp, así que esos tres stores se
// reemplazan por versiones que no guardan nada. Las sesiones cifradas (Sessions, Identities, PreKeys)
// sí se conservan: sin ellas no se podrían confirmar las entregas y el remitente reintentaría el envío.

type noContacts struct{}

var _ store.ContactStore = noContacts{}

func (noContacts) PutPushName(context.Context, types.JID, string) (bool, string, error) {
	return false, "", nil
}
func (noContacts) PutBusinessName(context.Context, types.JID, string) (bool, string, error) {
	return false, "", nil
}
func (noContacts) PutContactName(context.Context, types.JID, string, string) error { return nil }
func (noContacts) PutAllContactNames(context.Context, []store.ContactEntry) error  { return nil }
func (noContacts) PutManyRedactedPhones(context.Context, []store.RedactedPhoneEntry) error {
	return nil
}
func (noContacts) GetContact(context.Context, types.JID) (types.ContactInfo, error) {
	return types.ContactInfo{}, nil
}
func (noContacts) GetAllContacts(context.Context) (map[types.JID]types.ContactInfo, error) {
	return map[types.JID]types.ContactInfo{}, nil
}

type noChatSettings struct{}

var _ store.ChatSettingsStore = noChatSettings{}

func (noChatSettings) PutMutedUntil(context.Context, types.JID, time.Time) error { return nil }
func (noChatSettings) PutPinned(context.Context, types.JID, bool) error          { return nil }
func (noChatSettings) PutArchived(context.Context, types.JID, bool) error        { return nil }
func (noChatSettings) GetChatSettings(context.Context, types.JID) (types.LocalChatSettings, error) {
	return types.LocalChatSettings{}, nil
}

type noMsgSecrets struct{}

var _ store.MsgSecretStore = noMsgSecrets{}

func (noMsgSecrets) PutMessageSecrets(context.Context, []store.MessageSecretInsert) error { return nil }
func (noMsgSecrets) PutMessageSecret(context.Context, types.JID, types.JID, types.MessageID, []byte) error {
	return nil
}
func (noMsgSecrets) GetMessageSecret(context.Context, types.JID, types.JID, types.MessageID) ([]byte, types.JID, error) {
	return nil, types.EmptyJID, nil
}

// minimizeStore instala los stores que no guardan nada.
func minimizeStore(device *store.Device) {
	device.Contacts = noContacts{}
	device.ChatSettings = noChatSettings{}
	device.MsgSecrets = noMsgSecrets{}
}

// purgedTables son las tablas que dejan de usarse con minimizeStore; se vacían al abrir el motor
// para borrar lo que se haya guardado antes (p. ej. al vincular con una versión anterior).
var purgedTables = []string{"whatsmeow_contacts", "whatsmeow_chat_settings", "whatsmeow_message_secrets"}
