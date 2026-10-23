package com.jarviscaseiro.app.alarme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda localmente os alarmes agendados (id, horário, recorrência) para que
 * o BootReceiver consiga reagendá-los depois que o aparelho reinicia — o
 * AlarmManager perde todos os alarmes pendentes em um reboot.
 */
class AlarmStore(context: Context) {
    private val sp = context.getSharedPreferences("jarvis_alarmes", Context.MODE_PRIVATE)

    fun salvar(comandoId: String, horario: String?, recorrente: Boolean) {
        val obj = JSONObject()
        obj.put("id", comandoId)
        obj.put("horario", horario ?: JSONObject.NULL)
        obj.put("recorrente", recorrente)
        val atuais = carregarJson()
        // remove entrada antiga com mesmo id, se houver
        val novos = JSONArray()
        for (i in 0 until atuais.length()) {
            val item = atuais.getJSONObject(i)
            if (item.getString("id") != comandoId) novos.put(item)
        }
        novos.put(obj)
        sp.edit().putString(KEY, novos.toString()).apply()
    }

    fun remover(comandoId: String) {
        val atuais = carregarJson()
        val novos = JSONArray()
        for (i in 0 until atuais.length()) {
            val item = atuais.getJSONObject(i)
            if (item.getString("id") != comandoId) novos.put(item)
        }
        sp.edit().putString(KEY, novos.toString()).apply()
    }

    fun listarTodos(): List<Triple<String, String?, Boolean>> {
        val atuais = carregarJson()
        val lista = mutableListOf<Triple<String, String?, Boolean>>()
        for (i in 0 until atuais.length()) {
            val item = atuais.getJSONObject(i)
            val horario = if (item.isNull("horario")) null else item.getString("horario")
            lista.add(Triple(item.getString("id"), horario, item.getBoolean("recorrente")))
        }
        return lista
    }

    private fun carregarJson(): JSONArray {
        val texto = sp.getString(KEY, "[]") ?: "[]"
        return try { JSONArray(texto) } catch (e: Exception) { JSONArray() }
    }

    companion object {
        private const val KEY = "lista_alarmes"
    }
}
