package com.jarviscaseiro.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        criarCanaisDeNotificacao()
    }

    private fun criarCanaisDeNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val canalServico = NotificationChannel(
            CANAL_SERVICO,
            getString(R.string.canal_servico),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificação persistente enquanto o Jarvis está escutando comandos"
        }

        val canalAlarme = NotificationChannel(
            CANAL_ALARME,
            getString(R.string.canal_alarme),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarmes e apitos remotos do Jarvis"
            enableVibration(true)
        }

        manager.createNotificationChannel(canalServico)
        manager.createNotificationChannel(canalAlarme)
    }

    companion object {
        const val CANAL_SERVICO = "jarvis_servico"
        const val CANAL_ALARME = "jarvis_alarme"
    }
}
