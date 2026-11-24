package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.ClinicalImageRef
import com.example.data.model.ClinicalSpotterCatalog
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.editorialColors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Compact badge displayed on Practice / CBT question headers for Image-Based Questions (IBQ).
 */
@Composable
fun ImageBasedQuestionBadge(modifier: Modifier = Modifier) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = editorial.acidLimeMuted,
        border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.65f)),
        modifier = modifier.testTag("ibq_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = "Image-Based Clinical Question",
                tint = if (editorial.isDark) editorial.acidLime else PureBlack,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "IBQ",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = if (editorial.isDark) editorial.acidLime else PureBlack
            )
        }
    }
}

/**
 * High-contrast clinical visual card for Image-Based MCQs (ECG strips, surgical instruments,
 * IV cannulas, BMW segregation bins, oxygen devices, and remote Admin OTA clinical photos).
 * Supports tap-to-open full-screen pinch-and-pan lightbox.
 */
@Composable
fun ClinicalQuestionImageCard(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val ref = remember(imageUrl) { ClinicalImageRef.parse(imageUrl) }
    if (ref is ClinicalImageRef.None) return

    val editorial = MaterialTheme.editorialColors
    val badgeLabel = remember(ref) { ClinicalSpotterCatalog.badgeLabelFor(ref) }
    var showLightbox by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showLightbox = true }
            .testTag("clinical_question_image_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top clinical metadata strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(editorial.elevatedSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = editorial.acidLime,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = editorial.cardSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.testTag("question_image_zoom_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "Zoom clinical image",
                            tint = if (editorial.isDark) AcidLime else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Tap to Zoom",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Visual viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color(0xFF0E1116)),
                contentAlignment = Alignment.Center
            ) {
                ClinicalVisualContent(
                    ref = ref,
                    isLightbox = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showLightbox) {
        ClinicalImageZoomLightbox(
            ref = ref,
            badgeLabel = badgeLabel,
            onDismiss = { showLightbox = false }
        )
    }
}

@Composable
private fun ClinicalImageZoomLightbox(
    ref: ClinicalImageRef,
    badgeLabel: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF07080A),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top dark-room toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                        Text(
                            text = "Pinch to zoom · Drag to pan · Double-tap to reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite.copy(alpha = 0.72f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1A1D24)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fx", scale),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        if (scale > 1.05f || offset != Offset.Zero) {
                            TextButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reset zoom",
                                    tint = AcidLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", color = AcidLime)
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("clinical_lightbox_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close zoomed clinical image",
                                tint = PureWhite
                            )
                        }
                    }
                }

                // Pinch-and-pan stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.15f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.2f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 3.5f)
                                scale = nextScale
                                if (nextScale <= 1.02f) {
                                    offset = Offset.Zero
                                } else {
                                    val maxX = (size.width * (nextScale - 1f)) / 2f
                                    val maxY = (size.height * (nextScale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .aspectRatio(1.45f)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ClinicalVisualContent(
                            ref = ref,
                            isLightbox = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalVisualContent(
    ref: ClinicalImageRef,
    isLightbox: Boolean,
    modifier: Modifier = Modifier
) {
    when (ref) {
        is ClinicalImageRef.Diagram -> ClinicalDiagramRenderer(
            diagramKey = ref.key,
            modifier = modifier
        )
        is ClinicalImageRef.Drawable -> {
            val resId = resolveBundledDrawable(ref.key)
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Clinical nursing spotter photograph",
                    contentScale = if (isLightbox) ContentScale.Fit else ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                ClinicalVisualFallback(
                    message = "Spotter image unavailable (${ref.key})",
                    modifier = modifier
                )
            }
        }
        is ClinicalImageRef.RemoteUrl -> {
            SubcomposeAsyncImage(
                model = ref.url,
                contentDescription = "Clinical question visual",
                contentScale = if (isLightbox) ContentScale.Fit else ContentScale.Crop,
                modifier = modifier,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AcidLime,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                error = {
                    ClinicalVisualFallback(
                        message = "Connect to internet to load this OTA clinical image.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
        ClinicalImageRef.None -> Unit
    }
}

@DrawableRes
private fun resolveBundledDrawable(key: String): Int? = when (key) {
    ClinicalSpotterCatalog.DRAWABLE_LARYNGOSCOPE -> R.drawable.img_spotter_laryngoscope_1795519121492
    ClinicalSpotterCatalog.DRAWABLE_SURGICAL_TRAY -> R.drawable.img_spotter_surgical_tray_1795519119734
    else -> null
}

@Composable
private fun ClinicalVisualFallback(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF141820))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.BrokenImage,
            contentDescription = null,
            tint = PureWhite.copy(alpha = 0.55f),
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// VECTOR-SHARP CLINICAL DIAGRAM RENDERERS (ECG STRIPS, IV CANNULA, BMW, VENTURI)
// ============================================================================

@Composable
private fun ClinicalDiagramRenderer(
    diagramKey: String,
    modifier: Modifier = Modifier
) {
    when (diagramKey) {
        ClinicalSpotterCatalog.DIAGRAM_ECG_VTACH,
        ClinicalSpotterCatalog.DIAGRAM_ECG_VFIB,
        ClinicalSpotterCatalog.DIAGRAM_ECG_AFIB,
        ClinicalSpotterCatalog.DIAGRAM_ECG_STEMI,
        ClinicalSpotterCatalog.DIAGRAM_ECG_AVBLOCK3,
        ClinicalSpotterCatalog.DIAGRAM_ECG_HYPERKALEMIA -> {
            EcgStripCanvas(diagramKey = diagramKey, modifier = modifier)
        }
        ClinicalSpotterCatalog.DIAGRAM_IV_CANNULA_18G -> {
            IvCannulaSpotterCard(
                hubColor = Color(0xFF1B8E4B),
                hubBorderColor = Color(0xFF66DF93),
                odLabel = "1.3 mm OD",
                flowLabel = "~90 mL/min",
                colorName = "ISO GREEN HUB",
                modifier = modifier
            )
        }
        ClinicalSpotterCatalog.DIAGRAM_IV_CANNULA_22G -> {
            IvCannulaSpotterCard(
                hubColor = Color(0xFF1565C0),
                hubBorderColor = Color(0xFF64B5F6),
                odLabel = "0.9 mm OD",
                flowLabel = "~36 mL/min",
                colorName = "ISO BLUE HUB",
                modifier = modifier
            )
        }
        ClinicalSpotterCatalog.DIAGRAM_BMW_RED -> {
            BmwBinsSpotterCard(highlightBin = "RED", modifier = modifier)
        }
        ClinicalSpotterCatalog.DIAGRAM_BMW_YELLOW -> {
            BmwBinsSpotterCard(highlightBin = "YELLOW", modifier = modifier)
        }
        ClinicalSpotterCatalog.DIAGRAM_VENTURI_MASK -> {
            VenturiMaskSpotterCard(modifier = modifier)
        }
        else -> ClinicalVisualFallback(
            message = "Unsupported clinical diagram: $diagramKey",
            modifier = modifier
        )
    }
}

@Composable
private fun EcgStripCanvas(
    diagramKey: String,
    modifier: Modifier = Modifier
) {
    val leadTag = when (diagramKey) {
        ClinicalSpotterCatalog.DIAGRAM_ECG_STEMI -> "LEAD V2–V4 · 25 mm/s · 10 mm/mV"
        else -> "LEAD II · 25 mm/s · 10 mm/mV"
    }
    Box(
        modifier = modifier
            .background(Color(0xFFFFF5F5))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEcgGridPaper()
            drawEcgCalibrationPulse()
            drawEcgWaveform(diagramKey)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xDD1A1D24),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = leadTag,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = PureWhite,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun DrawScope.drawEcgGridPaper() {
    val minorStep = (size.height / 26f).coerceAtLeast(6f)
    val majorStep = minorStep * 5f
    val minorColor = Color(0xFFF6C2C9)
    val majorColor = Color(0xFFE3818E)

    var x = 0f
    while (x <= size.width) {
        drawLine(minorColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += minorStep
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(minorColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += minorStep
    }

    x = 0f
    while (x <= size.width) {
        drawLine(majorColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.8f)
        x += majorStep
    }
    y = 0f
    while (y <= size.height) {
        drawLine(majorColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.8f)
        y += majorStep
    }
}

private fun DrawScope.drawEcgCalibrationPulse() {
    val baseY = size.height * 0.56f
    val unitH = size.height * 0.24f
    val startX = size.width * 0.025f
    val pulseW = size.width * 0.032f
    val path = Path().apply {
        moveTo(startX, baseY)
        lineTo(startX + pulseW * 0.3f, baseY)
        lineTo(startX + pulseW * 0.3f, baseY - unitH)
        lineTo(startX + pulseW * 1.15f, baseY - unitH)
        lineTo(startX + pulseW * 1.15f, baseY)
        lineTo(startX + pulseW * 1.5f, baseY)
    }
    drawPath(
        path = path,
        color = Color(0xFF151822),
        style = Stroke(width = 2.6f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
}

private fun gaussian(x: Float, mean: Float, sigma: Float): Float {
    val z = (x - mean) / sigma
    return exp(-0.5f * z * z)
}

private fun DrawScope.drawEcgWaveform(diagramKey: String) {
    val startX = size.width * 0.085f
    val endX = size.width * 0.975f
    val traceWidth = (endX - startX).coerceAtLeast(1f)
    val baseY = size.height * 0.56f
    val amp = size.height * 0.30f
    val samples = 420
    val path = Path()

    for (i in 0..samples) {
        val u = i.toFloat() / samples.toFloat() // 0..1 across strip
        val x = startX + u * traceWidth
        val mv: Float = when (diagramKey) {
            ClinicalSpotterCatalog.DIAGRAM_ECG_VTACH -> {
                // Regular wide monomorphic VTach (~8 broad QRS cycles across strip)
                val phase = (u * 8.2f) % 1f
                val broadPeak = 0.92f * gaussian(phase, 0.30f, 0.12f)
                val broadTrough = -0.68f * gaussian(phase, 0.62f, 0.13f)
                broadPeak + broadTrough
            }
            ClinicalSpotterCatalog.DIAGRAM_ECG_VFIB -> {
                // Coarse chaotic undulating ventricular fibrillation
                val w1 = 0.45f * sin(u * 2f * PI.toFloat() * 11.3f)
                val w2 = 0.34f * sin(u * 2f * PI.toFloat() * 17.7f + 1.2f)
                val w3 = 0.24f * sin(u * 2f * PI.toFloat() * 6.4f + 2.5f)
                val env = 0.65f + 0.35f * sin(u * 2f * PI.toFloat() * 2.1f)
                (w1 + w2 + w3) * env
            }
            ClinicalSpotterCatalog.DIAGRAM_ECG_AFIB -> {
                // Irregularly irregular narrow QRS complexes + fine fibrillatory baseline, NO P waves
                val beatCenters = listOf(0.08f, 0.21f, 0.41f, 0.52f, 0.76f, 0.91f)
                var v = 0.06f * sin(u * 2f * PI.toFloat() * 34f) +
                    0.04f * sin(u * 2f * PI.toFloat() * 51f + 0.9f)
                for (c in beatCenters) {
                    v -= 0.14f * gaussian(u, c - 0.011f, 0.005f)
                    v += 0.95f * gaussian(u, c, 0.0065f)
                    v -= 0.22f * gaussian(u, c + 0.012f, 0.006f)
                    v += 0.22f * gaussian(u, c + 0.052f, 0.018f)
                }
                v
            }
            ClinicalSpotterCatalog.DIAGRAM_ECG_STEMI -> {
                // Acute Anterior STEMI: P + narrow QRS + dramatic convex tombstone ST-elevation
                val phase = (u * 3.6f) % 1f
                val pWave = 0.14f * gaussian(phase, 0.15f, 0.032f)
                val qWave = -0.15f * gaussian(phase, 0.26f, 0.012f)
                val rWave = 0.92f * gaussian(phase, 0.295f, 0.016f)
                val stTombstone = 0.54f * gaussian(phase, 0.42f, 0.095f)
                val tMerge = 0.34f * gaussian(phase, 0.54f, 0.07f)
                pWave + qWave + rWave + stTombstone + tMerge
            }
            ClinicalSpotterCatalog.DIAGRAM_ECG_AVBLOCK3 -> {
                // Complete 3rd-Degree AV Block: regular P waves (~7.5 cycles) completely dissociated from slow escape QRS (~2.4 cycles)
                val pCenters = listOf(0.05f, 0.18f, 0.31f, 0.44f, 0.57f, 0.70f, 0.83f, 0.96f)
                val qrsCenters = listOf(0.14f, 0.53f, 0.92f)
                var v = 0f
                for (p in pCenters) {
                    v += 0.22f * gaussian(u, p, 0.013f)
                }
                for (c in qrsCenters) {
                    v -= 0.18f * gaussian(u, c - 0.018f, 0.009f)
                    v += 0.94f * gaussian(u, c, 0.014f)
                    v -= 0.34f * gaussian(u, c + 0.022f, 0.012f)
                    v += 0.26f * gaussian(u, c + 0.085f, 0.026f)
                }
                v
            }
            ClinicalSpotterCatalog.DIAGRAM_ECG_HYPERKALEMIA -> {
                // Severe Hyperkalemia: flattened P, slightly widened QRS, TALL NARROW TENTED PEAKED T-WAVES!
                val phase = (u * 3.8f) % 1f
                val flatP = 0.05f * gaussian(phase, 0.14f, 0.045f)
                val qWave = -0.14f * gaussian(phase, 0.25f, 0.014f)
                val rWave = 0.78f * gaussian(phase, 0.29f, 0.020f)
                val sWave = -0.26f * gaussian(phase, 0.34f, 0.018f)
                // Signature tall, narrow symmetrical tented T wave
                val distT = kotlin.math.abs(phase - 0.54f)
                val tentedT = if (distT < 0.12f) {
                    1.08f * (1f - (distT / 0.12f)).pow(1.45f)
                } else {
                    0f
                }
                flatP + qWave + rWave + sWave + tentedT
            }
            else -> 0f
        }

        val y = (baseY - mv * amp).coerceIn(size.height * 0.08f, size.height * 0.92f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = Color(0xFF151822),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
private fun IvCannulaSpotterCard(
    hubColor: Color,
    hubBorderColor: Color,
    odLabel: String,
    flowLabel: String,
    colorName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0E141B))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = hubColor.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, hubBorderColor)
            ) {
                Text(
                    text = colorName,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = hubBorderColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "$odLabel · $flowLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = PureWhite.copy(alpha = 0.85f)
            )
        }

        // Vector technical peripheral IV cannula drawing
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            val midY = size.height * 0.56f
            val left = size.width * 0.06f
            val right = size.width * 0.94f
            val w = right - left

            // Translucent catheter shaft + metal needle bevel (left half)
            val shaftStart = left + w * 0.05f
            val shaftEnd = left + w * 0.48f
            // Needle bevel tip
            val bevelPath = Path().apply {
                moveTo(left, midY)
                lineTo(shaftStart + 10f, midY - 4f)
                lineTo(shaftStart + 10f, midY + 4f)
                close()
            }
            drawPath(bevelPath, color = Color(0xFFDCE4EC))
            // Translucent PTFE cannula sleeve
            drawRoundRect(
                color = Color(0x99A5D6E8),
                topLeft = Offset(shaftStart, midY - 7f),
                size = Size(shaftEnd - shaftStart, 14f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(
                color = Color(0xFFE6EEF5),
                start = Offset(shaftStart, midY),
                end = Offset(shaftEnd, midY),
                strokeWidth = 3f
            )

            // Color-coded butterfly wings (above & below hub)
            val hubStart = shaftEnd
            val hubW = w * 0.24f
            val wingH = size.height * 0.28f
            // Upper wing
            drawRoundRect(
                color = hubColor.copy(alpha = 0.85f),
                topLeft = Offset(hubStart + hubW * 0.14f, midY - 14f - wingH),
                size = Size(hubW * 0.72f, wingH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Lower wing
            drawRoundRect(
                color = hubColor.copy(alpha = 0.85f),
                topLeft = Offset(hubStart + hubW * 0.14f, midY + 14f),
                size = Size(hubW * 0.72f, wingH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Main central color-coded hub body
            drawRoundRect(
                color = hubColor,
                topLeft = Offset(hubStart, midY - 18f),
                size = Size(hubW, 36f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = hubBorderColor,
                topLeft = Offset(hubStart, midY - 18f),
                size = Size(hubW, 36f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.2f)
            )
            // Top injection port cap
            drawRoundRect(
                color = hubBorderColor,
                topLeft = Offset(hubStart + hubW * 0.30f, midY - 36f),
                size = Size(hubW * 0.40f, 18f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Flashback chamber + Luer lock stopper (right side)
            val flashStart = hubStart + hubW
            val flashW = w * 0.21f
            drawRoundRect(
                color = Color(0x55FFFFFF),
                topLeft = Offset(flashStart, midY - 13f),
                size = Size(flashW, 26f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0x99CFD8DC),
                topLeft = Offset(flashStart, midY - 13f),
                size = Size(flashW, 26f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.8f)
            )
            // White hydrophobic stopper plug
            drawRoundRect(
                color = Color(0xFFECEFF1),
                topLeft = Offset(flashStart + flashW - 8f, midY - 10f),
                size = Size(w * 0.06f, 20f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        Text(
            text = "Peripheral IV Catheter · Identify gauge & primary clinical indication",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun BmwBinsSpotterCard(
    highlightBin: String,
    modifier: Modifier = Modifier
) {
    data class BinSpec(val id: String, val label: String, val fill: Color, val textOnFill: Color)
    val bins = remember {
        listOf(
            BinSpec("YELLOW", "YELLOW", Color(0xFFFBC02D), PureBlack),
            BinSpec("RED", "RED", Color(0xFFD32F2F), PureWhite),
            BinSpec("BLUE", "BLUE", Color(0xFF1976D2), PureWhite),
            BinSpec("WHITE", "WHITE", Color(0xFFECEFF1), PureBlack)
        )
    }

    Column(
        modifier = modifier
            .background(Color(0xFF101319))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "INDIA BIO-MEDICAL WASTE RULES (2016) · HIGHLIGHTED BIN SPOTTER",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
            color = PureWhite.copy(alpha = 0.78f),
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bins.forEach { bin ->
                val isTarget = bin.id == highlightBin
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isTarget) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AcidLime
                        ) {
                            Text(
                                text = "TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = PureBlack,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                    // Bin lid + body
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                        color = if (isTarget) bin.fill else bin.fill.copy(alpha = 0.28f),
                        border = BorderStroke(
                            width = if (isTarget) 2.5.dp else 1.dp,
                            color = if (isTarget) AcidLime else PureWhite.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTarget) 88.dp else 72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "☣",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isTarget) bin.textOnFill else PureWhite.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = bin.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTarget) bin.textOnFill else PureWhite.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Match the highlighted $highlightBin container with its designated hospital waste stream",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun VenturiMaskSpotterCard(modifier: Modifier = Modifier) {
    data class AdapterSpec(val label: String, val fio2: String, val color: Color, val fg: Color)
    val adapters = remember {
        listOf(
            AdapterSpec("Blue", "24%", Color(0xFF1E88E5), PureWhite),
            AdapterSpec("White", "28%", Color(0xFFECEFF1), PureBlack),
            AdapterSpec("Yellow", "35%", Color(0xFFFDD835), PureBlack),
            AdapterSpec("Red", "40%", Color(0xFFE53935), PureWhite),
            AdapterSpec("Green", "60%", Color(0xFF43A047), PureWhite)
        )
    }

    Column(
        modifier = modifier
            .background(Color(0xFF0E141B))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HIGH-FLOW AIR-ENTRAINMENT MASK",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = AcidLime,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fixed FiO₂ Jet Adapters",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.72f)
            )
        }

        // Schematic mask + jet entrainment valve drawing
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            val cx = size.width * 0.5f
            val maskTop = size.height * 0.08f
            val maskBottom = size.height * 0.62f
            val maskW = size.width * 0.24f

            // Clear facial mask dome
            val maskPath = Path().apply {
                moveTo(cx, maskTop)
                cubicTo(
                    cx + maskW * 0.85f, maskTop + 10f,
                    cx + maskW, maskBottom - 8f,
                    cx + maskW * 0.42f, maskBottom
                )
                lineTo(cx - maskW * 0.42f, maskBottom)
                cubicTo(
                    cx - maskW, maskBottom - 8f,
                    cx - maskW * 0.85f, maskTop + 10f,
                    cx, maskTop
                )
                close()
            }
            drawPath(maskPath, color = Color(0x4480DEEA))
            drawPath(maskPath, color = Color(0xCC80DEEA), style = Stroke(width = 2.4f))

            // Color-coded jet barrel (Blue 24% installed)
            val barrelW = maskW * 0.48f
            drawRoundRect(
                color = Color(0xFF1E88E5),
                topLeft = Offset(cx - barrelW / 2f, maskBottom),
                size = Size(barrelW, size.height * 0.22f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Air-entrainment side windows
            drawCircle(
                color = Color(0xFF0E141B),
                radius = 5f,
                center = Offset(cx - barrelW * 0.22f, maskBottom + size.height * 0.11f)
            )
            drawCircle(
                color = Color(0xFF0E141B),
                radius = 5f,
                center = Offset(cx + barrelW * 0.22f, maskBottom + size.height * 0.11f)
            )
            // O2 supply tubing below
            drawLine(
                color = Color(0xFF90CAF9),
                start = Offset(cx, maskBottom + size.height * 0.22f),
                end = Offset(cx, size.height * 0.96f),
                strokeWidth = 5f
            )
        }

        // Color-coded jet adapter strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            adapters.forEach { a ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = a.color,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = a.fio2,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.ExtraBold,
                            color = a.fg
                        )
                        Text(
                            text = a.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = a.fg.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
