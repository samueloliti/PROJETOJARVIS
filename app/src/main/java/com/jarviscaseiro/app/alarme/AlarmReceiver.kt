package com.jarviscaseiro.app.alarme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Recebido pelo AlarmManager na hora marcada. Abre a tela de alarme em tela cheia
 * (funciona mesmo com o aparelho bloqueado, graças a showWhenLocked/turnScreenOn
 * na Activity) e, se for recorrente diário, reagenda para o dia seguinte.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val comandoId = intent.getStringExtra(EXTRA_COMANDO_ID) ?: return
        val recorrente = intent.getBooleanExtra(EXTRA_RECORRENTE, false)
        val horario = intent.getStringExtra(EXTRA_HORARIO)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_COMANDO_ID, comandoId)
        }
        context.startActivity(fullScreenIntent)

        if (recorrente && horario != null) {
            AlarmScheduler(context).agendar(
                comandoId = comandoId,
                horario = horario,
                atrasoMinutos = null,
                recorrenteDiario = true
            )
        }
    }

    companion object {
        const val EXTRA_COMANDO_ID = "comando_id"
        const val EXTRA_RECORRENTE = "recorrente"
        const val EXTRA_HORARIO = "horario"
    }
}
