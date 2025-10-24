# Android Photo Capture App - Versión Simplificada

Esta es una aplicación Android que captura fotos en segundo plano (sin vista previa) y las almacena localmente con procesamiento de imagen optimizado.

## Características Principales

- ✅ **Captura sin vista previa**: Toma fotos directamente sin mostrar preview
- ✅ **Almacenamiento local**: Guarda las fotos procesadas en el almacenamiento del dispositivo
- ✅ **Procesamiento de imagen avanzado**:
  - Rotación automática (90° para cámara frontal)
  - Redimensionado inteligente (máximo 800x600)
  - Corrección de balance de color (reduce tinte verde)
  - Compresión JPEG optimizada (85% calidad)
- ✅ **Compatible con Android 8.1+** (API 27+)
- ✅ **Optimizado para hardware limitado**: Funciona con Camera2 API

## Requisitos

- Android Studio Giraffe o superior
- Android SDK 34
- Dispositivo/emulador con Android 13 (API 33) o superior
- Cámara física o emulada

## Configuración

### 1. Configurar la URL de la API

Edita el archivo `ApiClient.kt` y cambia la URL base:

```kotlin
private const val BASE_URL = "https://tu-api.com/api/"
```

### 2. Endpoint esperado

La aplicación espera un endpoint POST en `/upload-photo` que reciba:

```json
{
  "image": "base64_encoded_image_string",
  "filename": "PHOTO_2024-01-01-12-00-00-000.jpg",
  "timestamp": 1704110400000
}
```

Y devuelva:

```json
{
  "success": true,
  "message": "Foto subida correctamente",
  "photoId": "12345",
  "url": "https://tu-api.com/photos/12345.jpg"
}
```

## Instalación

1. Abre el proyecto en Android Studio
2. Sincroniza Gradle
3. Conecta un dispositivo Android o inicia un emulador
4. Ejecuta la aplicación

## Uso

1. Al abrir la app, se solicitarán permisos de cámara
2. Una vez otorgados, se inicializará la cámara en segundo plano
3. Presiona el botón "Capturar y Subir Foto"
4. La foto se captura sin mostrar preview
5. Se almacena localmente en el dispositivo
6. Se convierte a Base64
7. Se sube a la API configurada
8. El registro muestra todo el proceso

## Estructura del Proyecto

```
app/src/main/java/com/boletera/capturafoto/
├── MainActivity.kt           # Actividad principal
├── CameraManager.kt          # Gestión de CameraX
├── ImageUtils.kt             # Utilidades de conversión Base64
├── ApiClient.kt              # Cliente Retrofit
└── ApiService.kt             # Definición de API

app/src/main/res/
├── layout/
│   └── activity_main.xml     # UI principal
├── values/
│   ├── strings.xml
│   ├── colors.xml
│   └── themes.xml
└── xml/
    └── network_security_config.xml
```

## Permisos

- `CAMERA`: Para capturar fotos
- `INTERNET`: Para subir fotos a la API
- `ACCESS_NETWORK_STATE`: Para verificar conectividad

## Dependencias Principales

- **CameraX**: Captura de imágenes moderna
- **Retrofit**: Cliente HTTP
- **OkHttp**: Networking
- **Coroutines**: Programación asíncrona
- **Material Components**: UI moderna

## Notas de Desarrollo

- Las fotos se almacenan en `getExternalFilesDir(null)` o `filesDir`
- La cámara se inicializa sin crear un Preview
- El proceso es completamente asíncrono usando coroutines
- Incluye logging completo para depuración

## Solución de Problemas

### Error: "Camera provider no inicializado"
- Verifica que los permisos de cámara estén otorgados
- Reinicia la aplicación

### Error al subir foto
- Verifica la URL de la API en `ApiClient.kt`
- Revisa los logs en el registro de actividad
- Asegúrate de tener conexión a internet

### Foto muy grande
- La conversión Base64 aumenta el tamaño ~33%
- Considera comprimir la imagen antes de convertir

## Licencia

Este proyecto es de código abierto para uso interno de Boletera General.
