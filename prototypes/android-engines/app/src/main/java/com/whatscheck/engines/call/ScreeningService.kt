package com.whatscheck.engines.call

import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Filtrado de llamadas (RF-02). Android lo invoca solo para números que no están en los contactos.
 * Con permiso de overlay: responde "permitir" al instante y verifica después, con el aviso visible.
 * Sin overlay (RF-05): verifica dentro del plazo de Telecom (máx. 4 s), responde y notifica.
 * Nunca silencia ni rechaza: la llamada siempre suena (principio "nunca estorbar la llamada").
 */
class ScreeningService : CallScreeningService() {

    private fun allow() = CallResponse.Builder().build()

    override fun onScreenCall(details: Call.Details) {
        val t0 = SystemClock.elapsedRealtime()
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, allow())
            return
        }
        val ctx = applicationContext
        val digits = CallCheck.normalize(details.handle?.schemeSpecificPart)
        val overlay = CallCheck.overlay(ctx)

        if (overlay.canShow()) {
            respondToCall(details, allow())
            val tScreen = SystemClock.elapsedRealtime() - t0
            val tOverlay = runCatching { overlay.show(OverlayController.Kind.CHECKING) - t0 }.getOrNull()
            CallCheck.scope.launch {
                val outcome = CallCheck.verify(ctx, digits)
                val shown = runCatching { overlay.show(CallCheck.kindOf(outcome.result), outcome.detail) }.getOrNull()
                val tResult = (shown ?: SystemClock.elapsedRealtime()) - t0
                Journal.call(ctx, CallCheck.record(ctx, digits, outcome, if (tOverlay != null) "overlay" else "overlay-fallido", t0, tScreen, tOverlay, tResult))
            }
        } else {
            CallCheck.scope.launch {
                val outcome = withTimeoutOrNull(NO_OVERLAY_BUDGET_MS) { CallCheck.verify(ctx, digits) }
                respondToCall(details, allow())
                val tScreen = SystemClock.elapsedRealtime() - t0
                val final = outcome ?: CallCheck.Outcome(
                    com.whatscheck.engine.CheckResult.unknown(com.whatscheck.engine.UnknownReason.TIMEOUT),
                    "sin resultado en ${NO_OVERLAY_BUDGET_MS / 1000} s", null, null, null,
                )
                Notifier.result(ctx, CallCheck.kindOf(final.result).text)
                val tResult = SystemClock.elapsedRealtime() - t0
                Journal.call(ctx, CallCheck.record(ctx, digits, final, "notificacion", t0, tScreen, null, tResult))
            }
        }
    }

    companion object {
        /** Telecom da ~5 s para responder; se deja margen. */
        const val NO_OVERLAY_BUDGET_MS = 4_000L
    }
}
