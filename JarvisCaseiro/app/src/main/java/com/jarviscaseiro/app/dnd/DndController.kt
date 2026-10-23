package com.jarviscaseiro.app.dnd

import android.app.NotificationManager
import android.content.Context
import android.util.Log

/**
 * Liga/desliga o modo Não Perturbe. Requer que o usuário tenha concedido
 * "Acesso a política de notificações" manualmente (não dá pra pedir isso
 * em runtime como uma permissão normal — leva para a tela de configuração).
 */
class DndController(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun temPermissao(): Boolean = manager.isNotificationPolicyAccessGranted

    fun ligar(): Boolean {
        if (!temPermissao()) return false
        return try {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ligar Não Perturbe: ${e.message}")
            false
        }
    }

    fun desligar(): Boolean {
        if (!temPermissao()) return false
        return try {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desligar Não Perturbe: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "DndController"
    }
}
