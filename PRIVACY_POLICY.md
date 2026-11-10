# Política de Privacidad de Vontext

**Última actualización: 10 de Noviembre de 2026**

Vontext ("nosotros", "la aplicación") es una herramienta de productividad y depuración desarrollada para transformar grabaciones de pantalla y voz en resúmenes técnicos estructurados y reportes técnicos para desarrolladores y agentes de IA.

---

### 1. Información que procesamos y transmitimos

- **Grabaciones de video y capturas de pantalla (Fotos y Videos):** 
  - **Procesamiento Local:** Los videos seleccionados o grabados por el usuario se procesan principalmente de manera local dentro del dispositivo para extraer fotogramas clave y ejecutar reconocimiento de texto en pantalla (OCR) sin conexión a internet.
  - **Transmisión en Modo Gemini (Multimodal):** Cuando el usuario decide activar y configurar voluntariamente un modelo externo de Google Gemini, **fotogramas seleccionados (capturas de pantalla / imágenes)** del video y notas técnicas se transmiten de forma cifrada (HTTPS/TLS 1.3) a los servidores de la API de Google Gemini para realizar el análisis visual y la síntesis técnica solicitada por el usuario.
- **Grabaciones de audio y voz:**
  - **Modo Local (Offline):** Cuando se utiliza el motor integrado (Vosk), todo el procesamiento de voz y transcripción se ejecuta de forma 100% local en el dispositivo sin enviar audio a ninguna red.
  - **Modo Remoto (Whisper / OpenAI / Groq):** Si el usuario elige usar APIs remotas, el audio narrado se envía de forma cifrada (HTTPS/TLS 1.3) exclusivamente al endpoint del proveedor que el propio usuario haya configurado.
- **Registros técnicos del sistema (Logcat / Diagnóstico):**
  - A solicitud explícita del usuario, la aplicación puede capturar un extracto de eventos del sistema (errores y excepciones `AndroidRuntime` / `FATAL`) durante el periodo de reproducción del bug.
  - **Redacción de Privacidad:** Todos los registros pasan por un filtro automático que reemplaza por `[REDACTED]` cualquier token de autenticación (`Bearer`), claves de API (`sk-...`, `AIza...`), tokens JWT (`eyJ...`), contraseñas y correos electrónicos antes de ser almacenados o exportados.
- **Claves de API y Credenciales:**
  - Las claves de API introducidas por el usuario se almacenan en el dispositivo protegidas mediante **cifrado criptográfico de hardware con Android Keystore (AES-256-GCM)**.
  - El archivo de credenciales está explícitamente excluido del servicio de copia de seguridad en la nube de Google (`Android Cloud Backup` / `data-extraction-rules`), impidiendo su sincronización no autorizada.

---

### 2. Procesamiento Local vs. Servicios de Terceros

- **Modo Local por Defecto:** Vontext puede funcionar de forma 100% autónoma y desconectada usando Vosk y ML Kit on-device.
- **Servicios de Terceros Opcionales:** La conexión a APIs remotas (Google Gemini, OpenAI, Groq) es totalmente opcional, requiere que el usuario ingrese sus propias credenciales y solo se activa cuando el usuario elige expresamente dichos modelos. No compartimos datos con intermediarios publicitarios ni redes de monetización.

---

### 3. Almacenamiento, Retención y Control del Usuario

- Todos los artefactos generados (reportes PDF, capturas de pantalla, archivos ZIP y registros logcat) se almacenan en el almacenamiento local del dispositivo o se exportan a la carpeta pública de Descargas (`MediaStore.Downloads`) bajo control exclusivo del usuario.
- El usuario puede eliminar cualquier trabajo, archivo temporal o historial completo en cualquier momento pulsando "Eliminar" o "Limpiar Todo" desde la pestaña de Historial de la aplicación.
- Desinstalar la aplicación o limpiar sus datos en el sistema elimina de manera definitiva todas las grabaciones, credenciales cifradas y registros generados.

---

### 4. Permisos de Servicios en Primer Plano

- Para la funcionalidad de grabación de bugs en pantalla y micrófono narrado, Vontext emplea un servicio en primer plano con los tipos específicos `mediaProjection` y `microphone` (`android:foregroundServiceType="mediaProjection|microphone"`).
- Este servicio muestra una notificación persistente y un control visible mientras la sesión de captura de pantalla y audio está activa, siendo iniciado y detenido exclusivamente por la acción del usuario.

---

### 5. Contacto

Si tienes preguntas, dudas o inquietudes sobre esta política de privacidad o sobre el tratamiento de datos en Vontext, puedes contactarnos a través de:

- **Correo electrónico de contacto:** [CONTACT_EMAIL] (soporte@vontext.dev)
- **Repositorio de soporte:** https://github.com/laar19/vontext2
