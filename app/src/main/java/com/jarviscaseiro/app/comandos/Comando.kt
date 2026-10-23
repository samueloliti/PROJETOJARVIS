package com.jarviscaseiro.app.comandos

/**
 * Modelo de um comando recebido pelo Firebase, em /comandos/{id}.
 *
 * Campos (formato combinado com a página jarvis.html):
 *   tipo:     String  - ação a executar (ex: "tirar_foto_traseira")
 *   conteudo: Map     - parâmetros livres da ação (ex: {"minutos": 10})
 *   origem:   String  - deviceId de quem enviou
 *   destino:  String  - deviceId alvo, "grupo:<nome>" ou "todos"
 *   hora:     Long    - timestamp de envio (millis), usado para expirar comandos antigos
 *   status:   String  - "recebido" | "executando" | "concluido" | "erro" (escrito de volta)
 */
data class Comando(
    val id: String = "",
    val tipo: String = "",
    val conteudo: Map<String, Any?> = emptyMap(),
    val origem: String = "",
    val destino: String = "",
    val hora: Long = 0L
) {
    fun destinadoPara(deviceId: String, grupo: String): Boolean {
        if (destino == "todos") return true
        if (destino == deviceId) return true
        if (destino == "grupo:$grupo") return true
        return false
    }

    /** Comando é considerado velho/expirado se foi enviado há mais de [maxMinutos] minutos. */
    fun expirado(maxMinutos: Long = 15): Boolean {
        if (hora <= 0L) return false
        val agora = System.currentTimeMillis()
        return (agora - hora) > maxMinutos * 60_000
    }
}

enum class StatusComando(val valor: String) {
    RECEBIDO("recebido"),
    EXECUTANDO("executando"),
    CONCLUIDO("concluido"),
    ERRO("erro"),
    IGNORADO("ignorado")
}
