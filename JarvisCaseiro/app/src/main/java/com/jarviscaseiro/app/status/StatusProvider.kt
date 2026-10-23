package com.jarviscaseiro.app.status

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager

/** Lê status de bateria e rede do aparelho (comandos status_bateria / status_rede). */
class StatusProvider(private val context: Context) {

    fun statusBateria(): Map<String, Any?> {
        val filtro = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val bateria = context.registerReceiver(null, filtro) ?: return emptyMap()

        val nivel = bateria.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = bateria.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentual = if (nivel >= 0 && escala > 0) (nivel * 100 / escala) else -1

        val status = bateria.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val carregando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val temperatura = bateria.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0

        return mapOf(
            "percentual" to percentual,
            "carregando" to carregando,
            "temperaturaCelsius" to temperatura
        )
    }

    fun statusRede(): Map<String, Any?> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val rede = cm.activeNetwork
        val caps = rede?.let { cm.getNetworkCapabilities(it) }

        val tipo = when {
            caps == null -> "sem_conexao"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "dados_moveis"
            else -> "outro"
        }

        return mapOf(
            "tipo" to tipo,
            "online" to (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false)
        )
    }
}
