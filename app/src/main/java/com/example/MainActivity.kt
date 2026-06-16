package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Job
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoViewModel
import java.io.File
import androidx.compose.ui.platform.testTag

// --- LOCALIZATION & THEME COMPILATION ADAPTERS ---
data class AppColors(
    val BrandGreen: Color,
    val BrandDarkGreen: Color,
    val LightSage: Color,
    val CharcoalText: Color,
    val GrayDetail: Color,
    val SoftGrayBorder: Color,
    val CardBorderHighlight: Color,
    val OffWhiteBg: Color,
    val WarmWhite: Color,
    val MutedGray: Color
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        BrandGreen = Color(0xFF1A6B4A),
        BrandDarkGreen = Color(0xFF111F0E),
        LightSage = Color(0xFFD2E8D1),
        CharcoalText = Color(0xFF191C19),
        GrayDetail = Color(0xFF414941),
        SoftGrayBorder = Color(0xFFE1E4DF),
        CardBorderHighlight = Color(0xFFBFC9BA),
        OffWhiteBg = Color(0xFFFBFDF8),
        WarmWhite = Color(0xFFFFFFFF),
        MutedGray = Color(0xFF717971)
    )
}

val LocalLanguage = staticCompositionLocalOf { "es" }

@Composable
fun txt(key: String): String {
    return getTxt(key, LocalLanguage.current)
}

fun getTxt(key: String, lang: String): String {
    return when (lang) {
        "en" -> when (key) {
            "v2.1.1 • Listo" -> "v2.1.1 • Ready"
            "Inicio" -> "Home"
            "Historial" -> "History"
            "Ajustes" -> "Settings"
            "Intervalo de Captura" -> "Capture Interval"
            "Auto (Detección)" -> "Auto (Detection)"
            " segundos" -> " seconds"
            "0 = Extraerá un set uniforme de capturas automáticamente." -> "0 = Will automatically extract an even set of captures."
            "Notas Adicionales (Opcional)" -> "Additional Notes (Optional)"
            "Por ejemplo: Logs relevantes de la terminal, credenciales ficticias de demo, o instrucciones para el agente..." -> "For example: Relevant terminal logs, fictional demo credentials, or instructions for the agent..."
            "Favor de seleccionar un video primero" -> "Please select a video first"
            "Reporte Vontext generado con éxito!" -> "Vontext report generated successfully!"
            "Procesar Video" -> "Process Video"
            "Historial de Reportes" -> "Report History"
            "Limpiar Todo" -> "Clear All"
            "Historial Vacío" -> "Empty History"
            "Los reportes y archivos de contexto que generes aparecerán organizados aquí." -> "The reports and context files you generate will appear organized here."
            "Eliminar Registro" -> "Delete Record"
            "¿Deseas eliminar permanentemente este reporte de Vontext?" -> "Do you want to permanently delete this Vontext report?"
            "Eliminar" -> "Delete"
            "Cancelar" -> "Cancel"
            "Completo" -> "Completed"
            "Fallido" -> "Failed"
            "Procesando" -> "Processing"
            "Reporte #" -> "Report #"
            "Video: " -> "Video: "
            " seg" -> " sec"
            "Whisper " -> "Whisper "
            "Ver PDF" -> "View PDF"
            "Compartir ZIP" -> "Share ZIP"
            "Configuración de Procesamiento" -> "Processing Settings"
            "API Key de Whisper (OpenAI / Groq)" -> "Whisper API Key (OpenAI / Groq)"
            "Endpoint URL" -> "Endpoint URL"
            "Modelo Whisper Remoto" -> "Remote Whisper Model"
            "Muestreo por Defecto (Segundos)" -> "Default Sampling (Seconds)"
            "Acerca de" -> "About"
            "Vontext está diseñado para desarrolladores y analistas de IA. Procesa de manera segura videos convirtiendo flujos temporales a reportes legibles listos para el consumo de LLMs." -> "Vontext is designed for developers and AI analysts. It securely processes videos, converting temporal streams into readable reports ready for LLMs."
            "💾 Guarda tus cambios usando el botón al final. Vontext guardará todos tus reportes generados en el directorio público Descargas/Vontext/ de tu memoria local." -> "💾 Save your changes using the button at the bottom. Vontext will save all your generated reports in the public Downloads/Vontext/ directory of your local storage."
            "Guardar Configuración" -> "Save Configuration"
            "Restablecer" -> "Reset Defaults"
            "Configuración guardada correctamente." -> "Settings saved successfully."
            "Valores por defecto restablecidos." -> "Default values restored."
            "Modelo de Transcripción Local (Whisper)" -> "Local Transcription Model (Whisper)"
            "Descargar Whisper Local (96 MB)" -> "Download Local Whisper (96 MB)"
            "Descargando..." -> "Downloading..."
            "Eliminar Modelo" -> "Delete Model"
            "Modelo local listo y disponible offline." -> "Local model ready and available offline."
            "Requiere conexión a internet una sola vez para descargar los parámetros del codificador de audio." -> "Requires an internet connection once to download the audio encoder parameters."
            "Modelo descargado con éxito." -> "Model successfully downloaded."
            "Modelo local eliminado." -> "Local model deleted."
            "Procesando Video" -> "Processing Video"
            "Extracción de Frames" -> "Frames Extraction"
            "Transcripción Whisper" -> "Whisper Transcription"
            "Generación de PDF y ZIP" -> "PDF & ZIP Generation"
            "Logs de Consola:" -> "Console Logs:"
            "⚠️ El análisis de audio e imagen se ejecuta de forma local. No cierres la ventana." -> "⚠️ Audio and image analysis runs locally. Do not close the window."
            "Iniciando..." -> "Starting..."
            "Video seleccionado: " -> "Video selected: "
            "¿Modo Remoto?" -> "Remote Mode?"
            "Permite procesar el audio con una API externa (Whisper en OpenAI / Groq) en vez de local. Recomendado para videos de larga duración." -> "Allows processing audio with an external API (Whisper on OpenAI / Groq) instead of locally. Recommended for long video files."
            "Configuración de la Aplicación" -> "Application Settings"
            "Tema Oscuro" -> "Dark Theme"
            "Idioma" -> "Language"
            "Español" -> "Spanish"
            "Inglés" -> "English"
            "Selecciona un video de demostración o graba tu pantalla para empezar" -> "Select a demo video or record your screen to start"
            "Genera Contexto para Agentes de IA" -> "Generate AI Agent Context"
            "Procesa videos de grabación de pantalla para generar reportes PDF interactivos con capturas y transcripciones automatizadas." -> "Process screen recording videos to generate interactive PDF reports with captures and automated transcriptions."
            "¡Video Seleccionado!" -> "Video Selected!"
            "Seleccionar Video" -> "Select Video"
            "Toca aquí para seleccionar un video del almacenamiento local" -> "Tap here to select a video from local storage"
            "Modo de Transcripción Whisper" -> "Whisper Transcription Mode"
            "Local" -> "Local"
            "API Remota" -> "Remote API"
            "v2.1.1 • Listo" -> "v2.1.1 • Ready"
            else -> key
        }
        else -> key
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VideoViewModel = viewModel()
            MyApplicationTheme(darkTheme = viewModel.isDarkTheme) {
                MainAppScreen(viewModel)
            }
        }
    }
}

// --- FILE SYSTEM ACCESS HELPERS ---
fun shareFile(context: Context, filePath: String, mimeType: String) {
    try {
        val file = File(filePath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir con..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun openPdf(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se encontró un visualizador de PDF compatible. " +
                "Puedes compartirlo por ZIP o abrirlo desde la carpeta Descargas/Vontext.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun MainAppScreen(viewModel: VideoViewModel = viewModel()) {
    val context = LocalContext.current
    val allJobs by viewModel.allJobs.collectAsState()
    
    var currentTab by remember { mutableStateOf("home") } // "home", "history", "settings"

    val isDark = viewModel.isDarkTheme
    val colors = if (isDark) {
        AppColors(
            BrandGreen = Color(0xFF68B08A),
            BrandDarkGreen = Color(0xFFE1E4DF),
            LightSage = Color(0xFF233D2D),
            CharcoalText = Color(0xFFE1E4DF),
            GrayDetail = Color(0xFFA1A9A1),
            SoftGrayBorder = Color(0xFF2D332D),
            CardBorderHighlight = Color(0xFF3E483D),
            OffWhiteBg = Color(0xFF111411),
            WarmWhite = Color(0xFF1E211E),
            MutedGray = Color(0xFF8E998E)
        )
    } else {
        AppColors(
            BrandGreen = Color(0xFF1A6B4A),
            BrandDarkGreen = Color(0xFF111F0E),
            LightSage = Color(0xFFD2E8D1),
            CharcoalText = Color(0xFF191C19),
            GrayDetail = Color(0xFF414941),
            SoftGrayBorder = Color(0xFFE1E4DF),
            CardBorderHighlight = Color(0xFFBFC9BA),
            OffWhiteBg = Color(0xFFFBFDF8),
            WarmWhite = Color(0xFFFFFFFF),
            MutedGray = Color(0xFF717971)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalLanguage provides viewModel.appLanguage
    ) {
        val appColors = LocalAppColors.current
        val OffWhiteBg = appColors.OffWhiteBg

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // --- PROFESSIONAL HEADER ---
                HeaderBlock()

                // --- TAB CONTENT ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (currentTab) {
                        "home" -> HomeTabContent(viewModel, context)
                        "history" -> HistoryTabContent(viewModel, allJobs, context)
                        "settings" -> SettingsTabContent(viewModel)
                    }
                }

                // --- NAVIGATION BAR ---
                BottomNavigationBar(currentTab = currentTab, onTabSelect = { currentTab = it })
            }

            // --- ACTIVE PROCESSING OVERLAY (Professional dark & terminal logs) ---
            if (viewModel.isProcessing) {
                ProcessingOverlay(viewModel)
            }
        }
    }
}

@Composable
fun HeaderBlock() {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val CharcoalText = colors.CharcoalText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F7FF))
                    .border(1.dp, colors.SoftGrayBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier.size(54.dp)
                )
            }
            Column {
                Text(
                    text = "Vontext",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = CharcoalText
                    )
                )
                Text(
                    text = txt("v2.1.1 • Listo"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = BrandGreen
                    )
                )
            }
        }
    }
}

@Composable
fun HomeTabContent(viewModel: VideoViewModel, context: Context) {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val BrandDarkGreen = colors.BrandDarkGreen
    val LightSage = colors.LightSage
    val CharcoalText = colors.CharcoalText
    val GrayDetail = colors.GrayDetail
    val SoftGrayBorder = colors.SoftGrayBorder
    val CardBorderHighlight = colors.CardBorderHighlight
    val WarmWhite = colors.WarmWhite
    val MutedGray = colors.MutedGray

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.selectVideo(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Upper Promo banner highlighting active feature
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightSage)
                    .border(1.dp, CardBorderHighlight, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = txt("Genera Contexto para Agentes de IA"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = BrandDarkGreen
                    )
                    Text(
                        text = txt("Procesa videos de grabación de pantalla para generar reportes PDF interactivos con capturas y transcripciones automatizadas."),
                        fontSize = 13.sp,
                        color = GrayDetail,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Selected Video DropZone Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePicker.launch("video/*") }
                    .testTag("select_video_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.selectedVideoUri != null) LightSage else WarmWhite
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (viewModel.selectedVideoUri != null) BrandGreen else SoftGrayBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.selectedVideoUri != null) BrandGreen else LightSage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (viewModel.selectedVideoUri != null) Icons.Default.CheckCircle else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (viewModel.selectedVideoUri != null) WarmWhite else BrandGreen
                        )
                    }

                    if (viewModel.selectedVideoUri != null) {
                        Text(
                            text = txt("¡Video Seleccionado!"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandDarkGreen
                        )
                        Text(
                            text = viewModel.selectedVideoUri?.lastPathSegment ?: "video.mp4",
                            fontSize = 13.sp,
                            color = BrandGreen,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = txt("Seleccionar Video"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = txt("Toca aquí para seleccionar un video del almacenamiento local"),
                            fontSize = 13.sp,
                            color = GrayDetail,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Whisper Mode selector card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = txt("Modo de Transcripción Whisper"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GrayDetail
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (viewModel.whisperMode == "LOCAL") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                            .clickable { viewModel.whisperMode = "LOCAL" }
                            .padding(14.dp)
                            .testTag("whisper_mode_local"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (viewModel.whisperMode == "LOCAL") BrandGreen else GrayDetail,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = txt("Local"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (viewModel.whisperMode == "LOCAL") BrandDarkGreen else GrayDetail
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (viewModel.whisperMode == "REMOTE") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                            .clickable { viewModel.whisperMode = "REMOTE" }
                            .padding(14.dp)
                            .testTag("whisper_mode_remote"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = if (viewModel.whisperMode == "REMOTE") BrandGreen else GrayDetail,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = txt("API Remota"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (viewModel.whisperMode == "REMOTE") BrandDarkGreen else GrayDetail
                            )
                        }
                    }
                }
            }
        }

        // Captures interval slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = txt("Intervalo de Captura"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrayDetail
                    )
                    Text(
                        text = if (viewModel.frameInterval == 0) txt("Auto (Detección)") else "${viewModel.frameInterval}${txt(" segundos")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandGreen
                    )
                }

                Slider(
                    value = viewModel.frameInterval.toFloat(),
                    onValueChange = { viewModel.frameInterval = it.toInt() },
                    valueRange = 0f..30f,
                    steps = 30,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandGreen,
                        activeTrackColor = BrandGreen,
                        inactiveTrackColor = SoftGrayBorder
                    ),
                    modifier = Modifier.testTag("capture_interval_slider")
                )

                Text(
                    text = txt("0 = Extraerá un set uniforme de capturas automáticamente."),
                    fontSize = 11.sp,
                    color = MutedGray
                )
            }
        }

        // Additional notes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = txt("Notas Adicionales (Opcional)"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GrayDetail
                )
                OutlinedTextField(
                    value = viewModel.additionalNotes,
                    onValueChange = { viewModel.additionalNotes = it },
                    placeholder = {
                        Text(
                            text = txt("Por ejemplo: Logs relevantes de la terminal, credenciales ficticias de demo, o instrucciones para el agente..."),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("notes_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SoftGrayBorder,
                        focusedLabelColor = BrandGreen
                    )
                )
            }
        }

        // Action process button
        item {
            Button(
                onClick = {
                    if (viewModel.selectedVideoUri == null) {
                        Toast.makeText(context, getTxt("Favor de seleccionar un video primero", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.startProcessing { jobId ->
                        Toast.makeText(context, getTxt("Reporte Vontext generado con éxito!", viewModel.appLanguage), Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("process_video_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                enabled = viewModel.selectedVideoUri != null
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = WarmWhite)
                    Text(
                        text = txt("Procesar Video"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = WarmWhite
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(viewModel: VideoViewModel, allJobs: List<Job>, context: Context) {
    val colors = LocalAppColors.current
    val CharcoalText = colors.CharcoalText
    val GrayDetail = colors.GrayDetail
    val MutedGray = colors.MutedGray

    var jobToDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = txt("Historial de Reportes"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GrayDetail
            )
            if (allJobs.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllJobs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E)),
                    modifier = Modifier.testTag("clear_all_jobs_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(txt("Limpiar Todo"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (allJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = txt("Historial Vacío"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CharcoalText
                    )
                    Text(
                        text = txt("Los reportes y archivos de contexto que generes aparecerán organizados aquí."),
                        fontSize = 13.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(allJobs, key = { it.jobId }) { job ->
                    JobCard(
                        job = job,
                        onOpenPdf = { path -> openPdf(context, path) },
                        onShareZip = { path -> shareFile(context, path, "application/zip") },
                        onDeleteClick = { jobToDelete = job.jobId }
                    )
                }
            }
        }
    }

    if (jobToDelete != null) {
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title = { Text(txt("Eliminar Registro"), fontWeight = FontWeight.Bold) },
            text = { Text(txt("¿Deseas eliminar permanentemente este reporte de Vontext?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        jobToDelete?.let { viewModel.deleteJob(it) }
                        jobToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(txt("Eliminar"), color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { jobToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(txt("Cancelar"))
                }
            }
        )
    }
}

@Composable
fun JobCard(
    job: Job,
    onOpenPdf: (String) -> Unit,
    onShareZip: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val BrandDarkGreen = colors.BrandDarkGreen
    val LightSage = colors.LightSage
    val CharcoalText = colors.CharcoalText
    val GrayDetail = colors.GrayDetail
    val SoftGrayBorder = colors.SoftGrayBorder
    val WarmWhite = colors.WarmWhite
    val MutedGray = colors.MutedGray

    Card(
        modifier = Modifier.fillMaxWidth().testTag("job_card_${job.jobId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Status row plus timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                val statusText: String
                val pillBg: Color
                val pillColor: Color
                when (job.status) {
                    "COMPLETED" -> {
                        statusText = txt("Completo")
                        pillBg = LightSage
                        pillColor = BrandDarkGreen
                    }
                    "FAILED" -> {
                        statusText = txt("Fallido")
                        pillBg = Color(0xFFF9DEDC)
                        pillColor = Color(0xFF410E0B)
                    }
                    else -> {
                        statusText = txt("Procesando")
                        pillBg = SoftGrayBorder
                        pillColor = CharcoalText
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = pillColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Date label
                val dateStr = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(job.createdAt))
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resource title / original filename
            Text(
                text = "${txt("Reporte #")}${job.jobId.take(5).uppercase()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )
            Text(
                text = "${txt("Video: ")}${job.videoFilename}",
                fontSize = 13.sp,
                color = GrayDetail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration & stats
            if (job.videoDuration > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ ${String.format("%.1f", job.videoDuration)}${txt(" seg")}",
                        fontSize = 12.sp,
                        color = MutedGray
                    )
                    Text(
                        text = "📂 ${txt("Whisper ")}${job.whisperMode}",
                        fontSize = 12.sp,
                        color = MutedGray
                    )
                }
            }

            // Expanded short snippet transcription excerpt
            if (!job.transcriptionText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftGrayBorder.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = job.transcriptionText,
                        fontSize = 11.sp,
                        color = CharcoalText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions (Open PDF, Share ZIP, delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!job.pdfPath.isNullOrEmpty() && File(job.pdfPath).exists()) {
                    Button(
                        onClick = { onOpenPdf(job.pdfPath) },
                        modifier = Modifier.weight(1f).testTag("open_pdf_btn_${job.jobId}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(txt("Ver PDF"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!job.zipPath.isNullOrEmpty() && File(job.zipPath).exists()) {
                    Button(
                        onClick = { onShareZip(job.zipPath) },
                        modifier = Modifier.weight(1f).testTag("share_zip_btn_${job.jobId}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(txt("Compartir ZIP"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF9DEDC).copy(alpha = 0.5f))
                        .testTag("delete_job_btn_${job.jobId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = txt("Cancelar"),
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(viewModel: VideoViewModel) {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val BrandDarkGreen = colors.BrandDarkGreen
    val LightSage = colors.LightSage
    val CharcoalText = colors.CharcoalText
    val GrayDetail = colors.GrayDetail
    val SoftGrayBorder = colors.SoftGrayBorder
    val WarmWhite = colors.WarmWhite
    val MutedGray = colors.MutedGray

    var apiKey by remember { mutableStateOf(viewModel.settingsRepository.getApiKey()) }
    var endpoint by remember { mutableStateOf(viewModel.settingsRepository.getEndpoint()) }
    var model by remember { mutableStateOf(viewModel.settingsRepository.getModel()) }
    var defaultSecs by remember { mutableStateOf(viewModel.settingsRepository.getDefaultInterval().toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = txt("Configuración de Procesamiento"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GrayDetail
            )
        }

        // API settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // API Key
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("API Key de Whisper (OpenAI / Groq)"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                            },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Base url endpoint
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Endpoint URL"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = {
                                endpoint = it
                            },
                            placeholder = { Text("https://api.openai.com/v1") },
                            modifier = Modifier.fillMaxWidth().testTag("endpoint_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Whisper Model Type
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Modelo Whisper Remoto"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = model,
                            onValueChange = {
                                model = it
                            },
                            placeholder = { Text("whisper-1") },
                            modifier = Modifier.fillMaxWidth().testTag("model_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Local Whisper model management card
        item {
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = txt("Modelo de Transcripción Local (Whisper)"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayDetail
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = txt("Requiere conexión a internet una sola vez para descargar los parámetros del codificador de audio."),
                            fontSize = 12.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
                        )

                        if (viewModel.isWhisperLocalDownloaded) {
                            // Model is ready
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                    )
                                    Text(
                                        text = txt("Modelo local listo y disponible offline."),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CharcoalText
                                    )
                                }
                                
                                TextButton(
                                    onClick = {
                                        viewModel.deleteLocalWhisper {
                                            Toast.makeText(context, getTxt("Modelo local eliminado.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    modifier = Modifier.testTag("delete_whisper_btn")
                                ) {
                                    Text(txt("Eliminar Modelo"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            // Model is not ready / downloading
                            if (viewModel.isDownloadingLocalWhisper) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = txt("Descargando..."),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen
                                        )
                                        Text(
                                            text = "${(viewModel.localWhisperDownloadProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandDarkGreen
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { viewModel.localWhisperDownloadProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = BrandGreen,
                                        trackColor = SoftGrayBorder
                                    )
                                    Text(
                                        text = viewModel.localWhisperDownloadStatus,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GrayDetail
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.downloadLocalWhisper(
                                            onSuccess = { msg ->
                                                Toast.makeText(context, getTxt("Modelo descargado con éxito.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("download_whisper_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                                ) {
                                    Text(
                                        text = txt("Descargar Whisper Local (96 MB)"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interface Preferences Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = txt("Preferencias de Interfaz"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayDetail
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Theme switches
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(txt("Tema visual"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (!viewModel.isDarkTheme) LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                        .clickable { viewModel.updateDarkTheme(false) }
                                        .testTag("theme_light_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = txt("Claro"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!viewModel.isDarkTheme) BrandDarkGreen else GrayDetail
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (viewModel.isDarkTheme) LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                        .clickable { viewModel.updateDarkTheme(true) }
                                        .testTag("theme_dark_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = txt("Oscuro"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (viewModel.isDarkTheme) BrandDarkGreen else GrayDetail
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SoftGrayBorder))

                        // Language switches
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(txt("Idioma de la aplicación"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (viewModel.appLanguage == "es") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                        .clickable { viewModel.updateLanguage("es") }
                                        .testTag("lang_es_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Español",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (viewModel.appLanguage == "es") BrandDarkGreen else GrayDetail
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (viewModel.appLanguage == "en") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                        .clickable { viewModel.updateLanguage("en") }
                                        .testTag("lang_en_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "English",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (viewModel.appLanguage == "en") BrandDarkGreen else GrayDetail
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // App general settings Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Default seconds interval
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Muestreo por Defecto (Segundos)"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = defaultSecs,
                            onValueChange = {
                                defaultSecs = it
                            },
                            placeholder = { Text("5") },
                            modifier = Modifier.fillMaxWidth().testTag("default_interval_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // App description attribution info
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Acerca de"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        Text(
                            text = txt("Vontext está diseñado para desarrolladores y analistas de IA. Procesa de manera segura videos convirtiendo flujos temporales a reportes legibles listos para el consumo de LLMs."),
                            fontSize = 12.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Visual notice
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(LightSage.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Text(
                    text = txt("💾 Guarda tus cambios usando el botón al final. Vontext guardará todos tus reportes generados en el directorio público Descargas/Vontext/ de tu memoria local."),
                    fontSize = 12.sp,
                    color = BrandDarkGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Buttons: Save and Reset Defaults
        item {
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset settings to program defaults
                        viewModel.settingsRepository.setApiKey("")
                        viewModel.settingsRepository.setEndpoint("https://api.openai.com/v1")
                        viewModel.settingsRepository.setModel("whisper-1")
                        viewModel.settingsRepository.setDefaultInterval(5)
                        
                        // Sync local view states
                        apiKey = ""
                        endpoint = "https://api.openai.com/v1"
                        model = "whisper-1"
                        defaultSecs = "5"
                        
                        Toast.makeText(context, getTxt("Valores por defecto restablecidos.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reset_defaults_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen)
                ) {
                    Text(text = txt("Restablecer"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        // Persist local view states to repository
                        viewModel.settingsRepository.setApiKey(apiKey)
                        viewModel.settingsRepository.setEndpoint(endpoint)
                        viewModel.settingsRepository.setModel(model)
                        val valInterval = defaultSecs.toIntOrNull() ?: 5
                        viewModel.settingsRepository.setDefaultInterval(valInterval)
                        
                        Toast.makeText(context, getTxt("Configuración guardada correctamente.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text(text = txt("Guardar Configuración"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: String, onTabSelect: (String) -> Unit) {
    val colors = LocalAppColors.current
    val SoftGrayBorder = colors.SoftGrayBorder
    val WarmWhite = colors.WarmWhite

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        color = WarmWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = txt("Inicio"),
                isActive = currentTab == "home",
                onClick = { onTabSelect("home") }
            )
            BottomNavItem(
                icon = Icons.Default.DateRange,
                label = txt("Historial"),
                isActive = currentTab == "history",
                onClick = { onTabSelect("history") }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = txt("Ajustes"),
                isActive = currentTab == "settings",
                onClick = { onTabSelect("settings") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val BrandDarkGreen = colors.BrandDarkGreen
    val LightSage = colors.LightSage
    val GrayDetail = colors.GrayDetail

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("nav_item_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isActive) LightSage else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) BrandDarkGreen else GrayDetail,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) BrandDarkGreen else GrayDetail
        )
    }
}

@Composable
fun ProcessingOverlay(viewModel: VideoViewModel) {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val BrandDarkGreen = colors.BrandDarkGreen
    val LightSage = colors.LightSage
    val CharcoalText = colors.CharcoalText
    val GrayDetail = colors.GrayDetail
    val SoftGrayBorder = colors.SoftGrayBorder
    val CardBorderHighlight = colors.CardBorderHighlight
    val WarmWhite = colors.WarmWhite
    val MutedGray = colors.MutedGray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = WarmWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderHighlight)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with custom green container & percent counter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(LightSage)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = txt("Procesando Video"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandDarkGreen
                            )
                            Text(
                                text = txt(viewModel.currentStatusMessage),
                                fontSize = 12.sp,
                                color = BrandGreen
                            )
                        }

                        // Circular percent
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WarmWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${viewModel.currentProgress}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = BrandGreen
                            )
                        }
                    }
                }

                // Progress Indicator
                LinearProgressIndicator(
                    progress = viewModel.currentProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = BrandGreen,
                    trackColor = SoftGrayBorder
                )

                // Processing dynamic checkmarks
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProcessingStepRow(
                        stepName = txt("Extracción de Frames"),
                        isActive = viewModel.currentProgress >= 10 && viewModel.currentProgress < 55,
                        isDone = viewModel.currentProgress >= 55
                    )
                    ProcessingStepRow(
                        stepName = txt("Transcripción Whisper"),
                        isActive = viewModel.currentProgress >= 55 && viewModel.currentProgress < 75,
                        isDone = viewModel.currentProgress >= 75
                    )
                    ProcessingStepRow(
                        stepName = txt("Generación de PDF y ZIP"),
                        isActive = viewModel.currentProgress >= 75 && viewModel.currentProgress < 100,
                        isDone = viewModel.currentProgress >= 100
                    )
                }

                // Live dynamic console logs (monospaced)
                Text(
                    text = txt("Logs de Consola:"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GrayDetail
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E211E)) // Dark logs background
                        .padding(12.dp)
                ) {
                    val logList = viewModel.logs.value
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logList) { log ->
                            Text(
                                text = log,
                                color = Color(0xFF68B08A), // Retro hacker green
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                Text(
                    text = txt("⚠️ El análisis de audio e imagen se ejecuta de forma local. No cierres la ventana."),
                    fontSize = 11.sp,
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ProcessingStepRow(stepName: String, isActive: Boolean, isDone: Boolean) {
    val colors = LocalAppColors.current
    val BrandGreen = colors.BrandGreen
    val LightSage = colors.LightSage
    val SoftGrayBorder = colors.SoftGrayBorder
    val WarmWhite = colors.WarmWhite
    val CharcoalText = colors.CharcoalText
    val MutedGray = colors.MutedGray

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> BrandGreen
                        isActive -> LightSage
                        else -> SoftGrayBorder.copy(alpha = 0.5f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = WarmWhite,
                    modifier = Modifier.size(12.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = BrandGreen
                )
            }
        }

        Text(
            text = stepName,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isDone -> CharcoalText
                isActive -> BrandGreen
                else -> MutedGray
            }
        )
    }
}
