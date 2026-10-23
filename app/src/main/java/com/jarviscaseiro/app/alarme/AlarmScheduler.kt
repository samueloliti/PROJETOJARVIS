package com.jarviscaseiro.app.alarme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Agenda e cancela alarmes exatos.
 *
 * Formato de parâmetros aceito no campo `conteudo` do comando `alarme`:
 *   { "horario": "HH:mm" }       -> toca hoje (ou amanhã, se já passou) nesse horário
 *   { "atraso_minutos": 10 }     -> toca daqui a N minutos
 *   { "recorrente_diario": true } com "horario" -> reagenda todo dia no mesmo horário
 *
 * Em Android 12+ é necessário SCHEDULE_EXACT_ALARM concedido; se não houver
 * permissão, cai para um alarme não exato (pode atrasar alguns minutos) e isso
 * é reportado de volta via Firebase.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun podeAgendarExato(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun agendar(
        comandoId: String,
        horario: String?,
        atrasoMinutos: Int?,
        recorrenteDiario: Boolean
    ): Long {
        val calendar = Calendar.getInstance()

        when {
            atrasoMinutos != null -> {
                calendar.add(Calendar.MINUTE, atrasoMinutos)
            }
            horario != null -> {
                val partes = horario.split(":")
                val hora = partes.getOrNull(0)?.toIntOrNull() ?: 7
                val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hora)
                calendar.set(Calendar.MINUTE, minuto)
                calendar.set(Calendar.SECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            else -> {
                // padrão de segurança: 1 minuto no futuro, para nunca disparar no passado
                calendar.add(Calendar.MINUTE, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_COMANDO_ID, comandoId)
            putExtra(AlarmReceiver.EXTRA_RECORRENTE, recorrenteDiario)
            putExtra(AlarmReceiver.EXTRA_HORARIO, horario)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, comandoId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (podeAgendarExato()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para alarme exato: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }

        AlarmStore(context).salvar(comandoId, horario, recorrenteDiario)
        return calendar.timeInMillis
    }

    fun cancelar(comandoId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, comandoId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        AlarmStore(context).remover(comandoId)
    }

    /** Cancela TODOS os alarmes conhecidos (usado pelo comando cancelar_alarme sem ID específico). */
    fun cancelarTodosConhecidos(ids: Collection<String>) {
        ids.forEach { cancelar(it) }
    }

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
