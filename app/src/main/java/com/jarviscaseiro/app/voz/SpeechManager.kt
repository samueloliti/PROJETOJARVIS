package com.jarviscaseiro.app.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Ativa o reconhecimento de voz local por tempo limitado (comando `ouvir_comando`)
 * e devolve o texto transcrito. Precisa de RECORD_AUDIO concedido.
 * A ativação do microfone é visível ao usuário (ícone do sistema), não é oculta.
 */
class SpeechManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun ouvir(onResultado: (String?) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResultado(null)
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR").toString())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val lista = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onResultado(lista?.firstOrNull())
                liberar()
            }
            override fun onError(error: Int) {
                Log.e(TAG, "Erro no reconhecimento de voz: $error")
                onResultado(null)
                liberar()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }

    fun liberar() {
        recognizer?.destroy()
        recognizer = null
    }

    companion object {
        private const val TAG = "SpeechManager"
    }
}
