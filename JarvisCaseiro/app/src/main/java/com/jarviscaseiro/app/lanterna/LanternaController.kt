package com.jarviscaseiro.app.lanterna

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log

/**
 * Liga/desliga a lanterna usando a Camera2 API diretamente (CameraManager.setTorchMode),
 * sem precisar abrir uma sessão de câmera completa. Funciona com o app em background
 * (não depende de preview), desde que nenhuma outra sessão de câmera esteja em uso.
 */
class LanternaController(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var idCameraComFlash: String? = descobrirCameraComFlash()
    private var ligada = false

    private fun descobrirCameraComFlash(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao procurar câmera com flash: ${e.message}")
            null
        }
    }

    fun ligar(): Boolean = setModo(true)
    fun desligar(): Boolean = setModo(false)
    fun estaLigada() = ligada

    private fun setModo(ligar: Boolean): Boolean {
        val id = idCameraComFlash ?: return false
        return try {
            cameraManager.setTorchMode(id, ligar)
            ligada = ligar
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao alternar lanterna: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "LanternaController"
    }
}
