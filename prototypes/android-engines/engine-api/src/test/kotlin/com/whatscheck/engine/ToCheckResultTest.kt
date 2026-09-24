package com.whatscheck.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mismos casos que prototypes/baileys-cli/src/result.test.ts (CA-02). */
class ToCheckResultTest {
    private val n = "51987654321"
    private val jid = "$n@s.whatsapp.net"
    private fun open(entries: List<PresenceEntry>?) = QueryOutcome.Response(entries, sessionOpen = true)

    @Test fun `exists true da HAS_WHATSAPP`() {
        assertEquals(
            CheckResult(CheckStatus.HAS_WHATSAPP, jid = jid, jidMismatch = false),
            toCheckResult(n, open(listOf(PresenceEntry(jid, exists = true)))),
        )
    }

    @Test fun `lista vacia con sesion abierta da NO_WHATSAPP`() {
        assertEquals(CheckResult(CheckStatus.NO_WHATSAPP), toCheckResult(n, open(emptyList())))
    }

    @Test fun `exists false da NO_WHATSAPP`() {
        assertEquals(CheckStatus.NO_WHATSAPP, toCheckResult(n, open(listOf(PresenceEntry(jid, exists = false)))).status)
    }

    @Test fun `jid distinto con una sola entrada se acepta y se marca jidMismatch`() {
        val mx = "525512345678@s.whatsapp.net"
        assertEquals(
            CheckResult(CheckStatus.HAS_WHATSAPP, jid = mx, jidMismatch = true),
            toCheckResult("5215512345678", open(listOf(PresenceEntry(mx, exists = true)))),
        )
    }

    @Test fun `varias entradas usa la que coincide exactamente`() {
        val r = toCheckResult(n, open(listOf(PresenceEntry("51900000000@s.whatsapp.net", true), PresenceEntry(jid, true))))
        assertEquals(jid, r.jid)
        assertEquals(false, r.jidMismatch)
    }

    @Test fun `varias entradas sin coincidencia da UNKNOWN AMBIGUOUS`() {
        val r = toCheckResult(n, open(listOf(PresenceEntry("51900000000@s.whatsapp.net", true), PresenceEntry("51900000001@s.whatsapp.net", true))))
        assertEquals(CheckResult.unknown(UnknownReason.AMBIGUOUS), r)
    }

    @Test fun `respuesta nula da UNKNOWN NO_RESPONSE`() {
        assertEquals(CheckResult.unknown(UnknownReason.NO_RESPONSE), toCheckResult(n, open(null)))
    }

    @Test fun `error da UNKNOWN, nunca NO_WHATSAPP`() {
        assertEquals(CheckResult.unknown(UnknownReason.ERROR), toCheckResult(n, QueryOutcome.Failure(RuntimeException("boom"))))
    }

    @Test fun `timeout da UNKNOWN, nunca NO_WHATSAPP`() {
        assertEquals(CheckResult.unknown(UnknownReason.TIMEOUT), toCheckResult(n, QueryOutcome.Timeout))
    }

    @Test fun `lista vacia con sesion cerrada da UNKNOWN SESSION_CLOSED, nunca NO_WHATSAPP`() {
        assertEquals(
            CheckResult.unknown(UnknownReason.SESSION_CLOSED),
            toCheckResult(n, QueryOutcome.Response(emptyList(), sessionOpen = false)),
        )
    }

    @Test fun `jidUser ignora el sufijo de dispositivo`() {
        assertEquals(n, jidUser("$n:12@s.whatsapp.net"))
        assertEquals(n, jidUser(jid))
    }
}
