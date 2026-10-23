package com.jarviscaseiro.app.camera

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Captura uma foto com CameraX. IMPORTANTE: isso só funciona com uma Activity
 * em primeiro plano (precisa de um LifecycleOwner visível) — é exatamente essa
 * a limitação intencional do projeto: a câmera NUNCA é acionada silenciosamente
 * com a tela bloqueada ou o app totalmente em segundo plano.
 */
class CameraController(private val context: Context) {

    fun capturar(
        lifecycleOwner: LifecycleOwner,
        usarFrontal: Boolean,
        onSucesso: (String) -> Unit, // retorna imagem já em Base64 comprimida
        onErro: (String) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                provider.unbindAll()

                val seletor = if (usarFrontal) CameraSelector.DEFAULT_FRONT_CAMERA
                              else CameraSelector.DEFAULT_BACK_CAMERA

                val captura = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                provider.bindToLifecycle(lifecycleOwner, seletor, captura)

                val arquivoTemp = File.createTempFile("jarvis_foto_", ".jpg", context.cacheDir)
                val opcoesSaida = ImageCapture.OutputFileOptions.Builder(arquivoTemp).build()

                captura.takePicture(
                    opcoesSaida,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            provider.unbindAll()
                            try {
                                val base64 = comprimirParaBase64(arquivoTemp)
                                arquivoTemp.delete()
                                onSucesso(base64)
                            } catch (e: Exception) {
                                onErro("Erro ao comprimir imagem: ${e.message}")
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            provider.unbindAll()
                            onErro("Erro ao capturar: ${exception.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                onErro("Erro ao iniciar câmera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Comprime a foto (reduz resolução e qualidade JPEG) antes de converter em Base64. */
    private fun comprimirParaBase64(arquivo: File, larguraMax: Int = 1280, qualidade: Int = 60): String {
        var bitmap = BitmapFactory.decodeFile(arquivo.absolutePath)
            ?: throw IllegalStateException("Não foi possível decodificar a imagem capturada")

        // Corrige rotação com base no EXIF (fotos de câmera costumam vir rotacionadas)
        try {
            val exif = ExifInterface(arquivo.absolutePath)
            val orientacao = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val graus = when (orientacao) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (graus != 0f) {
                val matriz = Matrix().apply { postRotate(graus) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível ler EXIF: ${e.message}")
        }

        if (bitmap.width > larguraMax) {
            val proporcao = larguraMax.toFloat() / bitmap.width
            val novaAltura = (bitmap.height * proporcao).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, larguraMax, novaAltura, true)
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, qualidade, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val TAG = "CameraController"
    }
}
