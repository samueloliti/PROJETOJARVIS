package com.jarviscaseiro.app.alarme

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import com.jarviscaseiro.app.databinding.ActivityAlarmBinding

/**
 * Tela cheia exibida quando um alarme dispara. Toca som de alarme e vibra até
 * o usuário tocar em "Parar". Mostrada por cima da tela de bloqueio.
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tocarAlarme()
        vibrar()

        binding.btnPararAlarme.setOnClickListener {
            pararTudo()
            finish()
        }
    }

    private fun tocarAlarme() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, uri!!)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Se não conseguir tocar som, o alarme ainda aparece visualmente na tela.
        }
    }

    private fun vibrar() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val padrao = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(padrao, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(padrao, 0)
        }
    }

    private fun pararTudo() {
        mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        pararTudo()
        super.onDestroy()
    }
}
