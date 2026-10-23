package com.jarviscaseiro.app.voz

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Faz o aparelho falar um texto em voz alta (comando `falar`). */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var pronto = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                pronto = true
            } else {
                Log.e(TAG, "Falha ao iniciar TextToSpeech")
            }
        }
    }

    fun falar(texto: String) {
        if (!pronto || texto.isBlank()) return
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "jarvis_fala_${System.currentTimeMillis()}")
    }

    fun liberar() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
