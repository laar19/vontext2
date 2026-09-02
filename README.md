# 📱 Vontext (Voice & Video Context for AI)

<div align="center">
  <h3>Convierte tus grabaciones de pantalla y voz en reportes técnicos perfectos para Agentes de IA</h3>
  <p><strong>Genera PDFs estructurados, capturas de pantalla secuenciadas, transcripciones con marcas de tiempo y volcados de Logcat en un solo paquete ZIP.</strong></p>
</div>

---

## 🚀 ¿Qué es Vontext?

Cuando grabas un bug o el comportamiento de una app y le hablas a tu teléfono, **Vontext**:
1. **Extrae los cuadros clave** del video en momentos significativos.
2. **Transcribe la voz** correlacionándola segundo a segundo con lo que ocurría en la pantalla.
3. **Analiza los elementos visuales (OCR)** presentes en cada captura.
4. **Extrae los logs del sistema (`logcat.txt`)** con excepciones, errores y advertencias ocurridos durante la prueba.
5. **Genera un reporte PDF profesional** y un **archivo ZIP** listo para adjuntarle a cualquier IA (Gemini, Claude, ChatGPT, etc.).

---

## ✨ Características Principales

- 🎯 **Detección Inteligente de Toques**: Acceso rápido a las opciones de desarrollador para mostrar círculos de toques en pantalla, facilitando que la IA sepa qué botón presionaste.
- 🫧 **Burbuja Flotante de Depuración**: Un botón flotante mientras interactúas con cualquier otra app para capturar logs al instante o volver a Vontext a procesar el video.
- 📋 **Volcado Automático de Logcat**: Incorporación de los registros y trazas de pila (Stack Traces) de Android sin necesidad de conectar el teléfono por USB o usar ADB.
- 📄 **Reporte PDF con Maquetación Completa**: Utiliza `StaticLayout` multilínea para que las transcripciones largas nunca se corten en la página.
- 📦 **Procesamiento Individual y por Lotes**: Procesa un video individual o una carpeta completa de grabaciones de forma masiva.
- 🛡️ **Seguridad y Robustez (Pre-Mortem Mitigado)**: Compatible con Android 8 hasta Android 15/16 (API 26 a API 36), soporte de Scoped Storage y ejecución con límites de tiempo para evitar bloqueos.

---

## 🛠️ Requisitos y Tecnologías

- **Lenguaje**: Kotlin 100%
- **Interfaz**: Jetpack Compose con Material Design 3
- **Persistencia**: Room Database
- **Versión mínima de Android**: Android 8.0 (API 26)
- **Versión objetivo**: Android 16 (API 36)
- **Herramienta de compilación**: Gradle (Kotlin DSL)

---

## 📥 Instalación y Compilación Local

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/vontext.git
cd vontext
```

### 2. Abrir en Android Studio
1. Abre **Android Studio** (Koala, Ladybug o superior).
2. Selecciona **Open** y elige la carpeta del proyecto.
3. Espera a que Gradle sincronice las dependencias automáticamente.

### 3. Configurar API Keys (Opcional para servicios Gemini)
Crea un archivo `.env` en la raíz del proyecto tomando como plantilla `.env.example`:
```bash
GEMINI_API_KEY=tu_clave_de_gemini_aqui
```

### 4. Compilar y Ejecutar
- **Desde Android Studio**: Presiona el botón verde de **Run (`Shift + F10`)**.
- **Desde la terminal**:
```bash
# Compilar el APK de depuración
gradle assembleDebug

# Ejecutar las pruebas unitarias
gradle :app:testDebugUnitTest
```

El APK resultante se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📂 Estructura del Proyecto

```
vontext/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt          # Pantalla principal con Jetpack Compose y navegación
│   │   │   ├── processor/
│   │   │   │   ├── VideoProcessor.kt    # Motor de extracción de frames, audio y generación PDF/ZIP
│   │   │   │   └── FrameOcrHelper.kt    # Análisis visual de elementos en pantalla
│   │   │   ├── service/
│   │   │   │   └── FloatingOverlayService.kt # Burbuja flotante de depuración
│   │   │   ├── util/
│   │   │   │   ├── DeveloperOptionsHelper.kt # Verificación de toques en pantalla
│   │   │   │   └── LogcatHelper.kt      # Captura segura de registros de sistema y almacenamiento
│   │   │   └── data/                    # Modelos y entidades de persistencia
│   │   └── AndroidManifest.xml
│   └── src/test/                        # Pruebas unitarias y Robolectric
├── ARCHITECTURE.md                      # Documentación de arquitectura detallada
└── README.md
```

---

## 📄 Licencia

Este proyecto está distribuido bajo la licencia MIT.
