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
        BrandGreen = Color(0xFF1E52B3),
        BrandDarkGreen = Color(0xFF0F1E36),
        LightSage = Color(0xFFD6E4F0),
        CharcoalText = Color(0xFF191C19),
        GrayDetail = Color(0xFF414941),
        SoftGrayBorder = Color(0xFFE1E4DF),
        CardBorderHighlight = Color(0xFFBFC9BA),
        OffWhiteBg = Color(0xFFF8FAFC),
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
            "v2.1.3" -> "v2.1.3"
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
            "Vosk Local (Offline)" -> "Local Vosk (Offline)"
            "Listo para usar offline (96 MB)" -> "Ready for offline use (96 MB)"
            "Requiere descarga para uso local" -> "Requires download for local use"
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
            "Modelo de Transcripción Local (Vosk)" -> "Local Transcription Model (Vosk)"
            "Descargar Vosk Local (96 MB)" -> "Download Local Vosk (96 MB)"
            "Descargando..." -> "Downloading..."
            "Eliminar Modelo" -> "Delete Model"
            "Modelo local listo y disponible offline (Vosk)." -> "Local model ready and available offline (Vosk)."
            "Requiere conexión a internet una sola vez para descargar los parámetros del codificador de audio." -> "Requires an internet connection once to download the audio encoder parameters."
            "Modelo descargado con éxito." -> "Model successfully downloaded."
            "Modelo local eliminado." -> "Local model deleted."
            "Procesando Video" -> "Processing Video"
            "Extracción de Frames" -> "Frames Extraction"
            "Transcripción de Audio" -> "Audio Transcription"
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
            "Cancelar Procesamiento" -> "Cancel Processing"
            "Seleccionar Video" -> "Select Video"
            "Toca aquí para seleccionar un video del almacenamiento local" -> "Tap here to select a video from local storage"
            "Modo de Transcripción y Análisis" -> "Transcription & Analysis Mode"
            "Local (Esp)" -> "Local (Sp)"
            "Gemini API" -> "Gemini API"
            "API Remote" -> "Remote API"
            "v2.1.3" -> "v2.1.3"
            "Seleccionar Videos" -> "Select Videos"
            "Toca aquí para seleccionar uno o varios videos del almacenamiento local" -> "Tap here to select one or multiple videos from local storage"
            "Agrupamiento de Reportes" -> "Report Grouping"
            "Juntos (Un Reporte)" -> "Together (One Report)"
            "Separados (N Reportes)" -> "Separated (N Reports)"
            else -> {
                if (key.startsWith("¡") && key.endsWith("Videos Seleccionados!")) {
                    val count = key.substring(1, key.indexOf(" "))
                    "$count Videos Selected!"
                } else if (key.startsWith("¡1 Video Seleccionado!")) {
                    "1 Video Selected!"
                } else {
                    key
                }
            }
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
fun getUriForFileResilient(context: Context, file: File): Uri {
    val potentialAuthorities = listOf(
        "${context.packageName}.fileprovider",
        "com.aistudio.vontext.hzqypl.fileprovider",
        "com.example.fileprovider"
    )
    var lastException: Exception? = null
    for (authority in potentialAuthorities) {
        try {
            return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            lastException = e
        }
    }
    throw lastException ?: IllegalArgumentException("No se encontró un proveedor de archivos válido.")
}

fun shareFile(context: Context, filePath: String, mimeType: String) {
    try {
        val file = File(filePath)
        val uri = getUriForFileResilient(context, file)
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
        val uri = getUriForFileResilient(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se encontró un visualizador de PDF compatible. Abriendo menú para compartir...", Toast.LENGTH_LONG).show()
        shareFile(context, filePath, "application/pdf")
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
            BrandGreen = Color(0xFF6B99E5),
            BrandDarkGreen = Color(0xFFE2E8F0),
            LightSage = Color(0xFF1F2D40),
            CharcoalText = Color(0xFFE2E8F0),
            GrayDetail = Color(0xFF94A3B8),
            SoftGrayBorder = Color(0xFF2D332D),
            CardBorderHighlight = Color(0xFF3E483D),
            OffWhiteBg = Color(0xFF0F172A),
            WarmWhite = Color(0xFF1E293B),
            MutedGray = Color(0xFF8E998E)
        )
    } else {
        AppColors(
            BrandGreen = Color(0xFF1E52B3),
            BrandDarkGreen = Color(0xFF0F1E36),
            LightSage = Color(0xFFD6E4F0),
            CharcoalText = Color(0xFF191C19),
            GrayDetail = Color(0xFF414941),
            SoftGrayBorder = Color(0xFFE1E4DF),
            CardBorderHighlight = Color(0xFFBFC9BA),
            OffWhiteBg = Color(0xFFF8FAFC),
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
                    text = txt("v2.1.3"),
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
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.selectVideos(uris)
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

        // Selected Videos DropZone Card
        item {
            val isSelected = viewModel.selectedVideoUris.isNotEmpty()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePicker.launch("video/*") }
                    .testTag("select_video_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) LightSage else WarmWhite
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) BrandGreen else SoftGrayBorder
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
                            .background(if (isSelected) BrandGreen else LightSage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isSelected) WarmWhite else BrandGreen
                        )
                    }

                    if (isSelected) {
                        val count = viewModel.selectedVideoUris.size
                        Text(
                            text = if (count == 1) txt("¡1 Video Seleccionado!") else txt("¡$count Videos Seleccionados!"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandDarkGreen
                        )
                        Text(
                            text = viewModel.selectedVideoUris.joinToString(", ") { it.lastPathSegment ?: "video.mp4" },
                            fontSize = 13.sp,
                            color = BrandGreen,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = txt("Seleccionar Videos"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = txt("Toca aquí para seleccionar uno o varios videos del almacenamiento local"),
                            fontSize = 13.sp,
                            color = GrayDetail,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Multiple files grouping mode selector card
        if (viewModel.selectedVideoUris.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = txt("Agrupamiento de Reportes"),
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
                                .background(if (viewModel.multipleFilesMode == "JUNTOS") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                .clickable { viewModel.multipleFilesMode = "JUNTOS" }
                                .padding(14.dp)
                                .testTag("multi_mode_juntos"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (viewModel.multipleFilesMode == "JUNTOS") BrandGreen else GrayDetail,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = txt("Juntos (Un Reporte)"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (viewModel.multipleFilesMode == "JUNTOS") BrandDarkGreen else GrayDetail
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (viewModel.multipleFilesMode == "SEPARADOS") LightSage else SoftGrayBorder.copy(alpha = 0.5f))
                                .clickable { viewModel.multipleFilesMode = "SEPARADOS" }
                                .padding(14.dp)
                                .testTag("multi_mode_separados"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = if (viewModel.multipleFilesMode == "SEPARADOS") BrandGreen else GrayDetail,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = txt("Separados (N Reportes)"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (viewModel.multipleFilesMode == "SEPARADOS") BrandDarkGreen else GrayDetail
                                )
                            }
                        }
                    }
                }
            }
        }

        // Whisper Mode selector card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = txt("Modo de Transcripción y Análisis"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GrayDetail
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Whisper Local option
                        val isLocal = viewModel.whisperMode == "LOCAL"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLocal) LightSage.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { viewModel.updateWhisperMode("LOCAL") }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isLocal,
                                onClick = { viewModel.updateWhisperMode("LOCAL") },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                modifier = Modifier.testTag("whisper_mode_local_radio")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = txt("Vosk Local (Offline)"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalText
                                )
                                val isDownloaded = viewModel.downloadedVoskLanguages.contains(viewModel.activeVoskLanguage)
                                val activeModelLabel = viewModel.availableVoskModels.find { it.code == viewModel.activeVoskLanguage }?.displayName ?: viewModel.activeVoskLanguage.uppercase()
                                Text(
                                    text = if (isDownloaded) "${txt("Listo para usar offline")} ($activeModelLabel)" else "${txt("Requiere descarga para uso local")} ($activeModelLabel)",
                                    fontSize = 12.sp,
                                    color = MutedGray
                                )
                            }

                            val isDownloaded = viewModel.downloadedVoskLanguages.contains(viewModel.activeVoskLanguage)
                            if (!isDownloaded) {
                                if (viewModel.isDownloadingLocalWhisper) {
                                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { viewModel.localWhisperDownloadProgress },
                                            strokeWidth = 2.dp,
                                            color = BrandGreen,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    val contextForToast = LocalContext.current
                                    IconButton(
                                        onClick = {
                                            viewModel.downloadLocalWhisper(
                                                viewModel.activeVoskLanguage,
                                                onSuccess = { msg ->
                                                    Toast.makeText(contextForToast, msg, Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(contextForToast, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        modifier = Modifier.testTag("download_whisper_inline")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Descargar Vosk Local",
                                            tint = BrandGreen
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Pre-configured Gemini API (Conditional show)
                        val isGeminiConfigured = run {
                            val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY
                            val isBuildConfigValid = buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.length > 10
                            isBuildConfigValid || viewModel.settingsRepository.getApiKey().isNotEmpty()
                        }
                        if (isGeminiConfigured) {
                            val isGemini = viewModel.whisperMode == "GEMINI"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isGemini) LightSage.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { viewModel.updateWhisperMode("GEMINI") }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isGemini,
                                    onClick = { viewModel.updateWhisperMode("GEMINI") },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                    modifier = Modifier.testTag("whisper_mode_gemini_radio")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = txt("Gemini API (Plataforma)"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = txt("Multimodal y rápido (gemini-1.5-flash)"),
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                            }
                        }

                        // 3. Pre-configured Remote Whisper (Conditional show)
                        val isRemoteConfigured = viewModel.settingsRepository.getApiKey().isNotEmpty()
                        if (isRemoteConfigured) {
                            val isRemote = viewModel.whisperMode == "REMOTE"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRemote) LightSage.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { viewModel.updateWhisperMode("REMOTE") }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isRemote,
                                    onClick = { viewModel.updateWhisperMode("REMOTE") },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                    modifier = Modifier.testTag("whisper_mode_remote_radio")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = txt("Whisper Remoto (Estándar)"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = txt("Vía Endpoint configurado en ajustes"),
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                            }
                        }

                        // 4. Dynamic custom configurations
                        viewModel.customModelsList.forEach { customModel ->
                            val isCustomSel = viewModel.whisperMode == customModel.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCustomSel) LightSage.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { viewModel.updateWhisperMode(customModel.id) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCustomSel,
                                    onClick = { viewModel.updateWhisperMode(customModel.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                    modifier = Modifier.testTag("whisper_mode_custom_${customModel.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = customModel.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = "${if (customModel.type == "GEMINI") "Gemini API" else "Ajuste OpenAI"} - ${customModel.modelName}",
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Include timestamps toggle item
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateIncludeTimestamps(!viewModel.includeTimestamps) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = txt("Incluir marcas de tiempo (Timestamps)"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = txt("Mostrar marcas [mm:ss] al lado de la transcripción en el PDF"),
                            fontSize = 11.sp,
                            color = MutedGray
                        )
                    }
                    Switch(
                        checked = viewModel.includeTimestamps,
                        onCheckedChange = { viewModel.updateIncludeTimestamps(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandGreen,
                            uncheckedThumbColor = GrayDetail,
                            uncheckedTrackColor = SoftGrayBorder
                        ),
                        modifier = Modifier.testTag("include_timestamps_switch")
                    )
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
            val hasSelected = viewModel.selectedVideoUris.isNotEmpty()
            Button(
                onClick = {
                    if (viewModel.selectedVideoUris.isEmpty()) {
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
                enabled = hasSelected
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

        // Card to Add custom list of models
        item {
            var newModelName by remember { mutableStateOf("") }
            var newModelType by remember { mutableStateOf("GEMINI") } // "GEMINI" or "OPENAI_COMPATIBLE"
            var newModelApiKey by remember { mutableStateOf("") }
            var newModelEndpoint by remember { mutableStateOf("https://generativelanguage.googleapis.com") }
            var newModelNameCode by remember { mutableStateOf("gemini-1.5-flash") }
            var isDropdownExpanded by remember { mutableStateOf(false) }

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
                        text = txt("Agregar Nuevo Modelo o API"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandDarkGreen
                    )

                    // 1. Dropdown for API selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Tipo de API"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Transparent)
                                .clickable { isDropdownExpanded = true }
                        ) {
                            OutlinedTextField(
                                value = if (newModelType == "GEMINI") "Gemini API" else "OpenAI Compatible",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { isDropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("custom_model_type_field"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(WarmWhite)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gemini API") },
                                    onClick = {
                                        newModelType = "GEMINI"
                                        if (newModelEndpoint.isEmpty() || newModelEndpoint.contains("api.openai.com")) {
                                            newModelEndpoint = "https://generativelanguage.googleapis.com"
                                        }
                                        if (newModelNameCode.isEmpty() || newModelNameCode.contains("whisper")) {
                                            newModelNameCode = "gemini-1.5-flash"
                                        }
                                        isDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("dropdown_type_gemini")
                                )
                                DropdownMenuItem(
                                    text = { Text("OpenAI Compatible (Groq, Whisper, etc.)") },
                                    onClick = {
                                        newModelType = "OPENAI_COMPATIBLE"
                                        if (newModelEndpoint.isEmpty() || newModelEndpoint.contains("generativelanguage")) {
                                            newModelEndpoint = "https://api.openai.com/v1"
                                        }
                                        if (newModelNameCode.isEmpty() || newModelNameCode.contains("gemini")) {
                                            newModelNameCode = "whisper-1"
                                        }
                                        isDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("dropdown_type_openai")
                                )
                            }
                        }
                    }

                    // 2. Custom Config Name
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Nombre de Configuración"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = newModelName,
                            onValueChange = { newModelName = it },
                            placeholder = { Text("Ej. Mi Gemini Pro o Groq Whisper") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_config_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 3. API Key
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("API Key"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = newModelApiKey,
                            onValueChange = { newModelApiKey = it },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_config_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 4. Endpoint URL
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Endpoint URL"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = newModelEndpoint,
                            onValueChange = { newModelEndpoint = it },
                            placeholder = { Text(if (newModelType == "GEMINI") "https://generativelanguage.googleapis.com" else "https://api.openai.com/v1") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_config_endpoint_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 5. Model Code Name
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(txt("Nombre del modelo"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        OutlinedTextField(
                            value = newModelNameCode,
                            onValueChange = { newModelNameCode = it },
                            placeholder = { Text(if (newModelType == "GEMINI") "gemini-1.5-flash" else "whisper-1") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_config_model_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 6. Action button to add
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (newModelName.isBlank()) {
                                Toast.makeText(context, getTxt("Ingresa un nombre para la configuración.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addCustomModel(
                                    configName = newModelName,
                                    type = newModelType,
                                    apiKey = newModelApiKey,
                                    endpoint = newModelEndpoint,
                                    modelName = newModelNameCode
                                )
                                Toast.makeText(context, getTxt("Modelo agregado correctamente.", viewModel.appLanguage), Toast.LENGTH_SHORT).show()
                                // Clear inputs
                                newModelName = ""
                                newModelApiKey = ""
                                if (newModelType == "GEMINI") {
                                    newModelEndpoint = "https://generativelanguage.googleapis.com"
                                    newModelNameCode = "gemini-1.5-flash"
                                } else {
                                    newModelEndpoint = "https://api.openai.com/v1"
                                    newModelNameCode = "whisper-1"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("add_custom_model_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                    ) {
                        Text(
                            text = txt("Agregar Modelo"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                    text = txt("Modelos de Idioma de Transcripción Local (Vosk)"),
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = txt("Para procesar de forma local y 100% offline. Selecciona cuál idioma usar para la transcripción y descarga su respectivo paquete de parámetros."),
                            fontSize = 12.sp,
                            color = MutedGray,
                            lineHeight = 16.sp
                        )

                        viewModel.availableVoskModels.forEach { targetModel ->
                            val code = targetModel.code
                            val isDownloaded = viewModel.downloadedVoskLanguages.contains(code)
                            val isActive = viewModel.activeVoskLanguage == code
                            val isDownloadingThis = viewModel.isDownloadingLocalWhisper && viewModel.downloadingLocalWhisperLangCode == code

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) LightSage.copy(alpha = 0.4f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isActive) BrandGreen.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RadioButton to set as Active
                                    RadioButton(
                                        selected = isActive,
                                        onClick = {
                                            viewModel.selectActiveVoskLanguage(code)
                                            Toast.makeText(context, "${targetModel.displayName} ${getTxt("seleccionado", viewModel.appLanguage)}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    
                                    // Language flag/pill badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(LightSage)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = code.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetModel.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalText
                                        )
                                        Text(
                                            text = targetModel.size,
                                            fontSize = 11.sp,
                                            color = GrayDetail
                                        )
                                    }

                                    // Action buttons (Download, Delete, Progress, Done)
                                    if (isDownloadingThis) {
                                        // Row shows indicator on the right instead of button
                                        Text(
                                            text = "${(viewModel.localWhisperDownloadProgress * 100).toInt()}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen
                                        )
                                    } else if (isDownloaded) {
                                        // Small Trash Can icon button to delete model
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteLocalWhisper(code) {
                                                    Toast.makeText(context, "${targetModel.displayName} ${getTxt("eliminado", viewModel.appLanguage)}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar ${targetModel.displayName}",
                                                tint = Color(0xFFB3261E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        // Download button
                                        IconButton(
                                            onClick = {
                                                if (viewModel.isDownloadingLocalWhisper) {
                                                    Toast.makeText(context, "Espere a que termine la descarga actual", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.downloadLocalWhisper(
                                                        code,
                                                        onSuccess = { msg ->
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Descargar ${targetModel.displayName}",
                                                tint = BrandGreen,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // Show progress bar if downloading this specific model
                                if (isDownloadingThis) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { viewModel.localWhisperDownloadProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
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
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- CUSTOM MODELS SECTION ---
        item {
            Text(
                text = txt("Modelos y APIs Personalizados"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GrayDetail
            )
        }

        // Custom models list (with delete buttons)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGrayBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.customModelsList.isEmpty()) {
                        Text(
                            text = txt("No has agregado ningún modelo personalizado todavía."),
                            fontSize = 13.sp,
                            color = MutedGray
                        )
                    } else {
                        viewModel.customModelsList.forEach { customModel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customModel.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = "${if (customModel.type == "GEMINI") "Gemini API" else "Ajuste OpenAI"} - ${customModel.modelName}",
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCustomModel(customModel.id) },
                                    modifier = Modifier.testTag("delete_custom_model_${customModel.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar Modelo",
                                        tint = Color(0xFFB3261E)
                                    )
                                }
                            }
                            if (customModel != viewModel.customModelsList.last()) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SoftGrayBorder.copy(alpha = 0.5f)))
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

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SoftGrayBorder))

                        // Timestamps Switch preference
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateIncludeTimestamps(!viewModel.includeTimestamps) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = txt("Incluir marcas de tiempo (Timestamps) en el PDF"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CharcoalText
                                )
                                Text(
                                    text = txt("Muestra las etiquetas de minutos y segundos al lado de las transcripciones."),
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = viewModel.includeTimestamps,
                                onCheckedChange = { viewModel.updateIncludeTimestamps(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandGreen,
                                    uncheckedThumbColor = GrayDetail,
                                    uncheckedTrackColor = SoftGrayBorder
                                ),
                                modifier = Modifier.testTag("include_timestamps_settings_switch")
                            )
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
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("reset_defaults_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
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
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = txt("Guardar Configuración"), 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 13.sp, 
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
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
                        stepName = txt("Transcripción de Audio"),
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

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { viewModel.cancelProcessing() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cancel_processing_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = txt("Cancelar Procesamiento"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
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
