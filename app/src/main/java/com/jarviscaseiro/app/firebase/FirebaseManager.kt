package com.jarviscaseiro.app.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jarviscaseiro.app.comandos.Comando
import com.jarviscaseiro.app.comandos.StatusComando

/**
 * Centraliza toda a comunicação com o Firebase Realtime Database.
 *
 * Estrutura usada:
 *   /comandos/{id}          -> comandos enviados por qualquer dispositivo
 *   /respostas/{id}         -> respostas/resultados (status, imagens, texto)
 *   /dispositivos/{id}      -> presença/último status de cada aparelho (opcional)
 *
 * Observação de segurança: a URL do banco já existe e é usada como veio informado.
 * As regras de acesso (quem pode ler/escrever) ficam no arquivo database.rules.json
 * deste projeto — elas PRECISAM ser publicadas manualmente no console do Firebase,
 * isso não é algo que o app consegue fazer sozinho.
 */
class FirebaseManager(private val databaseUrl: String) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(databaseUrl)
    private var comandosRef: DatabaseReference = database.getReference("comandos")
    private var conexaoRef: DatabaseReference = database.getReference(".info/connected")
    private val auth = FirebaseAuth.getInstance()

    private var listenerComandos: ChildEventListener? = null
    private var listenerConexao: ValueEventListener? = null

    init {
        garantirAutenticacao()
    }

    /**
     * As regras do Firebase (database.rules.json) exigem auth != null para
     * ler/escrever. Usamos autenticação anônima — é gratuita e não exige login
     * do usuário, só precisa estar habilitada no console (Authentication ->
     * Sign-in method -> Anonymous). A página jarvis.html precisa fazer o mesmo
     * com o SDK JS do Firebase antes de ler/escrever em /comandos.
     */
    private fun garantirAutenticacao() {
        if (auth.currentUser != null) return
        auth.signInAnonymously()
            .addOnFailureListener { e -> Log.e(TAG, "Falha na autenticação anônima: ${e.message}") }
    }

    fun escutarConexao(onMudanca: (Boolean) -> Unit) {
        listenerConexao?.let { conexaoRef.removeEventListener(it) }
        listenerConexao = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conectado = snapshot.getValue(Boolean::class.java) ?: false
                onMudanca(conectado)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao observar conexão: ${error.message}")
                onMudanca(false)
            }
        }
        conexaoRef.addValueEventListener(listenerConexao!!)
    }

    /**
     * Escuta apenas comandos ADICIONADOS a partir de agora (childAdded cobre também
     * comandos perdidos enquanto offline, por isso o chamador deve filtrar por
     * anti-replay e por expiração de horário).
     */
    fun escutarComandos(onNovoComando: (Comando) -> Unit) {
        pararDeEscutarComandos()
        listenerComandos = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val comando = parseComando(snapshot) ?: return
                onNovoComando(comando)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao escutar comandos: ${error.message}")
            }
        }
        comandosRef.addChildEventListener(listenerComandos!!)
    }

    fun pararDeEscutarComandos() {
        listenerComandos?.let { comandosRef.removeEventListener(it) }
        listenerComandos = null
    }

    private fun parseComando(snapshot: DataSnapshot): Comando? {
        return try {
            val tipo = snapshot.child("tipo").getValue(String::class.java) ?: return null
            val origem = snapshot.child("origem").getValue(String::class.java) ?: ""
            val destino = snapshot.child("destino").getValue(String::class.java) ?: "todos"
            val hora = snapshot.child("hora").getValue(Long::class.java) ?: 0L
            val conteudoSnap = snapshot.child("conteudo")
            val conteudo = mutableMapOf<String, Any?>()
            for (filho in conteudoSnap.children) {
                conteudo[filho.key ?: continue] = filho.value
            }
            Comando(
                id = snapshot.key ?: return null,
                tipo = tipo,
                conteudo = conteudo,
                origem = origem,
                destino = destino,
                hora = hora
            )
        } catch (e: Exception) {
            Log.e(TAG, "Comando malformado (${snapshot.key}): ${e.message}")
            null
        }
    }

    /** Atualiza o status de execução de um comando em /respostas/{id}. */
    fun atualizarStatus(
        comando: Comando,
        status: StatusComando,
        mensagem: String? = null,
        dadosExtras: Map<String, Any?> = emptyMap()
    ) {
        val respostaRef = database.getReference("respostas").child(comando.id)
        val dados = mutableMapOf<String, Any?>(
            "status" to status.valor,
            "origem_comando" to comando.origem,
            "destino_comando" to comando.destino,
            "tipo" to comando.tipo,
            "respondidoEm" to ServerValue.TIMESTAMP
        )
        if (mensagem != null) dados["mensagem"] = mensagem
        dados.putAll(dadosExtras)
        respostaRef.updateChildren(dados)
    }

    /** Envia uma imagem (Base64 já comprimida) como resposta a um comando de foto. */
    fun enviarImagem(comando: Comando, base64Imagem: String, camera: String) {
        val respostaRef = database.getReference("respostas").child(comando.id)
        respostaRef.updateChildren(
            mapOf(
                "status" to StatusComando.CONCLUIDO.valor,
                "origem_comando" to comando.origem,
                "destino_comando" to comando.destino,
                "tipo" to comando.tipo,
                "camera" to camera,
                "imagemBase64" to base64Imagem,
                "respondidoEm" to ServerValue.TIMESTAMP
            )
        )
    }

    fun publicarStatusDispositivo(deviceId: String, dados: Map<String, Any?>) {
        database.getReference("dispositivos").child(deviceId).updateChildren(dados)
    }

    companion object {
        private const val TAG = "FirebaseManager"
    }
}
