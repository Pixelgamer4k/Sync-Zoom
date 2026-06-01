package com.example

import android.content.Intent
import androidx.compose.foundation.layout.widthIn
import android.content.Context
import android.graphics.ImageDecoder
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.CropDin
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Face
import android.net.Uri
import android.os.Build
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.runtime.compositionLocalOf
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log2
import kotlin.math.pow


val LocalHazeState = compositionLocalOf<HazeState?> { null }

fun saveHistory(context: Context, history: List<List<Uri>>) {
    val prefs = context.getSharedPreferences("synczoom_prefs", Context.MODE_PRIVATE)
    val stringRep = history.joinToString("|") { uris -> uris.joinToString(",") { it.toString() } }
    prefs.edit().putString("history_ordered", stringRep).apply()
}

fun loadHistory(context: Context): List<List<Uri>> {
    val prefs = context.getSharedPreferences("synczoom_prefs", Context.MODE_PRIVATE)
    val stringRep = prefs.getString("history_ordered", "") ?: ""
    if (stringRep.isEmpty()) return emptyList()
    return stringRep.split("|").map { group ->
        group.split(",").map { Uri.parse(it) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var crashDump by mutableStateOf<String?>(null)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            crashDump = sw.toString()
            android.util.Log.e("CRASH_DUMP", crashDump!!)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                if (crashDump != null) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Red).padding(16.dp)) {
                        Text(
                            text = crashDump ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    SyncZoomApp()
                }
            }
        }
    }
}

class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
}

data class ImageHistogram(
    val red: List<Float>,
    val green: List<Float>,
    val blue: List<Float>,
    val luma: List<Float>,
    val waveform: android.graphics.Bitmap? = null,
    val vectorscope: android.graphics.Bitmap? = null
)

suspend fun extractExifMetadata(context: Context, uri: Uri): Map<String, String> {
    return withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>()
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx != -1) metadata["File Name"] = it.getString(nameIdx)
                    if (sizeIdx != -1) {
                        val sizeBytes = it.getLong(sizeIdx)
                        metadata["File Size"] = String.format(java.util.Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0))
                    }
                }
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                val attributes = listOf(
                    android.media.ExifInterface.TAG_DATETIME to "Date & Time",
                    android.media.ExifInterface.TAG_MAKE to "Make",
                    android.media.ExifInterface.TAG_MODEL to "Model",
                    android.media.ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
                    android.media.ExifInterface.TAG_F_NUMBER to "F-Number",
                    android.media.ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
                    android.media.ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO",
                    android.media.ExifInterface.TAG_IMAGE_WIDTH to "Width",
                    android.media.ExifInterface.TAG_IMAGE_LENGTH to "Height",
                    android.media.ExifInterface.TAG_GPS_LATITUDE to "Latitude",
                    android.media.ExifInterface.TAG_GPS_LONGITUDE to "Longitude"
                )

                attributes.forEach { (tag, label) ->
                    val value = exif.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        metadata[label] = value
                    }
                }
                
                // Add resolution combination if width & height present
                if (metadata.containsKey("Width") && metadata.containsKey("Height")) {
                    metadata["Resolution"] = "${metadata["Width"]} x ${metadata["Height"]}"
                    metadata.remove("Width")
                    metadata.remove("Height")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            metadata["Error"] = "Failed to extract metadata"
        }
        metadata
    }
}

suspend fun calculateHistogram(context: Context, uri: Uri): ImageHistogram {
    return withContext(Dispatchers.IO) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(8) // Higher res for waveform
                    // ARGB_8888 for better accuracy
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val binsR = IntArray(256)
            val binsG = IntArray(256)
            val binsB = IntArray(256)
            val binsLuma = IntArray(256)
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            var maxCount = 0
            val skip = 2
            
            // Scopes data
            val waveCounts = IntArray(256 * 256)
            val vectorCounts = IntArray(256 * 256)
            var maxWave = 0
            var maxVector = 0

            for (y in 0 until height step skip) {
                for (x in 0 until width step skip) {
                    val pixel = pixels[y * width + x]
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    
                    val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                    
                    binsR[r]++
                    binsG[g]++
                    binsB[b]++
                    binsLuma[luma]++
                    
                    val currentMax = maxOf(binsR[r], binsG[g], binsB[b], binsLuma[luma])
                    if (currentMax > maxCount) maxCount = currentMax

                    // Waveform
                    val waveX = (x * 255) / width
                    val waveY = 255 - luma
                    val waveIdx = waveY * 256 + waveX
                    waveCounts[waveIdx]++
                    if (waveCounts[waveIdx] > maxWave) maxWave = waveCounts[waveIdx]

                    // Vectorscope
                    val cbRaw = -0.168736 * r - 0.331264 * g + 0.5 * b
                    val crRaw = 0.5 * r - 0.418688 * g - 0.081312 * b
                    val vectorScale = 2.5
                    val vecX = (cbRaw * vectorScale + 128).toInt().coerceIn(0, 255)
                    val vecY = (255 - (crRaw * vectorScale + 128)).toInt().coerceIn(0, 255)
                    val vecIdx = vecY * 256 + vecX
                    vectorCounts[vecIdx]++
                    if (vectorCounts[vecIdx] > maxVector) maxVector = vectorCounts[vecIdx]
                }
            }
            if (maxCount == 0) maxCount = 1
            if (maxWave == 0) maxWave = 1
            if (maxVector == 0) maxVector = 1

            val wavePixels = IntArray(256 * 256)
            val vectorPixels = IntArray(256 * 256)
            
            val logMaxWave = Math.log1p(maxWave.toDouble())
            val logMaxVec = Math.log1p(maxVector.toDouble())

            for (i in 0 until 65536) {
                val wVal = Math.log1p(waveCounts[i].toDouble()) / logMaxWave
                val wNorm = (wVal * 255 * 1.5).toFloat().coerceIn(0f, 255f)
                wavePixels[i] = android.graphics.Color.argb(wNorm.toInt(), 76, 175, 80) // Green tint

                val vVal = Math.log1p(vectorCounts[i].toDouble()) / logMaxVec
                val vNorm = (vVal * 255 * 1.5).toFloat().coerceIn(0f, 255f)
                val vecY = i / 256
                val vecX = i % 256
                val cb = (vecX - 128) / 2.5
                val cr = (127 - vecY) / 2.5
                val y = 128
                val rC = (y + 1.402 * cr).toInt().coerceIn(0, 255)
                val gC = (y - 0.344136 * cb - 0.714136 * cr).toInt().coerceIn(0, 255)
                val bC = (y + 1.772 * cb).toInt().coerceIn(0, 255)
                vectorPixels[i] = android.graphics.Color.argb(vNorm.toInt(), rC, gC, bC)
            }

            val waveformBitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
            waveformBitmap.setPixels(wavePixels, 0, 256, 0, 0, 256, 256)

            val vectorscopeBitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
            vectorscopeBitmap.setPixels(vectorPixels, 0, 256, 0, 0, 256, 256)

            ImageHistogram(
                red = binsR.map { it.toFloat() / maxCount },
                green = binsG.map { it.toFloat() / maxCount },
                blue = binsB.map { it.toFloat() / maxCount },
                luma = binsLuma.map { it.toFloat() / maxCount },
                waveform = waveformBitmap,
                vectorscope = vectorscopeBitmap
            )
        } catch (e: Exception) {
            ImageHistogram(
                List(256) { 0f }, List(256) { 0f }, List(256) { 0f }, List(256) { 0f }
            )
        }
    }
}

const val SOBEL_SHADER = """
    uniform shader image;
    half4 main(float2 fragCoord) {
        float2 dx = float2(1.0, 0.0);
        float2 dy = float2(0.0, 1.0);
        half3 c00 = image.eval(fragCoord - dx - dy).rgb;
        half3 c10 = image.eval(fragCoord - dy).rgb;
        half3 c20 = image.eval(fragCoord + dx - dy).rgb;
        half3 c01 = image.eval(fragCoord - dx).rgb;
        half3 c21 = image.eval(fragCoord + dx).rgb;
        half3 c02 = image.eval(fragCoord - dx + dy).rgb;
        half3 c12 = image.eval(fragCoord + dy).rgb;
        half3 c22 = image.eval(fragCoord + dx + dy).rgb;
        half3 sx = c00 + 2.0 * c01 + c02 - (c20 + 2.0 * c21 + c22);
        half3 sy = c00 + 2.0 * c10 + c20 - (c02 + 2.0 * c12 + c22);
        float dist = length(sx) + length(sy);
        return half4(dist, dist, dist, 1.0);
    }
"""

const val FOCUS_PEAKING_SHADER = """
    uniform shader image;
    half4 main(float2 fragCoord) {
        float2 dx = float2(1.0, 0.0);
        float2 dy = float2(0.0, 1.0);
        half4 color = image.eval(fragCoord);
        half3 c00 = image.eval(fragCoord - dx - dy).rgb;
        half3 c10 = image.eval(fragCoord - dy).rgb;
        half3 c20 = image.eval(fragCoord + dx - dy).rgb;
        half3 c01 = image.eval(fragCoord - dx).rgb;
        half3 c21 = image.eval(fragCoord + dx).rgb;
        half3 c02 = image.eval(fragCoord - dx + dy).rgb;
        half3 c12 = image.eval(fragCoord + dy).rgb;
        half3 c22 = image.eval(fragCoord + dx + dy).rgb;
        half3 sx = c00 + 2.0 * c01 + c02 - (c20 + 2.0 * c21 + c22);
        half3 sy = c00 + 2.0 * c10 + c20 - (c02 + 2.0 * c12 + c22);
        float dist = length(sx) + length(sy);
        if (dist > 1.0) {
            return half4(1.0, 0.0, 0.0, 1.0);
        }
        return color;
    }
"""

const val FALSE_COLOR_SHADER = """
    uniform shader image;
    half3 colormap(float t) {
        half3 c = half3(0.0);
        if (t < 0.25) { c = half3(0.0, 4.0 * t, 1.0); }
        else if (t < 0.5) { c = half3(0.0, 1.0, 2.0 - 4.0 * t); }
        else if (t < 0.75) { c = half3(4.0 * t - 2.0, 1.0, 0.0); }
        else { c = half3(1.0, 4.0 - 4.0 * t, 0.0); }
        return c;
    }
    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float luma = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
        return half4(colormap(luma), 1.0);
    }
"""

enum class ShaderMode { NORMAL, FOCUS_PEAKING, EDGES, FALSE_COLOR }

fun Modifier.liquidGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.2f,
    hazeState: HazeState? = null
) = composed {
    val state = hazeState ?: LocalHazeState.current
    this
    .then(
        if (state != null) 
            Modifier.hazeChild(state = state, shape = shape, style = HazeStyle(tint = Color(0xFF1E1E1E).copy(alpha = alpha), blurRadius = 16.dp))
        else 
            Modifier.background(Color(0xFF1E1E1E).copy(alpha = 0.7f), shape)
    )
    .clip(shape)
    .border(
        1.dp,
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.1f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        shape
    )
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.TIRAMISU)
fun createShaderRenderEffect(shaderCode: String): androidx.compose.ui.graphics.RenderEffect {
    return android.graphics.RenderEffect.createRuntimeShaderEffect(android.graphics.RuntimeShader(shaderCode), "image").asComposeRenderEffect()
}

@Composable
fun SyncZoomApp() {
    val hazeState = remember { HazeState() }
    androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides hazeState) {
        var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    var isSynced by remember { mutableStateOf(true) }
    var layoutMode by remember { mutableIntStateOf(0) } // 0=Auto, 1=Row, 2=Col
    var scopeMode by remember { mutableIntStateOf(0) } // 0=None, 1=Hist, 2=Wave, 3=Vector
    var rightBarExpanded by remember { mutableStateOf(false) }
    var bottomBarExpanded by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    var infiniteZoom by remember { mutableStateOf(true) }
    var subpixelGrid by remember { mutableStateOf(true) }
    var shaderMode by remember { mutableStateOf(ShaderMode.NORMAL) }
    
    var mirrorH by remember { mutableStateOf(false) }
    var mirrorV by remember { mutableStateOf(false) }
    var rotateImage by remember { mutableIntStateOf(0) } // 0..3 (0, 90, 180, 270)
    var advancedOverlay by remember { mutableIntStateOf(0) } // 0=None, 1=Crosshair, 2=Grid3x3, 3=GoldenRatio, 4=RuleOfThirds, 5=SafeMargins, 6=TitleSafe, 7=Diagonals
    var syncMode by remember { mutableIntStateOf(0) } // 0=Full, 1=PanOnly, 2=ZoomOnly, 3=None
    var lockAspectRatio by remember { mutableStateOf(true) }

    val independentZooms = remember { List(3) { ZoomState() } }
    val context = LocalContext.current
    var comparisonHistory by remember { mutableStateOf(loadHistory(context)) }
    
    LaunchedEffect(comparisonHistory) {
        saveHistory(context, comparisonHistory)
    }
    
    val histograms = remember { mutableStateMapOf<Uri, ImageHistogram>() }
    val scope = rememberCoroutineScope()
    var selectedMetadata by remember { mutableStateOf<Map<String, String>?>(null) }

    LaunchedEffect(imageUris) {
        histograms.clear()
        imageUris.forEach { uri ->
            histograms[uri] = calculateHistogram(context, uri)
        }
    }

    androidx.activity.compose.BackHandler(enabled = imageUris.isNotEmpty()) {
        if (imageUris.isNotEmpty()) {
            comparisonHistory = (listOf(imageUris) + comparisonHistory.filter { it != imageUris }).take(10)
        }
        imageUris = emptyList()
    }

    val getContent = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            imageUris = if (uris.size > 2) uris.take(2) else uris
            independentZooms.forEach { 
                it.scale = 1f
                it.offset = Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F0F12), // Deep dark background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { rightBarExpanded = false; bottomBarExpanded = false } }
        ) {
            if (imageUris.isNotEmpty()) {
                Box(modifier = Modifier.zIndex(1f).fillMaxWidth().padding(innerPadding).padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .liquidGlass(RoundedCornerShape(32.dp), 0.1f)
                            .clickable {
                                if (imageUris.isNotEmpty()) {
                                    comparisonHistory = (listOf(imageUris) + comparisonHistory.filter { it != imageUris }).take(10)
                                }
                                imageUris = emptyList()
                            }
                            .padding(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    val currentZoom = independentZooms[0]
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .liquidGlass(RoundedCornerShape(32.dp), 0.1f)
                            .clickable {
                                isSynced = !isSynced
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = if (isSynced) Color.White else Color.White.copy(alpha=0.4f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSynced) "SYNCED" else "UNSYNCED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSynced) Color.White else Color.White.copy(alpha = 0.4f))
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.2f)))
                        Spacer(Modifier.width(16.dp))
                        Text(text = "${String.format(java.util.Locale.US, "%.1f", currentZoom.scale * 100)}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        Box(
                            modifier = Modifier
                                .liquidGlass(RoundedCornerShape(32.dp), 0.1f)
                                .clickable { expandedMenu = true }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(Color(0xFF222228))
                        ) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Select Images", color = Color.White) }, onClick = { expandedMenu = false; getContent.launch("image/*") })
                            if (imageUris.size == 2) androidx.compose.material3.DropdownMenuItem(text = { Text("Swap Positions", color = Color.White) }, onClick = { expandedMenu = false; imageUris = imageUris.reversed() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Clear All", color = Color.Red) }, onClick = { expandedMenu = false; imageUris = emptyList() })
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().then(if (hazeState != null) Modifier.haze(hazeState) else Modifier)) {
                if (imageUris.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp).fillMaxWidth().widthIn(max=500.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipToFront, 
                                contentDescription = null, 
                                tint = Color.White.copy(0.2f), 
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E24), RoundedCornerShape(24.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { getContent.launch("image/*") }
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add Photos",
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF64B5F6)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Select Images to Compare",
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "High-performance synchronization tool",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = comparisonHistory.isNotEmpty(),
                                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy)) + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Recent Comparisons", color = Color.White.copy(0.7f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("CLEAR ALL", color = Color(0xFFFF5252).copy(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { comparisonHistory = emptyList() }.padding(4.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        items(comparisonHistory.size) { index ->
                                            val historyUris = comparisonHistory[index]
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF1E1E24), RoundedCornerShape(16.dp))
                                                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { 
                                                        imageUris = historyUris
                                                        independentZooms.forEach { z -> z.scale = 1f; z.offset = Offset.Zero }
                                                    }
                                                    .padding(6.dp)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    historyUris.forEach { uri ->
                                                        coil.compose.AsyncImage(
                                                            model = uri,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val isLandscape = when (layoutMode) {
                        1 -> true
                        2 -> false
                        else -> configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    }
                    
                    // Gap between images
                    val gap = 2.dp
                    val imageModifier = Modifier
                        .background(Color.Black, RoundedCornerShape(0.dp))
                        .clip(RoundedCornerShape(0.dp))
    
                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            imageUris.forEachIndexed { index, uri ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(0.15f))
                                    )
                                }
                                SyncZoomImage(
                                    uri = uri,
                                    zoomState = independentZooms[index],
                                    index = index,
                                    scopeMode = scopeMode,
                                    shaderMode = shaderMode,
                                    histogramData = histograms[uri],
                                    infiniteZoom = infiniteZoom,
                                    subpixelGrid = subpixelGrid,
                                    mirrorH = mirrorH,
                                    mirrorV = mirrorV,
                                    rotateImage = rotateImage,
                                    advancedOverlay = advancedOverlay,
                                    onInteract = { rightBarExpanded = false; bottomBarExpanded = false },
                                    onTransform = { offsetDelta, zoomDelta ->
                                        val applyTo = if (isSynced) independentZooms.take(imageUris.size) else listOf(independentZooms[index])
                                        val maxS = if (infiniteZoom) 100000000f else 100f
                                        applyTo.forEach { zState ->
                                            zState.offset += offsetDelta
                                            zState.scale = (zState.scale * zoomDelta).coerceIn(0.001f, maxS)
                                        }
                                    },
                                    onDoubleTap = {
                                        val applyTo = if (isSynced) independentZooms.take(imageUris.size) else listOf(independentZooms[index])
                                        applyTo.forEach { zState ->
                                            zState.scale = 1f
                                            zState.offset = Offset.Zero
                                        }
                                    },
                                    onLongPress = {
                                        scope.launch {
                                            selectedMetadata = extractExifMetadata(context, uri)
                                        }
                                    },
                                    modifier = imageModifier.weight(1f).fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            imageUris.forEachIndexed { index, uri ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .fillMaxWidth()
                                            .background(Color.White.copy(0.15f))
                                    )
                                }
                                SyncZoomImage(
                                    uri = uri,
                                    zoomState = independentZooms[index],
                                    index = index,
                                    scopeMode = scopeMode,
                                    shaderMode = shaderMode,
                                    histogramData = histograms[uri],
                                    infiniteZoom = infiniteZoom,
                                    subpixelGrid = subpixelGrid,
                                    mirrorH = mirrorH,
                                    mirrorV = mirrorV,
                                    rotateImage = rotateImage,
                                    advancedOverlay = advancedOverlay,
                                    onInteract = { rightBarExpanded = false; bottomBarExpanded = false },
                                    onTransform = { offsetDelta, zoomDelta ->
                                        val applyTo = if (isSynced) independentZooms.take(imageUris.size) else listOf(independentZooms[index])
                                        val maxS = if (infiniteZoom) 100000000f else 100f
                                        applyTo.forEach { zState ->
                                            zState.offset += offsetDelta
                                            zState.scale = (zState.scale * zoomDelta).coerceIn(0.001f, maxS)
                                        }
                                    },
                                    onDoubleTap = {
                                        val applyTo = if (isSynced) independentZooms.take(imageUris.size) else listOf(independentZooms[index])
                                        applyTo.forEach { zState ->
                                            zState.scale = 1f
                                            zState.offset = Offset.Zero
                                        }
                                    },
                                    onLongPress = {
                                        scope.launch {
                                            selectedMetadata = extractExifMetadata(context, uri)
                                        }
                                    },
                                    modifier = imageModifier.weight(1f).fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } // Close Image Box

            // Right Toolbar
            if (imageUris.isNotEmpty()) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(innerPadding).padding(end = 16.dp)) {
                    val cornerRadius by androidx.compose.animation.core.animateDpAsState(if (rightBarExpanded) 32.dp else 48.dp)
                    val bgPaddingV by androidx.compose.animation.core.animateDpAsState(if (rightBarExpanded) 16.dp else 0.dp)
                    
                    Column(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(cornerRadius), spotColor = Color.Black.copy(alpha = 0.6f))
                            .liquidGlass(RoundedCornerShape(cornerRadius), 0.15f)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                            .padding(vertical = bgPaddingV, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier
                            .clip(CircleShape)
                            .clickable { 
                                rightBarExpanded = !rightBarExpanded
                                if (rightBarExpanded) bottomBarExpanded = false
                            }.padding(12.dp)
                        ) {
                            Icon(if (rightBarExpanded) Icons.Default.Palette else Icons.Default.Palette, "Filters & Tools", tint = Color.White)
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = rightBarExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 4.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    // Filters
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("FILTERS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { shaderMode = if (shaderMode == ShaderMode.FOCUS_PEAKING) ShaderMode.NORMAL else ShaderMode.FOCUS_PEAKING }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.CenterFocusWeak, "Peaking", tint = if (shaderMode == ShaderMode.FOCUS_PEAKING) Color(0xFFFF5252) else Color.White.copy(0.4f))
                                            Text("Peaking", color = if (shaderMode == ShaderMode.FOCUS_PEAKING) Color(0xFFFF5252) else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (shaderMode == ShaderMode.FOCUS_PEAKING) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { shaderMode = if (shaderMode == ShaderMode.EDGES) ShaderMode.NORMAL else ShaderMode.EDGES }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Adjust, "Edges", tint = if (shaderMode == ShaderMode.EDGES) Color.White else Color.White.copy(0.4f))
                                            Text("Edges", color = if (shaderMode == ShaderMode.EDGES) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (shaderMode == ShaderMode.EDGES) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { shaderMode = if (shaderMode == ShaderMode.FALSE_COLOR) ShaderMode.NORMAL else ShaderMode.FALSE_COLOR }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.InvertColors, "False Color", tint = if (shaderMode == ShaderMode.FALSE_COLOR) Color.White else Color.White.copy(0.4f))
                                            Text("False", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (shaderMode == ShaderMode.FALSE_COLOR) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (shaderMode == ShaderMode.FALSE_COLOR) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TOOLS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { infiniteZoom = !infiniteZoom }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.AllInclusive, "Inf Zoom", tint = if (infiniteZoom) Color.White else Color.White.copy(0.4f))
                                            Text("Inf Zoom", color = if (infiniteZoom) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (infiniteZoom) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { subpixelGrid = !subpixelGrid }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.GridOn, "FilterMode", tint = if (subpixelGrid) Color.White else Color.White.copy(0.4f))
                                            Text("FilterMode", color = if (subpixelGrid) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (subpixelGrid) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { mirrorH = !mirrorH }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.SwapHoriz, "Flip H", tint = if (mirrorH) Color.White else Color.White.copy(0.4f))
                                            Text("Flip H", color = if (mirrorH) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (mirrorH) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { mirrorV = !mirrorV }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.SwapVert, "Flip V", tint = if (mirrorV) Color.White else Color.White.copy(0.4f))
                                            Text("Flip V", color = if (mirrorV) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (mirrorV) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { rotateImage = (rotateImage + 1) % 4 }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.RotateRight, "Rotate", tint = if (rotateImage != 0) Color.White else Color.White.copy(0.4f))
                                            Text("Rotate", color = if (rotateImage != 0) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (rotateImage != 0) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { advancedOverlay = (advancedOverlay + 1) % 8 }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Dashboard, "Overlay", tint = if (advancedOverlay != 0) Color.White else Color.White.copy(0.4f))
                                            Text("Overlay: $advancedOverlay", color = if (advancedOverlay != 0) Color.White else Color.White.copy(0.4f), fontSize = 9.sp, fontWeight = if (advancedOverlay != 0) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        if (imageUris.size == 2) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                                val tS = independentZooms[0].scale
                                                val tO = independentZooms[0].offset
                                                independentZooms[0].scale = independentZooms[1].scale
                                                independentZooms[0].offset = independentZooms[1].offset
                                                independentZooms[1].scale = tS
                                                independentZooms[1].offset = tO
                                                imageUris = imageUris.reversed() 
                                            }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Gavel, "Swap Img", tint = Color.White)
                                                Text("Swap Img", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Menu
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(innerPadding).padding(bottom = 32.dp)) {
                    val cornerRadius by androidx.compose.animation.core.animateDpAsState(if (bottomBarExpanded) 24.dp else 32.dp)
                    val bgPaddingH by androidx.compose.animation.core.animateDpAsState(if (bottomBarExpanded) 16.dp else 0.dp)
                    val bgPaddingV by androidx.compose.animation.core.animateDpAsState(if (bottomBarExpanded) 8.dp else 0.dp)
                    
                    Column(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(cornerRadius), spotColor = Color.Black.copy(alpha = 0.6f))
                            .liquidGlass(RoundedCornerShape(cornerRadius), 0.15f)
                            .padding(horizontal = bgPaddingH, vertical = bgPaddingV)
                            .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = bottomBarExpanded,
                            enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                BottomNavItem(Icons.Default.Equalizer, "Hist", scopeMode == 1) { scopeMode = if (scopeMode == 1) 0 else 1 }
                                BottomNavItem(Icons.Default.Menu, "Wave", scopeMode == 2) { scopeMode = if (scopeMode == 2) 0 else 2 }
                                BottomNavItem(Icons.Default.Adjust, "Vector", scopeMode == 3) { scopeMode = if (scopeMode == 3) 0 else 3 }
                            }
                        }
                        
                        Box(modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                bottomBarExpanded = !bottomBarExpanded 
                                if (bottomBarExpanded) rightBarExpanded = false
                            }.padding(12.dp)
                        ) {
                            Icon(if (bottomBarExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, "Expand", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        
        selectedMetadata?.let { metadata ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { selectedMetadata = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(24.dp), 0.25f)
                        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.3f), Color.White.copy(0.05f))), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text("Image Metadata", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                            Box(modifier = Modifier.clip(CircleShape).clickable { selectedMetadata = null }.background(Color.White.copy(0.1f)).padding(8.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 45f })
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.White.copy(0.2f), Color.Transparent))))
                        Spacer(Modifier.height(20.dp))
                        
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            val itemsList = metadata.entries.toList()
                            items(itemsList.size) { index ->
                                val entry = itemsList[index]
                                val key = entry.key
                                val value = entry.value
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(key, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // Close CompositionLocalProvider
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SyncZoomImage(
    uri: Uri, 
    zoomState: ZoomState, 
    index: Int,
    scopeMode: Int,
    shaderMode: ShaderMode,
    histogramData: ImageHistogram?,
    infiniteZoom: Boolean,
    subpixelGrid: Boolean,
    mirrorH: Boolean = false,
    mirrorV: Boolean = false,
    rotateImage: Int = 0,
    advancedOverlay: Int = 0,
    blendMode: androidx.compose.ui.graphics.BlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
    onInteract: () -> Unit = {},
    onTransform: (Offset, Float) -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(infiniteZoom) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    onInteract()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val offsetFromCenter = centroid - center
                    val zoomOffset = offsetFromCenter * (zoom - 1f)
                    val offsetDelta = pan - zoomOffset
                    
                    onTransform(offsetDelta, zoom)
                }
            }
            .pointerInput(Unit) {
                 detectTapGestures(
                     onDoubleTap = {
                         onDoubleTap()
                     },
                     onLongPress = {
                         onLongPress()
                     }
                 )
            }
    ) {
        val renderEffectToApply = remember(shaderMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when (shaderMode) {
                    ShaderMode.FOCUS_PEAKING -> createShaderRenderEffect(FOCUS_PEAKING_SHADER)
                    ShaderMode.EDGES -> createShaderRenderEffect(SOBEL_SHADER)
                    ShaderMode.FALSE_COLOR -> createShaderRenderEffect(FALSE_COLOR_SHADER)
                    else -> null
                }
            } else {
                null
            }
        }
        
        androidx.compose.runtime.key(uri, subpixelGrid) {
            AsyncImage(
                model = uri,
                contentDescription = "Image ${index + 1}",
                contentScale = ContentScale.Fit, 
                filterQuality = if (subpixelGrid) androidx.compose.ui.graphics.FilterQuality.None else androidx.compose.ui.graphics.FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomState.scale * if (mirrorH) -1f else 1f
                        scaleY = zoomState.scale * if (mirrorV) -1f else 1f
                        translationX = zoomState.offset.x
                        translationY = zoomState.offset.y
                        rotationZ = rotateImage * 90f
                        renderEffect = renderEffectToApply
                    }
                    .drawWithContent {
                        if (blendMode != androidx.compose.ui.graphics.BlendMode.SrcOver) {
                            val p = androidx.compose.ui.graphics.Paint().apply { this.blendMode = blendMode }
                            drawContext.canvas.saveLayer(size.toRect(), p)
                            drawContent()
                            drawContext.canvas.restore()
                        } else {
                            drawContent()
                        }
                        val paintColor = Color.White.copy(0.4f)
                        val stroke = 2.dp.toPx()
                        when(advancedOverlay) {
                            1 -> { // Crosshair
                                drawLine(paintColor, Offset(size.width/2, 0f), Offset(size.width/2, size.height), stroke)
                                drawLine(paintColor, Offset(0f, size.height/2), Offset(size.width, size.height/2), stroke)
                            }
                            2 -> { // 3x3
                                drawLine(paintColor, Offset(size.width/3, 0f), Offset(size.width/3, size.height), stroke)
                                drawLine(paintColor, Offset(size.width*2/3, 0f), Offset(size.width*2/3, size.height), stroke)
                                drawLine(paintColor, Offset(0f, size.height/3), Offset(size.width, size.height/3), stroke)
                                drawLine(paintColor, Offset(0f, size.height*2/3), Offset(size.width, size.height*2/3), stroke)
                            }
                            3 -> { // Golden Ratio (Fibonacci approximation grids)
                                drawLine(paintColor, Offset(size.width*0.382f, 0f), Offset(size.width*0.382f, size.height), stroke)
                                drawLine(paintColor, Offset(size.width*0.618f, 0f), Offset(size.width*0.618f, size.height), stroke)
                                drawLine(paintColor, Offset(0f, size.height*0.382f), Offset(size.width, size.height*0.382f), stroke)
                                drawLine(paintColor, Offset(0f, size.height*0.618f), Offset(size.width, size.height*0.618f), stroke)
                            }
                            4 -> { // Rule Of Thirds
                                drawLine(paintColor, Offset(size.width/3, 0f), Offset(size.width/3, size.height), stroke)
                                drawLine(paintColor, Offset(size.width*2/3, 0f), Offset(size.width*2/3, size.height), stroke)
                                drawLine(paintColor, Offset(0f, size.height/3), Offset(size.width, size.height/3), stroke)
                                drawLine(paintColor, Offset(0f, size.height*2/3), Offset(size.width, size.height*2/3), stroke)
                            }
                            5 -> { // Safe Margins (90%)
                                drawRect(paintColor, Offset(size.width*0.05f, size.height*0.05f), size * 0.9f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                            }
                            6 -> { // Title Safe (80%)
                                drawRect(paintColor, Offset(size.width*0.1f, size.height*0.1f), size * 0.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                            }
                            7 -> { // Diagonals
                                drawLine(paintColor, Offset(0f, 0f), Offset(size.width, size.height), stroke)
                                drawLine(paintColor, Offset(0f, size.height), Offset(size.width, 0f), stroke)
                            }
                        }
                    }
            )
        }

        if (scopeMode > 0 && histogramData != null) {
            val align = when (index) {
                0 -> Alignment.BottomStart
                1 -> Alignment.TopStart
                else -> Alignment.BottomStart
            }
            
            val boxWidth = if (scopeMode == 1) 160.dp else 180.dp
            val boxHeight = if (scopeMode == 1) 100.dp else 180.dp
            
            Box(
                modifier = Modifier
                    .align(align)
                    .padding(16.dp)
                    .size(width = boxWidth, height = boxHeight)
                    .background(Color(0xFF2A2A30).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                if (scopeMode == 1) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / 256f

                        fun drawChannelPath(channel: List<Float>, color: Color) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height)
                                channel.forEachIndexed { i, h ->
                                    val x = i * barWidth
                                    val y = size.height - (Math.pow(h.toDouble(), 0.6).toFloat() * size.height).coerceIn(0f, size.height)
                                    lineTo(x, y)
                                    lineTo(x + barWidth, y)
                                }
                                lineTo(size.width, size.height)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = color,
                                blendMode = androidx.compose.ui.graphics.BlendMode.Screen
                            )
                        }

                        drawChannelPath(histogramData.red, Color(0xFFFF5252).copy(alpha = 0.9f))
                        drawChannelPath(histogramData.green, Color(0xFF69F0AE).copy(alpha = 0.9f))
                        drawChannelPath(histogramData.blue, Color(0xFF448AFF).copy(alpha = 0.9f))
                        drawChannelPath(histogramData.luma, Color.White.copy(alpha = 0.6f))
                        
                        val shadowClip = histogramData.luma.firstOrNull() ?: 0f
                        val highlightClip = histogramData.luma.lastOrNull() ?: 0f
                        
                        if (shadowClip > 0.05f) {
                            drawRect(
                                color = Color.Cyan.copy(alpha = 0.5f),
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                            )
                        }
                        if (highlightClip > 0.05f) {
                            drawRect(
                                color = Color.Red.copy(alpha = 0.5f),
                                topLeft = Offset(size.width - 3.dp.toPx(), 0f),
                                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                            )
                        }
                    }
                } else if (scopeMode == 2 && histogramData.waveform != null) {
                    androidx.compose.foundation.Image(
                        bitmap = histogramData.waveform.asImageBitmap(),
                        contentDescription = "Waveform",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else if (scopeMode == 3 && histogramData.vectorscope != null) {
                    androidx.compose.foundation.Image(
                        bitmap = histogramData.vectorscope.asImageBitmap(),
                        contentDescription = "Vectorscope",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    }
}
