# Formulario de Seguridad de los Datos (Google Play Data Safety)

Cuando crees la ficha de **Vontext** en Google Play Console, te pedirán completar el cuestionario de **Seguridad de los Datos (Data Safety)**. Utiliza esta guía con las respuestas exactas y sus justificaciones técnicas oficiales:

---

## 1. Respuestas para el Cuestionario de Google Play Console

| Pregunta de Google Play | Respuesta Recomendada | Justificación Técnica |
| :--- | :--- | :--- |
| **¿Tu app recopila o comparte datos de los usuarios?** | **Sí** *(al usar transcripción remota con Whisper/Gemini)* o **No** *(si solo se utilizara el motor offline).* | Declarar que recopila archivos de audio **únicamente** para la funcionalidad principal solicitada explícitamente por el usuario (transcripción y generación del reporte). |
| **¿Los datos se transfieren mediante una conexión segura (HTTPS)?** | **Sí** | Todas las llamadas de red dirigidas a OpenAI / Groq / Google Gemini se efectúan mediante canales TLS 1.3 / HTTPS cifrados de extremo a extremo. |
| **¿Permites a los usuarios solicitar que se borren sus datos?** | **Sí** | Todo se almacena localmente en el dispositivo. En la app existe el botón "Limpiar Todo" o "Eliminar" en el historial, y borrar los datos de la app desde los ajustes de Android elimina de inmediato toda la información. |
| **¿Tu app recopila datos de ubicación o identificadores personales?** | **No** | Vontext no solicita permisos de GPS/Ubicación, ni consulta IMEI, número telefónico, contactos ni identificadores de publicidad para rastreo. |
| **Tipos de datos:** | **Archivos de audio y video** *(procesados únicamente para generar el reporte de depuración).* | Los datos se procesan de forma efímera para el reporte y nunca se emplean con fines publicitarios, de rastreo comercial ni venta a intermediarios. |

---

## 2. Declaración de Servicios en Primer Plano (Foreground Service Declaration)

Para el permiso `FOREGROUND_SERVICE_SPECIAL_USE`:
- **Tipo seleccionado en Play Console**: Herramientas para desarrolladores / Productividad (`Developer Tools / Debugging`).
- **Descripción de la justificación**:
  > "Vontext uses a foreground service with a persistent notification while the user enables the floating debug bubble. This allows developers and testers to capture on-demand logcat dumps and return to the session while reproducing bugs across third-party applications. The service is explicitly initiated and terminated by the user."
