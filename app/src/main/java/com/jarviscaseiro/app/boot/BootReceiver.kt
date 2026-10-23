package com.jarviscaseiro.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarviscaseiro.app.alarme.AlarmScheduler
import com.jarviscaseiro.app.alarme.AlarmStore
import com.jarviscaseiro.app.service.JarvisForegroundService
import com.jarviscaseiro.app.util.Prefs

/**
 * Após reiniciar o aparelho:
 *  1. Reagenda todos os alarmes que estavam ativos (o AlarmManager os perde no reboot).
 *  2. Religa o serviço em primeiro plano, SE o usuário tinha deixado o Jarvis ativo
 *     antes de reiniciar (não liga automaticamente se o usuário tinha parado o serviço).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scheduler = AlarmScheduler(context)
        AlarmStore(context).listarTodos().forEach { (id, horario, recorrente) ->
            scheduler.agendar(comandoId = id, horario = horario, atrasoMinutos = null, recorrenteDiario = recorrente)
        }

        val prefs = Prefs(context)
        if (prefs.servicoAtivo) {
            JarvisForegroundService.iniciar(context)
        }
    }
}
