package com.boletera.capturafoto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(private val context: Context) {
    
    private var cameraManager: android.hardware.camera2.CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    
    fun initialize() {
        try {
            Log.d(TAG, "Starting Camera2 initialization")
            Log.d(TAG, "Context: $context")
            
            // Verificar que el contexto sea válido
            if (context == null) {
                Log.e(TAG, "Context is null!")
                return
            }
            
            startBackgroundThread()
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            if (cameraManager == null) {
                Log.e(TAG, "Failed to get CameraManager service")
                return
            }
            
            val cameras = cameraManager!!.cameraIdList
            Log.d(TAG, "Available cameras: ${cameras.joinToString(", ")}")
            
            openCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization", e)
        }
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    private fun openCamera() {
        try {
            val manager = cameraManager ?: run {
                Log.e(TAG, "CameraManager is null")
                return
            }
            val cameraId = getFrontCameraId() ?: run {
                Log.e(TAG, "No front camera found")
                return
            }
            
            Log.d(TAG, "Front camera ID: $cameraId")
            
            // Setup ImageReader with better resolution for quality
            imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 1)
            Log.d(TAG, "ImageReader created: 1280x720 (better quality)")
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                throw SecurityException("Camera permission not granted")
            }
            
            Log.d(TAG, "Opening camera $cameraId")
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened successfully: ${camera.id}")
                    cameraDevice = camera
                    createCaptureSession()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected: ${camera.id}")
                    camera.close()
                    cameraDevice = null
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    val errorMsg = when(error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera in use"
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                        else -> "Unknown error ($error)"
                    }
                    Log.e(TAG, "Camera error: $errorMsg")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
        }
    }
    
    private fun getFrontCameraId(): String? {
        return try {
            val manager = cameraManager ?: return null
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding front camera", e)
            null
        }
    }
    
    private fun createCaptureSession() {
        try {
            Log.d(TAG, "Creating capture session...")
            val camera = cameraDevice ?: run {
                Log.e(TAG, "Camera device is null")
                return
            }
            val reader = imageReader ?: run {
                Log.e(TAG, "ImageReader is null")
                return
            }
            
            Log.d(TAG, "Creating session with surface: ${reader.surface}")
            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        Log.d(TAG, "Capture session configured successfully")
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        captureSession = null
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }
    
    suspend fun capturePhoto(): File = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Starting photo capture...")
            
            val session = captureSession ?: run {
                Log.e(TAG, "Capture session not initialized")
                continuation.resumeWithException(IllegalStateException("Capture session not initialized"))
                return@suspendCancellableCoroutine
            }
            
            val reader = imageReader ?: run {
                Log.e(TAG, "ImageReader not initialized")
                continuation.resumeWithException(IllegalStateException("ImageReader not initialized"))
                return@suspendCancellableCoroutine
            }
            
            val photoFile = createPhotoFile()
            
            reader.setOnImageAvailableListener({
                val image = reader.acquireLatestImage()
                try {
                    saveImageToFile(image, photoFile)
                    Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
                    
                    // Clear the listener to avoid buffer issues
                    reader.setOnImageAvailableListener(null, null)
                    
                    continuation.resume(photoFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving photo", e)
                    continuation.resumeWithException(e)
                } finally {
                    image?.close()
                    // Force cleanup to avoid HAL buffer errors
                    System.gc()
                }
            }, backgroundHandler)
            
            // Minimal capture request to avoid HAL errors
            val camera = cameraDevice ?: run {
                continuation.resumeWithException(IllegalStateException("Camera device not available"))
                return@suspendCancellableCoroutine
            }
            
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            
            // Configuración mejorada para calidad de imagen
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f) // Infinity focus
            
            // Habilitar procesamiento automático básico para mejor calidad
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            
            // Configuración de color mejorada para evitar tonos verdes
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST)
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST)
            
            // Reducción de ruido básica
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
            captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST)
            
            // Configuración de exposición
            captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
            
            // Calidad JPEG mejorada
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 85.toByte())
            
            // Capture the photo with improved resource management
            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(TAG, "Capture completed without autofocus - frame ${result.frameNumber}")
                    
                    // Add small delay to ensure proper buffer handling
                    backgroundHandler?.postDelayed({
                        Log.d(TAG, "Capture resources cleaned up")
                    }, 100)
                }
                
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    Log.e(TAG, "Capture failed: ${failure.reason}")
                    reader.setOnImageAvailableListener(null, null)
                    continuation.resumeWithException(RuntimeException("Capture failed: ${failure.reason}"))
                }
                
                override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
                    Log.d(TAG, "Capture sequence completed - sequence: $sequenceId, frame: $frameNumber")
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in capturePhoto", e)
            continuation.resumeWithException(e)
        }
    }
    
    private fun saveImageToFile(image: Image, file: File) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        FileOutputStream(file).use { it.write(bytes) }
    }
    
    private fun createPhotoFile(): File {
        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        return File(storageDir, "PHOTO_${timestamp}.jpg")
    }
    
    fun resetCaptureSession() {
        try {
            Log.d(TAG, "Resetting capture session to avoid buffer issues...")
            
            // Close current session aggressively
            captureSession?.close()
            captureSession = null
            
            // Clear ImageReader listener to prevent buffer leaks
            imageReader?.setOnImageAvailableListener(null, null)
            
            // Force garbage collection for problematic hardware
            System.gc()
            
            // Longer delay for hardware compatibility
            backgroundHandler?.postDelayed({
                Log.d(TAG, "Recreating capture session after reset...")
                createCaptureSession()
            }, 500)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting capture session", e)
        }
    }
    
    fun shutdown() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
            stopBackgroundThread()
            
            captureSession = null
            cameraDevice = null
            imageReader = null
            cameraManager = null
            
            Log.d(TAG, "Camera2 shutdown completed")
        } catch (e: Exception) {
            Log.w(TAG, "Error during shutdown: ${e.message}")
        }
    }
}
