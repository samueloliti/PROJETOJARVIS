package com.jarviscaseiro.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarviscaseiro.app.comandos.LogDeComandos
import com.jarviscaseiro.app.databinding.ActivityMainBinding
import com.jarviscaseiro.app.service.JarvisForegroundService
import com.jarviscaseiro.app.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    private val pedirPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { atualizarTelaDePermissoes() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        binding.txtDeviceId.text = "ID: ${prefs.deviceId}"

        binding.btnToggleServico.setOnClickListener {
            if (prefs.servicoAtivo) {
                JarvisForegroundService.parar(this)
            } else {
                solicitarPermissoesEssenciais()
                JarvisForegroundService.iniciar(this)
            }
            atualizarEstadoServico()
        }

        solicitarPermissoesEssenciais()
    }

    override fun onResume() {
        super.onResume()
        atualizarEstadoServico()
        atualizarTelaDePermissoes()
        atualizarListaComandos()
        agendarAtualizacaoPeriodica()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun agendarAtualizacaoPeriodica() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                atualizarListaComandos()
                handler.postDelayed(this, 3000)
            }
        }, 3000)
    }

    private fun atualizarEstadoServico() {
        binding.txtServicoStatus.text = if (prefs.servicoAtivo) "Serviço: ativo" else "Serviço: parado"
        binding.btnToggleServico.text =
            if (prefs.servicoAtivo) getString(R.string.parar_servico) else getString(R.string.iniciar_servico)
        binding.txtStatusFirebase.text =
            if (prefs.servicoAtivo) getString(R.string.status_conectando) else getString(R.string.status_desconectado)
    }

    private fun atualizarListaComandos() {
        val ultimos = LogDeComandos.ultimos()
        binding.txtUltimosComandos.text =
            if (ultimos.isEmpty()) "Nenhum comando recebido ainda." else ultimos.joinToString("\n")
    }

    private fun solicitarPermissoesEssenciais() {
        val permissoes = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissoes.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val faltando = permissoes.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltando.isNotEmpty()) {
            pedirPermissoes.launch(faltando.toTypedArray())
        }
    }

    private fun atualizarTelaDePermissoes() {
        val pendentes = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            pendentes.add("Câmera")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            pendentes.add("Microfone")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pendentes.add("Notificações")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) pendentes.add("Alarmes exatos")
        }

        val notifManager = getSystemService(NotificationManager::class.java)
        if (!notifManager.isNotificationPolicyAccessGranted) pendentes.add("Acesso ao Não Perturbe")

        binding.txtPermissoesPendentes.text =
            if (pendentes.isEmpty()) "Tudo certo — nenhuma permissão pendente." else pendentes.joinToString(", ")
    }
}
