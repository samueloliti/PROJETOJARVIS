package com.jarviscaseiro.app.apito

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Toca um som repetitivo e audível até receber ordem de parar, com uma duração
 * máxima de segurança (configurável) para nunca ficar preso indefinidamente
 * por falha de conexão ou erro do app.
 *
 * Usa o volume de notificação/alarme do próprio aparelho (não força volume máximo
 * e não mexe nos controles de volume do usuário).
 */
class ApitoManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnableSeguranca: Runnable? = null
    private var tocando = false

    fun iniciar(duracaoMaximaSegundos: Int, onFimPorSeguranca: (() -> Unit)? = null) {
        parar() // garante que não há outro tocando

        try {
            val uriSom = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uriSom)
                isLooping = true
                prepare()
                start()
            }
            tocando = true

            val duracaoMs = duracaoMaximaSegundos.coerceIn(5, 3600) * 1000L
            runnableSeguranca = Runnable {
                Log.w(TAG, "Duração máxima de segurança atingida, parando apito automaticamente")
                parar()
                onFimPorSeguranca?.invoke()
            }
            handler.postDelayed(runnableSeguranca!!, duracaoMs)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar apito: ${e.message}")
            tocando = false
        }
    }

    /** Pode (e deve) ser chamado mesmo que `iniciar` nunca tenha sido bem-sucedido. */
    fun parar() {
        runnableSeguranca?.let { handler.removeCallbacks(it) }
        runnableSeguranca = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar apito: ${e.message}")
        } finally {
            mediaPlayer = null
            tocando = false
        }
    }

    fun estaTocando() = tocando

    companion object {
        private const val TAG = "ApitoManager"
    }
}
