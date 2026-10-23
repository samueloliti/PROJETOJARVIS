package com.jarviscaseiro.app.apps

import android.content.Context
import android.content.Intent
import android.util.Log
import com.jarviscaseiro.app.util.Prefs

/**
 * Abre outro app, mas SOMENTE se o pacote estiver na whitelist configurada
 * pelo próprio usuário em Prefs.appsPermitidos. Isso evita a promessa vaga de
 * "abrir qualquer app" e deixa explícito o que é permitido.
 */
class AppLauncher(private val context: Context) {

    fun abrir(pacote: String): Boolean {
        val permitidos = Prefs(context).appsPermitidos
        if (pacote !in permitidos) {
            Log.w(TAG, "Tentativa de abrir app fora da whitelist: $pacote")
            return false
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pacote) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir $pacote: ${e.message}")
            false
        }
    }

    fun abrirUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir URL $url: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "AppLauncher"
    }
}
