package com.jarviscaseiro.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarviscaseiro.app.Config
import com.jarviscaseiro.app.JarvisApplication
import com.jarviscaseiro.app.MainActivity
import com.jarviscaseiro.app.R
import com.jarviscaseiro.app.comandos.CommandProcessor
import com.jarviscaseiro.app.firebase.FirebaseManager
import com.jarviscaseiro.app.util.Prefs

/**
 * Serviço em primeiro plano responsável por manter a escuta do Firebase ativa
 * mesmo com o app fora da tela principal, dentro das possibilidades do Android 13.
 * Mostra uma notificação persistente (obrigatória para foreground service).
 */
class JarvisForegroundService : Service() {

    private lateinit var firebase: FirebaseManager
    private lateinit var processor: CommandProcessor
    private lateinit var prefs: Prefs

    private var conectado = false

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        firebase = FirebaseManager(Config.FIREBASE_DATABASE_URL)
        processor = CommandProcessor(this, firebase)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarEmPrimeiroPlano()
        prefs.servicoAtivo = true

        firebase.escutarConexao { conectadoAgora ->
            conectado = conectadoAgora
            atualizarNotificacao()
            firebase.publicarStatusDispositivo(
                prefs.deviceId,
                mapOf("online" to conectadoAgora, "ultimaAtualizacao" to System.currentTimeMillis())
            )
        }

        firebase.escutarComandos { comando ->
            // Filtros obrigatórios: destino correto, não expirado, não executado antes.
            if (!comando.destinadoPara(prefs.deviceId, prefs.grupo)) return@escutarComandos
            if (comando.expirado(Config.EXPIRACAO_COMANDO_MINUTOS)) return@escutarComandos
            if (prefs.jaExecutou(comando.id)) return@escutarComandos

            processor.processar(comando)
        }

        return START_STICKY
    }

    private fun iniciarEmPrimeiroPlano() {
        val intentAbrirApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(this, JarvisApplication.CANAL_SERVICO)
            .setContentTitle(getString(R.string.notificacao_servico_titulo))
            .setContentText(getString(R.string.notificacao_servico_texto))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(intentAbrirApp)
            .build()

        // Os tipos de foreground service já estão declarados no AndroidManifest.xml
        // (camera|microphone|mediaPlayback|specialUse); em compileSdk/targetSdk 33
        // não é obrigatório repetir o bitmask aqui.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notificacao)
        } else {
            startForeground(NOTIFICATION_ID, notificacao)
        }
    }

    private fun atualizarNotificacao() {
        val texto = if (conectado) getString(R.string.status_conectado) else getString(R.string.status_desconectado)
        val notificacao = NotificationCompat.Builder(this, JarvisApplication.CANAL_SERVICO)
            .setContentTitle(getString(R.string.notificacao_servico_titulo))
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        val manager = ContextCompat.getSystemService(this, android.app.NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notificacao)
    }

    override fun onDestroy() {
        firebase.pararDeEscutarComandos()
        processor.liberar()
        prefs.servicoAtivo = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun iniciar(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun parar(context: Context) {
            context.stopService(Intent(context, JarvisForegroundService::class.java))
        }
    }
}
