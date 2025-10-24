package com.boletera.capturafoto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.boletera.capturafoto.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicializar servicios
        cameraManager = CameraManager(this)
        
        // Configurar UI
        setupUI()
        
        // Verificar permisos
        if (allPermissionsGranted()) {
            initializeCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }
        
        updateStatus("Esperando acción del usuario")
    }
    
    private fun initializeCamera() {
        try {
            cameraManager.initialize()
            updateStatus("Cámara inicializada correctamente")
            binding.btnCapture.isEnabled = true
        } catch (e: IllegalStateException) {
            val message = when {
                e.message?.contains("No hay cámaras disponibles") == true -> 
                    "Este dispositivo no tiene cámaras disponibles"
                e.message?.contains("Camera provider no inicializado") == true -> 
                    "Error interno de la cámara"
                else -> "Error de cámara: ${e.message}"
            }
            updateStatus("Error: $message")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val message = "Error al inicializar cámara: ${e.localizedMessage ?: e.message}"
            updateStatus("Error: $message")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun capturePhoto() {
        binding.btnCapture.isEnabled = false
        updateStatus("📸 Iniciando captura de foto...")
        
        lifecycleScope.launch {
            try {
                // Capturar foto
                val photoFile = cameraManager.capturePhoto()
                updateStatus("✓ Foto capturada: ${photoFile.name}")
                
                // Procesar imagen localmente
                updateStatus("🔄 Procesando imagen...")
                val rotatedBitmap = ImageUtils.rotateImage(photoFile, 270)
                val resizedBitmap = ImageUtils.resizeImage(rotatedBitmap, 800, 600)
                val colorCorrectedBitmap = ImageUtils.adjustColorBalance(resizedBitmap)
                
                // Guardar la imagen procesada
                val processedFile = ImageUtils.saveBitmapToFile(colorCorrectedBitmap, this@MainActivity)
                updateStatus("✓ Imagen procesada y guardada: ${processedFile.name}")
                updateStatus("📁 Ubicación: ${processedFile.absolutePath}")
                updateStatus("✅ Proceso completado exitosamente")
                
                Toast.makeText(this@MainActivity, "Foto guardada: ${processedFile.name}", Toast.LENGTH_SHORT).show()
                
                // Reset capture session to avoid HAL buffer errors
                cameraManager.resetCaptureSession()
                
            } catch (e: Exception) {
                updateStatus("✗ Error: ${e.message}")
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Reset capture session even on error to clean up resources
                cameraManager.resetCaptureSession()
            } finally {
                binding.btnCapture.isEnabled = true
            }
        }
    }
    
    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
        binding.tvLog.text = "${binding.tvLog.text}\n[${getCurrentTime()}] $message"
        
        // Auto-scroll al final
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
    
    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permisos de cámara no otorgados",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }
}
