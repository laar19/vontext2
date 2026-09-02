# Arquitectura y Documentación Técnica - Vontext

**Vontext** (Voice & Video Context for AI Coding Agents) transforma grabaciones de pantalla y audio de voz en paquetes estructurados de contexto de depuración de alta fidelidad, listos para ser consumidos por modelos de IA (Gemini, Claude, GPT, Antigravity, etc.).

---

## 1. Arquitectura General

```
                        [ Usuario graba pantalla con toques + voz ]
                                           │
                                           ▼
                              [ Vontext VideoProcessor ]
               ┌───────────────────────────┴───────────────────────────┐
               ▼                                                       ▼
     [ Extracción de Cuadros ]                               [ Transcripción de Audio ]
  - MediaMetadataRetriever                                - Extracción de pista AAC/WAV
  - Detección de cambios (SSIM / Luminancia)              - Whisper / Speech Recognizer
  - Análisis OCR UI (FrameOcrHelper)                      - Timestamps precisos por segmento
               │                                                       │
               └───────────────────────────┬───────────────────────────┘
                                           ▼
                                 [ Fusión Temporal ]
                     - Correlación exacta entre frame y voz hablada
                     - Inyección de Logcat reciente del sistema
                                           │
                        ┌──────────────────┴──────────────────┐
                        ▼                                     ▼
             [ Generador de PDF ]                   [ Empaquetador ZIP ]
        - Páginas individuales por frame        - Carpeta de frames en PNG/WebP
        - StaticLayout multilínea (sin cortes) - Transcripción completa (.txt / .md)
        - Insignias de OCR / Elementos UI       - Logcat del sistema (logcat.txt)
        - Trazas de error y metadatos           - Metadatos del dispositivo (JSON)
```

---

## 2. Componentes Clave

### 2.1 Procesamiento de Video (`VideoProcessor.kt`)
- **Extracción de Cuadros**: Obtiene cuadros clave en momentos de transición visual significativa y en intervalos fijos para no saturar con imágenes redundantes.
- **Correlación de Audio**: Asocia la transcripción correspondiente a cada segundo del video con su cuadro visual correspondiente.
- **Generación de PDF con StaticLayout**: Renderiza cada frame con su transcripción multilínea con ajuste dinámico de texto (`StaticLayout`), márgenes protegidos y recorte de canvas (`clipRect`) para evitar desbordamientos en transcripciones largas.

### 2.2 Volcado de Registros del Sistema (`LogcatHelper.kt`)
- **Captura Segura de Logcat**: Ejecuta de forma segura `logcat -d -v time *:W` filtrando advertencias, errores, excepciones no controladas y fallos de la app.
- **Límite de Tiempo Rígido (Anti-Freeze)**: Timeout de 3 segundos mediante `process.waitFor(3, TimeUnit.SECONDS)` y limpieza asegurada en bloques `finally` con `destroy()`.
- **Compatibilidad con Scoped Storage**: Función `getVontextBaseDir(context)` que detecta automáticamente si el almacenamiento público en `Downloads/Vontext` está disponible o conmuta de forma transparente al almacenamiento protegido de la app (`context.getExternalFilesDir`).

### 2.3 Burbuja Flotante de Depuración (`FloatingOverlayService.kt`)
- **Superposición Global (`SYSTEM_ALERT_WINDOW`)**: Permite tener un botón accesible sobre cualquier otra aplicación que se esté probando o grabando.
- **Foreground Service Android 14+**: Cumple con los requerimientos estrictos de API 34/35/36 usando `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
- **Acciones Rápidas**:
  - Capturar volcado instantáneo de Logcat en cualquier momento.
  - Regresar a Vontext en un toque para procesar inmediatamente la grabación.
  - Minimizar / cerrar la burbuja.

### 2.4 Asistente de Opciones de Desarrollador (`DeveloperOptionsHelper.kt`)
- **Verificación de Toques Visuales**: Consulta el ajuste del sistema `Settings.System.SHOW_TOUCHES` (`show_touches`).
- **Navegación en 1 Toque**: Abre directamente la pantalla de Opciones de Desarrollador del sistema operativo para que el usuario active los círculos visuales de toques, permitiendo a la IA ver con exactitud dónde se interactuó con la interfaz.

### 2.5 Reconocimiento de Elementos en Pantalla (`FrameOcrHelper.kt`)
- **Análisis Ligero en Memoria**: Analiza componentes clave de UI, campos de texto, barras superiores y botones con decodificación de límites previa (`inJustDecodeBounds = true`) y escala reducida para evitar uso excesivo de RAM.

---

## 3. Formato del Paquete de Salida (ZIP y PDF)

El paquete generado contiene:
1. `reporte_vontext.pdf`: Documento completo con resumen del dispositivo, capturas secuenciadas, texto transcrito correspondiente a cada momento y diagnóstico de errores.
2. `frames/`: Directorio con las capturas de pantalla de alta calidad.
3. `transcripcion.txt` / `transcripcion.md`: Texto continuo narrado durante la grabación.
4. `logcat.txt`: Volcado de registros del sistema Android y excepciones producidas durante la sesión.
5. `metadata.json`: Datos del dispositivo (marca, modelo, versión de Android, SDK, duración y resolución).

---

## 4. Permisos Declarados en el Manifiesto

- `android.permission.READ_MEDIA_VIDEO`: Para seleccionar grabaciones de pantalla.
- `android.permission.RECORD_AUDIO`: Para captura de voz y transcripción.
- `android.permission.SYSTEM_ALERT_WINDOW`: Para la burbuja flotante sobre otras apps.
- `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Para la ejecución en segundo plano del servicio de superposición.
- `android.permission.POST_NOTIFICATIONS`: Notificación del servicio en primer plano para cumplir con los lineamientos de Android 13+.
