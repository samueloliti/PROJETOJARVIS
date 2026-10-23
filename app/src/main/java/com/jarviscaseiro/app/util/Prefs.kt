package com.jarviscaseiro.app.util

import android.content.Context
import java.util.UUID

/**
 * Armazenamento simples de configuração local do Jarvis.
 * Guarda: ID do dispositivo (configurável, não usa nome comercial do aparelho),
 * lista de comandos já executados (anti-replay) e apps permitidos (whitelist).
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    var deviceId: String
        get() {
            var id = sp.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = "celular-" + UUID.randomUUID().toString().substring(0, 6)
                sp.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) {
            sp.edit().putString(KEY_DEVICE_ID, value).apply()
        }

    var grupo: String
        get() = sp.getString(KEY_GRUPO, "default") ?: "default"
        set(value) = sp.edit().putString(KEY_GRUPO, value).apply()

    var servicoAtivo: Boolean
        get() = sp.getBoolean(KEY_SERVICO_ATIVO, false)
        set(value) = sp.edit().putBoolean(KEY_SERVICO_ATIVO, value).apply()

    /** Apps que o comando `abrir_app` tem permissão de abrir (package names), separados por vírgula. */
    var appsPermitidos: Set<String>
        get() = sp.getStringSet(KEY_APPS_PERMITIDOS, emptySet()) ?: emptySet()
        set(value) = sp.edit().putStringSet(KEY_APPS_PERMITIDOS, value).apply()

    /** Duração máxima padrão (segundos) do apito, caso o comando não informe. */
    var duracaoMaximaApitoSegundos: Int
        get() = sp.getInt(KEY_DURACAO_MAX_APITO, 120)
        set(value) = sp.edit().putInt(KEY_DURACAO_MAX_APITO, value).apply()

    // --- Anti-replay: últimos IDs de comando já processados ---
    fun jaExecutou(comandoId: String): Boolean {
        return sp.getStringSet(KEY_COMANDOS_EXECUTADOS, emptySet())?.contains(comandoId) == true
    }

    fun marcarExecutado(comandoId: String) {
        val atuais = sp.getStringSet(KEY_COMANDOS_EXECUTADOS, emptySet())?.toMutableSet() ?: mutableSetOf()
        atuais.add(comandoId)
        // mantém só os últimos 200 para não crescer indefinidamente
        val limitados = if (atuais.size > 200) atuais.toList().takeLast(200).toMutableSet() else atuais
        sp.edit().putStringSet(KEY_COMANDOS_EXECUTADOS, limitados).apply()
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_GRUPO = "grupo"
        private const val KEY_SERVICO_ATIVO = "servico_ativo"
        private const val KEY_APPS_PERMITIDOS = "apps_permitidos"
        private const val KEY_DURACAO_MAX_APITO = "duracao_max_apito"
        private const val KEY_COMANDOS_EXECUTADOS = "comandos_executados"
    }
}
