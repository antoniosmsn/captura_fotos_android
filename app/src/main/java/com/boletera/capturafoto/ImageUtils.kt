package com.boletera.capturafoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    
    /**
     * Rota una imagen desde archivo
     */
    fun rotateImage(file: File, degrees: Int): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
        return rotateBitmap(originalBitmap, degrees.toFloat())
    }
    
    /**
     * Redimensiona un bitmap a dimensiones específicas manteniendo proporción
     */
    fun resizeImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return resizeBitmap(bitmap, maxWidth, maxHeight)
    }
    
    /**
     * Ajusta el balance de color para corregir tintes verdes/azules
     */
    fun adjustColorBalance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        adjustedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val alpha = (pixel shr 24) and 0xFF
            var red = (pixel shr 16) and 0xFF
            var green = (pixel shr 8) and 0xFF
            var blue = pixel and 0xFF
            
            // Reducir canal verde ligeramente para corregir tinte
            green = (green * 0.9).toInt()
            
            // Aumentar rojo y azul ligeramente para balance
            red = minOf(255, (red * 1.05).toInt())
            blue = minOf(255, (blue * 1.02).toInt())
            
            pixels[i] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
        
        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return adjustedBitmap
    }
    
    /**
     * Agrega texto de información sobre la imagen (número de autobús y fecha)
     */
    fun addTextOverlay(bitmap: Bitmap, busNumber: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        // Configurar fecha actual
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        // Configurar paint para fondo semi-transparente
        val backgroundPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0) // Negro con 70% opacidad
            style = Paint.Style.FILL
        }
        
        // Configurar paint para texto
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.05f // 5% del ancho de la imagen
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        
        // Textos a mostrar
        val line1 = "Autobús: $busNumber"
        val line2 = "Fecha: $currentDate"
        
        // Medir textos
        val bounds1 = Rect()
        val bounds2 = Rect()
        textPaint.getTextBounds(line1, 0, line1.length, bounds1)
        textPaint.getTextBounds(line2, 0, line2.length, bounds2)
        
        val maxTextWidth = maxOf(bounds1.width(), bounds2.width())
        val textHeight = bounds1.height()
        val lineSpacing = textHeight * 0.3f
        
        // Calcular posición (esquina inferior izquierda con padding)
        val padding = bitmap.width * 0.03f
        val backgroundHeight = textHeight * 2 + lineSpacing + padding * 2
        val backgroundY = bitmap.height - backgroundHeight
        
        // Dibujar fondo semi-transparente
        canvas.drawRect(
            0f,
            backgroundY,
            maxTextWidth + padding * 2,
            bitmap.height.toFloat(),
            backgroundPaint
        )
        
        // Dibujar textos
        val textX = padding
        var textY = backgroundY + padding + textHeight
        canvas.drawText(line1, textX, textY, textPaint)
        
        textY += textHeight + lineSpacing
        canvas.drawText(line2, textX, textY, textPaint)
        
        return mutableBitmap
    }
    
    /**
     * Guarda un bitmap a archivo con timestamp único
     */
    fun saveBitmapToFile(bitmap: Bitmap, context: Context): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(Date())
        val filename = "PHOTO_PROCESSED_$timestamp.jpg"
        
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, filename)
        
        FileOutputStream(outputFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        }
        
        return outputFile
    }
    
    /**
     * Rota un bitmap por los grados especificados
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Redimensiona un bitmap manteniendo la proporción
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calcular el factor de escala
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        // Si la imagen ya es pequeña, no redimensionar
        if (scale >= 1.0f) return bitmap
        
        // Calcular nuevas dimensiones
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Convierte una cadena Base64 a un archivo
     */
    fun base64ToFile(base64String: String, outputFile: File) {
        val bytes = Base64.decode(base64String, Base64.NO_WRAP)
        outputFile.writeBytes(bytes)
    }
    
    /**
     * Obtiene el tamaño del archivo en formato legible
     */
    fun getFileSize(file: File): String {
        val bytes = file.length()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
