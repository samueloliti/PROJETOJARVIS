package com.jarviscaseiro.app.comandos

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jarviscaseiro.app.JarvisApplication
import com.jarviscaseiro.app.apito.ApitoManager
import com.jarviscaseiro.app.apps.AppLauncher
import com.jarviscaseiro.app.alarme.AlarmScheduler
import com.jarviscaseiro.app.camera.CameraCaptureActivity
import com.jarviscaseiro.app.dnd.DndController
import com.jarviscaseiro.app.firebase.FirebaseManager
import com.jarviscaseiro.app.lanterna.LanternaController
import com.jarviscaseiro.app.status.StatusProvider
import com.jarviscaseiro.app.util.Prefs
import com.jarviscaseiro.app.voz.SpeechManager
import com.jarviscaseiro.app.voz.TtsManager

/**
 * Recebe um Comando já validado (destino correto, não expirado, não repetido)
 * e executa a ação correspondente. Cada ramo atualiza o status no Firebase
 * (recebido -> executando -> concluido/erro).
 */
class CommandProcessor(
    private val context: Context,
    private val firebase: FirebaseManager
) {
    private val prefs = Prefs(context)
    private val lanterna = LanternaController(context)
    private val apito = ApitoManager(context)
    private val appLauncher = AppLauncher(context)
    private val alarmScheduler = AlarmScheduler(context)
    private val statusProvider = StatusProvider(context)
    private val dnd = DndController(context)
    private val tts = TtsManager(context)

    /** Comandos de câmera são tratados à parte (abrem uma Activity visível). */
    val comandosDeCamera = setOf("tirar_foto_traseira", "tirar_foto_frontal", "tirar_foto_ambas")

    fun processar(comando: Comando) {
        firebase.atualizarStatus(comando, StatusComando.EXECUTANDO)
        LogDeComandos.registrar(comando)

        try {
            when (comando.tipo) {
                in comandosDeCamera -> abrirTelaDeCaptura(comando)

                "abrir_camera" -> abrirTelaDeCaptura(comando, apenasAbrir = true)

                "lanterna_on" -> concluirCom(comando, lanterna.ligar())
                "lanterna_off" -> concluirCom(comando, lanterna.desligar())

                "apitar" -> {
                    val duracaoMax = (comando.conteudo["duracao_maxima_segundos"] as? Number)?.toInt()
                        ?: prefs.duracaoMaximaApitoSegundos
                    apito.iniciar(duracaoMax) {
                        firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, "Apito parado por segurança (duração máxima atingida)")
                    }
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, "Apito iniciado")
                }
                "parar_alarme" -> {
                    apito.parar()
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, "Som parado")
                }

                "alarme" -> {
                    val horario = comando.conteudo["horario"] as? String
                    val atraso = (comando.conteudo["atraso_minutos"] as? Number)?.toInt()
                    val recorrente = (comando.conteudo["recorrente_diario"] as? Boolean) ?: false
                    val quando = alarmScheduler.agendar(comando.id, horario, atraso, recorrente)
                    val avisoExato = if (!alarmScheduler.podeAgendarExato())
                        " (sem permissão de alarme exato concedida — pode atrasar alguns minutos)" else ""
                    firebase.atualizarStatus(
                        comando, StatusComando.CONCLUIDO,
                        "Alarme agendado para $quando$avisoExato"
                    )
                }
                "cancelar_alarme" -> {
                    alarmScheduler.cancelar(comando.id)
                    val idAlvo = comando.conteudo["id_alarme"] as? String
                    if (idAlvo != null) alarmScheduler.cancelar(idAlvo)
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, "Alarme cancelado")
                }

                "notificacao" -> {
                    val titulo = comando.conteudo["titulo"] as? String ?: "Jarvis"
                    val texto = comando.conteudo["mensagem"] as? String ?: ""
                    exibirNotificacao(titulo, texto)
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO)
                }
                "mensagem" -> {
                    val texto = comando.conteudo["texto"] as? String ?: ""
                    exibirNotificacao("Mensagem do Jarvis", texto)
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO)
                }

                "abrir_app" -> {
                    val pacote = comando.conteudo["pacote"] as? String
                    val ok = pacote != null && appLauncher.abrir(pacote)
                    firebase.atualizarStatus(
                        comando,
                        if (ok) StatusComando.CONCLUIDO else StatusComando.ERRO,
                        if (ok) null else "App não está na whitelist ou não pôde ser aberto: $pacote"
                    )
                }
                "abrir_url" -> {
                    val url = comando.conteudo["url"] as? String
                    val ok = url != null && appLauncher.abrirUrl(url)
                    firebase.atualizarStatus(comando, if (ok) StatusComando.CONCLUIDO else StatusComando.ERRO)
                }

                "falar" -> {
                    val texto = comando.conteudo["texto"] as? String ?: ""
                    tts.falar(texto)
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO)
                }
                "ouvir_comando" -> {
                    SpeechManager(context).ouvir { textoReconhecido ->
                        firebase.atualizarStatus(
                            comando, StatusComando.CONCLUIDO,
                            dadosExtras = mapOf("textoReconhecido" to (textoReconhecido ?: ""))
                        )
                    }
                }

                "status_bateria" -> {
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, dadosExtras = statusProvider.statusBateria())
                }
                "status_rede" -> {
                    firebase.atualizarStatus(comando, StatusComando.CONCLUIDO, dadosExtras = statusProvider.statusRede())
                }

                "modo_nao_perturbe" -> {
                    val ligar = (comando.conteudo["ligar"] as? Boolean) ?: true
                    val ok = if (ligar) dnd.ligar() else dnd.desligar()
                    firebase.atualizarStatus(
                        comando, if (ok) StatusComando.CONCLUIDO else StatusComando.ERRO,
                        if (ok) null else "Permissão de acesso à política de notificações não concedida"
                    )
                }

                else -> {
                    firebase.atualizarStatus(comando, StatusComando.IGNORADO, "Tipo de comando desconhecido: ${comando.tipo}")
                }
            }
        } catch (e: Exception) {
            firebase.atualizarStatus(comando, StatusComando.ERRO, e.message ?: "Erro desconhecido")
        }

        prefs.marcarExecutado(comando.id)
    }

    private fun concluirCom(comando: Comando, sucesso: Boolean) {
        firebase.atualizarStatus(
            comando,
            if (sucesso) StatusComando.CONCLUIDO else StatusComando.ERRO,
            if (sucesso) null else "Não foi possível executar (sem câmera com flash ou erro de hardware)"
        )
    }

    private fun abrirTelaDeCaptura(comando: Comando, apenasAbrir: Boolean = false) {
        val tipoEfetivo = if (apenasAbrir) "tirar_foto_traseira" else comando.tipo
        val intent = Intent(context, CameraCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(CameraCaptureActivity.EXTRA_TIPO, tipoEfetivo)
            putExtra(CameraCaptureActivity.EXTRA_COMANDO_ID, comando.id)
            putExtra(CameraCaptureActivity.EXTRA_ORIGEM, comando.origem)
            putExtra(CameraCaptureActivity.EXTRA_DESTINO, comando.destino)
        }
        context.startActivity(intent)
    }

    private fun exibirNotificacao(titulo: String, texto: String) {
        val builder = NotificationCompat.Builder(context, JarvisApplication.CANAL_SERVICO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun liberar() {
        tts.liberar()
    }
}

/** Log simples em memória dos últimos comandos, lido pela MainActivity. */
object LogDeComandos {
    private val lista = mutableListOf<String>()

    fun registrar(comando: Comando) {
        val linha = "[${comando.tipo}] de ${comando.origem} -> ${comando.destino}"
        lista.add(0, linha)
        if (lista.size > 30) lista.removeAt(lista.size - 1)
    }

    fun ultimos(): List<String> = lista.toList()
}
