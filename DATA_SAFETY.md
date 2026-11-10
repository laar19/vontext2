# Formulario de Seguridad de los Datos (Google Play Data Safety)

Guía técnica oficial y transparente para completar el cuestionario de **Seguridad de los Datos (Data Safety)** en Google Play Console para **Vontext v2.3.1**:

---

## 1. Cuestionario General de Seguridad de los Datos

| Pregunta de Google Play Console | Respuesta Declarada | Justificación Técnica Oficial |
| :--- | :--- | :--- |
| **¿Tu app recopila o comparte datos de los usuarios?** | **Sí** | Dado que la app ofrece la opción de conectar con servicios externos de IA (Google Gemini, OpenAI, Groq) por solicitud voluntaria del usuario, se debe declarar **Sí** de forma transparente (aunque exista un modo local offline). |
| **¿Todos los datos de los usuarios recopilados por tu app se cifran en tránsito?** | **Sí** | Toda comunicación con APIs externas (Gemini, OpenAI, Groq) se realiza de forma obligatoria bajo canales cifrados TLS 1.3 / HTTPS. |
| **¿Proporcionas una forma para que los usuarios soliciten que se eliminen sus datos?** | **Sí** | Todo se guarda localmente en el dispositivo. La app cuenta con la opción "Eliminar" individual y "Limpiar Todo" en el Historial. La desinstalación o borrado de datos de la app elimina de inmediato todos los archivos. |
| **¿Tu app recopila datos de ubicación precisa/aproximada o identificadores personales?** | **No** | No se solicita GPS ni ubicación por red. No se leen números telefónicos, contactos, libreta de direcciones, IMEI ni identificadores de publicidad para seguimiento de usuarios. |

---

## 2. Tipos de Datos a Declarar en Google Play Console

### A. Fotos y Videos
- **Tipo específico:** Fotos / Videos (Capturas de fotogramas del video y grabaciones de pantalla).
- **¿Se recopilan?** Sí (procesados y opcionalmente transmitidos si el usuario elige el modelo multimodal de Gemini).
- **¿Se comparten con terceros?** No se comparten con brokers ni anunciantes. Se transmiten únicamente al proveedor de inferencia elegido por el usuario (API de Google Gemini) para la síntesis de pantalla solicitada.
- **¿El usuario puede decidir si se recopilan?** Sí (el usuario decide qué videos procesar y qué modelo seleccionar; el modo local Vosk no transmite nada).
- **Finalidad:** Funcionalidad de la aplicación (análisis visual de pantallas en grabaciones de prueba técnica).

### B. Audio
- **Tipo específico:** Grabaciones de voz / audio.
- **¿Se recopilan?** Sí (procesados y opcionalmente transmitidos si el usuario configura un motor remoto de transcripción como Whisper/OpenAI).
- **¿Se comparten con terceros?** No se comparten comercialmente. Se transmiten exclusivamente al endpoint configurado por el usuario.
- **¿El usuario puede decidir si se recopilan?** Sí (el uso de modelos remotos es opcional; el motor local Vosk procesa el audio 100% en el dispositivo).
- **Finalidad:** Funcionalidad de la aplicación (transcripción de audio a texto para el reporte de bugs).

### C. Registros de Diagnóstico y Actividad de la App
- **Tipo específico:** Registros de diagnóstico (extractos de Logcat, reportes de fallos técnicos).
- **¿Se recopilan?** Sí (a petición del usuario para adjuntar evidencia técnica en el archivo ZIP del reporte).
- **¿Se comparten con terceros?** No. Permanecen en el dispositivo del usuario. Además, se aplica un algoritmo de redacción que censura automáticamente claves de API, tokens Bearer, contraseñas y correos antes de escribirlos en disco.
- **Finalidad:** Análisis de fallos y depuración de software.

---

## 3. Declaración de Servicios en Primer Plano (Foreground Service Declaration)

Vontext utiliza un servicio en primer plano para la grabación de pantalla y micrófono del reproductor de bugs:

- **Tipos de Servicio en Primer Plano declarados en el Manifiesto**:
  `android:foregroundServiceType="mediaProjection|microphone"`
- **Permisos requeridos en Google Play**:
  - `FOREGROUND_SERVICE_MEDIA_PROJECTION`
  - `FOREGROUND_SERVICE_MICROPHONE`
- **Justificación para el equipo de revisión de Google Play**:
  > "Vontext is a developer and QA debugging tool that captures screen recordings and tester audio narration to generate structured bug reports. The foreground service is initiated strictly when the user activates the floating recorder bubble and grants MediaProjection consent, maintaining a persistent system notification with a visible stop control and timer while screen and microphone capture is actively in progress."
