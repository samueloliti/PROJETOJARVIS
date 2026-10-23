package com.jarviscaseiro.app.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jarviscaseiro.app.Config
import com.jarviscaseiro.app.comandos.Comando
import com.jarviscaseiro.app.comandos.StatusComando
import com.jarviscaseiro.app.databinding.ActivityCameraCapturaBinding
import com.jarviscaseiro.app.firebase.FirebaseManager

/**
 * Activity visível (não oculta) que executa os comandos de câmera.
 * É aberta em primeiro plano mesmo quando o app estava em background, o que
 * acende a tela e mostra o preview — é essa visibilidade que garante que a
 * captura nunca acontece de forma escondida do usuário.
 *
 * Trata: tirar_foto_traseira, tirar_foto_frontal, tirar_foto_ambas.
 */
class CameraCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraCapturaBinding
    private lateinit var firebase: FirebaseManager
    private lateinit var controller: CameraController

    private var comando: Comando? = null
    private var etapas: MutableList<Boolean> = mutableListOf() // false = traseira, true = frontal
    private var indiceEtapaAtual = 0
    private val imagensCapturadas = mutableListOf<Pair<String, Boolean>>() // base64, frontal?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCapturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebase = FirebaseManager(Config.FIREBASE_DATABASE_URL)
        controller = CameraController(this)

        val tipo = intent.getStringExtra(EXTRA_TIPO) ?: return finishComErro("Tipo de comando ausente")
        comando = Comando(
            id = intent.getStringExtra(EXTRA_COMANDO_ID) ?: return finishComErro("ID ausente"),
            tipo = tipo,
            origem = intent.getStringExtra(EXTRA_ORIGEM) ?: "",
            destino = intent.getStringExtra(EXTRA_DESTINO) ?: ""
        )

        etapas = when (tipo) {
            "tirar_foto_traseira" -> mutableListOf(false)
            "tirar_foto_frontal" -> mutableListOf(true)
            "tirar_foto_ambas" -> mutableListOf(false, true)
            else -> return finishComErro("Tipo de comando de câmera desconhecido: $tipo")
        }

        if (!temPermissaoCamera()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        } else {
            proximaEtapa()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                proximaEtapa()
            } else {
                finishComErro("Permissão de câmera negada")
            }
        }
    }

    private fun proximaEtapa() {
        val cmd = comando ?: return
        if (indiceEtapaAtual >= etapas.size) {
            enviarResultadoFinal()
            return
        }
        val usarFrontal = etapas[indiceEtapaAtual]
        binding.txtAvisoCaptura.text =
            "Jarvis: capturando foto ${if (usarFrontal) "frontal" else "traseira"}…"

        controller.capturar(
            lifecycleOwner = this,
            usarFrontal = usarFrontal,
            onSucesso = { base64 ->
                imagensCapturadas.add(base64 to usarFrontal)
                indiceEtapaAtual++
                proximaEtapa()
            },
            onErro = { erro ->
                firebase.atualizarStatus(cmd, StatusComando.ERRO, mensagem = erro)
                finishComErro(erro)
            }
        )
    }

    private fun enviarResultadoFinal() {
        val cmd = comando ?: return
        if (imagensCapturadas.isEmpty()) {
            firebase.atualizarStatus(cmd, StatusComando.ERRO, mensagem = "Nenhuma imagem capturada")
            finish()
            return
        }
        // Para "ambas", envia as duas sequencialmente usando o mesmo ID de comando
        // com um sufixo de câmera nos dados extras.
        imagensCapturadas.forEach { (base64, frontal) ->
            firebase.enviarImagem(cmd, base64, camera = if (frontal) "frontal" else "traseira")
        }
        binding.txtAvisoCaptura.text = "Jarvis: foto(s) enviada(s)."
        binding.root.postDelayed({ finish() }, 800)
    }

    private fun temPermissaoCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun finishComErro(msg: String) {
        binding.txtAvisoCaptura.text = "Jarvis: erro — $msg"
        binding.root.postDelayed({ finish() }, 1500)
    }

    companion object {
        private const val REQ_CAMERA = 9001
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_COMANDO_ID = "comando_id"
        const val EXTRA_ORIGEM = "origem"
        const val EXTRA_DESTINO = "destino"
    }
}
