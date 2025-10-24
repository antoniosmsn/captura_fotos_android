# Android Photo Capture App - AI Assistant Instructions

## Project Overview
This is an Android 8.1+ photo capture application that takes photos in the background (without preview) and uploads them as Base64 to a REST API. The app uses modern Android architecture with CameraX, Retrofit, and Coroutines.

## Key Architecture Patterns

### Component Structure
- **MainActivity**: Single activity with ViewBinding, manages UI state and orchestrates photo capture workflow
- **CameraManager**: Encapsulates CameraX logic, provides suspend functions for photo capture without preview
- **ApiService/ApiClient**: Retrofit-based API layer with structured request/response models
- **ImageUtils**: Utility object for Base64 conversion and file operations

### Data Flow
1. Permission check → Camera initialization → User action → Photo capture → Base64 conversion → API upload
2. All async operations use Coroutines with `lifecycleScope.launch` in MainActivity
3. Real-time status updates flow through `updateStatus()` method to UI components

## Critical Implementation Details

### CameraX Without Preview
```kotlin
// Key pattern: ImageCapture without Preview binding
imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()

cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, imageCapture)
```

### API Configuration
- **Base URL**: Must be changed in `ApiClient.kt` - currently set to placeholder "https://tu-api.com/api/"
- **Endpoint**: Expects POST `/upload-photo` with PhotoUploadRequest body
- **Network Security**: Uses cleartext traffic permission for development (see network_security_config.xml)

### File Storage Strategy
- Photos stored in `getExternalFilesDir(null)` with fallback to `filesDir`
- Filename format: `PHOTO_yyyy-MM-dd-HH-mm-ss-SSS.jpg`
- No compression applied - full resolution images converted to Base64

### Error Handling Patterns
- Always wrap camera operations in try-catch with user-friendly status updates
- Network errors display HTTP status codes to user
- Permission failures terminate app with toast message

## Development Workflows

### Building & Running
```bash
# Windows PowerShell commands
.\gradlew clean
.\gradlew assembleDebug
.\gradlew installDebug

# For release builds
.\gradlew assembleRelease
```

### Testing on Device/Emulator
- Requires Android 8.1+ (API 27) minimum
- Camera permission must be granted manually on first run
- For emulator: Enable "Virtual Scene" in camera settings for testing

### Key Configuration Points
1. **API URL**: Change `BASE_URL` in `ApiClient.kt` before deployment
2. **Timeouts**: Default 30s connect/read/write timeouts in OkHttp client
3. **Logging**: HTTP requests logged via OkHttpLoggingInterceptor (remove for production)

## Common Modification Patterns

### Adding New API Endpoints
1. Add method to `ApiService` interface with appropriate annotations
2. Create request/response data classes in same file
3. Call from MainActivity using `lifecycleScope.launch`

### Modifying Photo Processing
- Extend `ImageUtils` for compression, filtering, or metadata operations
- Consider file size limits when modifying Base64 conversion (current: no compression)

### UI Updates
- All status changes go through `updateStatus()` method
- Log entries auto-scroll via `scrollView.fullScroll()`
- Material Design components used throughout (MaterialButton, MaterialCardView)

## Dependencies & Versions
- **CameraX**: 1.3.1 (core, camera2, lifecycle)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0
- **Coroutines**: 1.7.3 (Android + Core)
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 27 (Android 8.1)

## Security Considerations
- CAMERA permission required and checked at runtime
- Network security config allows cleartext (development only)
- No image compression = larger Base64 payloads
- API credentials should be added to request headers if authentication required

## Debugging & Troubleshooting
- Check Logcat for CameraManager and network logs
- Status updates show in real-time UI log
- Common issues: Permission denial, API URL misconfiguration, network connectivity
- For camera initialization errors: verify device has physical camera or emulator camera enabled