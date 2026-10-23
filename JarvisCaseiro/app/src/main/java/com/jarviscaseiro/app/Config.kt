package com.jarviscaseiro.app

/**
 * Configuração fixa do projeto. A URL do Realtime Database NÃO é um segredo
 * (a segurança vem das regras do banco + autenticação, ver database.rules.json
 * e o README), por isso pode ficar aqui no código.
 */
object Config {
    const val FIREBASE_DATABASE_URL = "https://projetojarvis-d2dfa-default-rtdb.firebaseio.com/"

    /** Comandos mais antigos que isso (minutos) são ignorados ao reconectar. */
    const val EXPIRACAO_COMANDO_MINUTOS = 15L
}
