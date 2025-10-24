# 📸 Captura de Fotos para Autobuses - Android

Aplicación Android para captura automática de fotos sin vista previa, diseñada para dispositivos instalados en autobuses. Procesa las imágenes localmente añadiendo información de identificación y fecha.

## ✨ Características Principales

- ✅ **Captura sin vista previa**: Toma fotos directamente sin mostrar preview
- ✅ **Almacenamiento local**: Guarda las fotos procesadas en el dispositivo
- ✅ **Información superpuesta**: Agrega número de autobús y fecha/hora en la imagen
- ✅ **Procesamiento de imagen optimizado**:
  - Rotación automática (270° para cámara frontal)
  - Redimensionado inteligente (máximo 800x600 píxeles)
  - Corrección de balance de color (reduce tinte verde)
  - Compresión JPEG optimizada (85% calidad)
- ✅ **Compatible con Android 8.1+** (API 27+)
- ✅ **Optimizado para hardware limitado**: Usa Camera2 API directa

## 📋 Requisitos del Sistema

- **Android 8.1+** (API nivel 27 o superior)
- **Cámara**: Hardware de cámara requerido (frontal o trasera)
- **Permisos**: CAMERA (solicitado automáticamente en primera ejecución)
- **Almacenamiento**: Espacio disponible para guardar fotos (~100-150KB por foto)

## 🚀 Instalación y Configuración

### Opción 1: Compilación desde código

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/antoniosmsn/captura_fotos_android.git
   cd captura_fotos_android
   ```

2. **Compilar el APK**
   ```bash
   # Windows PowerShell
   $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
   .\gradlew clean assembleDebug
   ```

3. **Instalar en dispositivo**
   ```bash
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Opción 2: Instalación directa del APK

1. Transferir el APK al dispositivo
2. Habilitar "Orígenes desconocidos" en Ajustes de Android
3. Abrir el APK y confirmar instalación

## 📱 Uso de la Aplicación

1. **Primera ejecución**: Otorgar permisos de cámara cuando lo solicite
2. **Inicialización**: Esperar a que se inicialice la cámara (mensaje en pantalla)
3. **Captura**: Presionar el botón "Capturar Foto"
4. **Proceso automático**:
   - Captura la foto sin preview
   - Rota la imagen correctamente
   - Redimensiona para optimizar tamaño
   - Corrige el balance de color
   - Agrega número de autobús (124) y fecha/hora actual
   - Guarda en almacenamiento local
5. **Verificación**: El log en pantalla muestra cada paso del proceso

## 📂 Ubicación de Archivos

Las fotos procesadas se guardan en:

```
/storage/emulated/0/Android/data/com.boletera.capturafoto/files/
```

**Formato del nombre**: `PHOTO_PROCESSED_yyyy-MM-dd-HH-mm-ss-SSS.jpg`

**Ejemplo**: `PHOTO_PROCESSED_2025-10-23-20-45-30-123.jpg`

## 🎨 Información Superpuesta en la Imagen

Cada foto incluye automáticamente:

- **Línea 1**: `Autobús: 124`
- **Línea 2**: `Fecha: 23/10/2025 20:45:30`

El texto se muestra con:
- Fondo semi-transparente negro
- Texto blanco con sombra para legibilidad
- Posición: Esquina inferior izquierda
- Tamaño: Proporcional al tamaño de la imagen

## 🏗️ Arquitectura Técnica

### Componentes Principales

```
app/src/main/java/com/boletera/capturafoto/
├── MainActivity.kt           # Actividad principal y orquestación
├── CameraManager.kt          # Gestión de Camera2 API
└── ImageUtils.kt             # Procesamiento de imagen y texto superpuesto

app/src/main/res/
├── layout/
│   └── activity_main.xml     # UI con botón y log de actividad
└── values/                   # Recursos (strings, colores, temas)
```

### Flujo de Procesamiento

```
Captura → Rotación (270°) → Redimensionado (800x600) → 
Corrección de Color → Texto Superpuesto → Compresión JPEG → 
Guardado Local
```

## 🔧 Configuración de Cámara

### Camera2 API - Configuración Optimizada

- **Modo de exposición**: Automático rápido (`CONTROL_AE_MODE_ON`)
- **Balance de blancos**: Automático (`CONTROL_AWB_MODE_AUTO`)
- **Corrección de color**: Modo rápido (`CONTROL_COLOR_CORRECTION_MODE_FAST`)
- **Reducción de ruido**: Modo rápido (`NOISE_REDUCTION_MODE_FAST`)
- **Resolución de captura**: 1280x720 píxeles
- **Calidad JPEG**: 85%

### Procesamiento de Imagen

1. **Rotación**: 270° para corregir orientación de cámara frontal
2. **Redimensionado**: Máximo 800x600 píxeles (mantiene proporción)
3. **Balance de color**: 
   - Reduce canal verde: -10%
   - Aumenta canal rojo: +5%
   - Aumenta canal azul: +2%
4. **Texto superpuesto**: Número de autobús y fecha/hora con fondo semi-transparente

## 📦 Dependencias

```gradle
// Cámara
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'

// Corrutinas
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// UI y Lifecycle
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
```

## 🔍 Comandos Útiles

### Ver fotos guardadas
```bash
adb shell ls -la /storage/emulated/0/Android/data/com.boletera.capturafoto/files/
```

### Descargar foto del dispositivo
```bash
adb pull /storage/emulated/0/Android/data/com.boletera.capturafoto/files/PHOTO_PROCESSED_[fecha].jpg
```

### Ver logs en tiempo real
```bash
adb logcat -s "CameraManager" "MainActivity"
```

### Limpiar fotos del dispositivo
```bash
adb shell rm -rf /storage/emulated/0/Android/data/com.boletera.capturafoto/files/*.jpg
```

## 🐛 Solución de Problemas

### "No hay cámaras disponibles"
- ✅ Verificar que el dispositivo tenga cámara física
- ✅ Revisar que los permisos de cámara estén otorgados
- ✅ Reiniciar la aplicación

### Errores HAL de cámara
- ✅ La configuración está optimizada para minimizar errores HAL
- ✅ Se implementa reset automático de sesión de captura
- ✅ Probado en dispositivos con hardware limitado (Android 8.1)

### Foto con orientación incorrecta
- ✅ Ajustar el ángulo de rotación en `MainActivity.kt` línea 87
- ✅ Valores comunes: 0°, 90°, 180°, 270°

### OutOfMemoryError
- ✅ Implementado reciclaje automático de bitmaps
- ✅ Redimensionado automático a 800x600 máximo
- ✅ Compresión JPEG al 85%

## 🎯 Características Técnicas

- **Mínimo SDK**: 27 (Android 8.1)
- **Target SDK**: 34 (Android 14)
- **Tamaño APK**: ~8 MB
- **Tamaño por foto**: ~100-150 KB (después de procesamiento)
- **Tiempo de procesamiento**: ~2-3 segundos por foto
- **Cámara**: Funciona con cámara frontal o trasera

## 📝 Notas de Desarrollo

### Optimizaciones Implementadas
- ✅ Reset automático de sesión de captura para evitar errores de buffer HAL
- ✅ Configuración minimalista de Camera2 para máxima compatibilidad
- ✅ Gestión eficiente de memoria con reciclaje de bitmaps
- ✅ Pipeline de procesamiento optimizado

### Probado en
- **Dispositivo**: FT2-G
- **Android**: 8.1.0
- **Cámara**: Solo frontal
- **Estado**: Funcionando estable

## 📄 Licencia

Proyecto de uso interno para Boletera General.
