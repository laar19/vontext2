# Política de Privacidad de Vontext

**Última actualización: Septiembre 2026**

Vontext ("nosotros", "la aplicación") es una herramienta de productividad y depuración desarrollada para transformar grabaciones de pantalla y voz en resúmenes técnicos estructurados.

---

### 1. Información que procesamos
- **Grabaciones de video y audio:** Los archivos seleccionados por el usuario se procesan localmente en el dispositivo para extraer cuadros clave y transcribir el audio narrado.
- **Registros técnicos (Logcat):** A solicitud del usuario, la aplicación puede generar un extracto de eventos del sistema (errores y advertencias) con el único fin de diagnosticar fallos en pruebas técnicas.
- **Claves de API (Opcional):** Si el usuario configura sus propias claves para modelos externos (por ejemplo, Whisper o Gemini), estas se almacenan exclusivamente de manera local y cifrada en el dispositivo.

---

### 2. Procesamiento Local vs. Servicios de Terceros
- **Modo Local (Offline):** Cuando se utiliza el motor integrado (Vosk), todo el procesamiento de voz y análisis de imágenes se realiza 100% dentro del dispositivo, sin requerir conexión a internet.
- **Modo Remoto (Opcional):** Si el usuario decide utilizar APIs externas de IA, los fragmentos de audio necesarios se envían de forma cifrada (HTTPS) directamente a los endpoints del proveedor configurado por el usuario.

---

### 3. Almacenamiento y Control del Usuario
- Todos los artefactos generados (PDFs, capturas e informes ZIP) se guardan en el almacenamiento local del usuario.
- El usuario puede eliminar cualquier reporte o registro en cualquier momento desde la pantalla de Historial de la aplicación.
- No recopilamos, no vendemos ni compartimos datos personales, ubicaciones ni telemetría comercial con terceros.

---

### 4. Contacto
Si tienes preguntas o inquietudes sobre esta política, puedes contactarnos a través de nuestro repositorio oficial de soporte.
