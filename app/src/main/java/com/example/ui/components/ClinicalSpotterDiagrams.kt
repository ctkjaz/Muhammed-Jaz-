package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

private fun spotterGaussian(x: Float, mean: Float, sigma: Float): Float {
    val z = (x - mean) / sigma
    return exp(-0.5f * z * z)
}

/**
 * Calibrated Obstetric Cardiotocography (CTG) dual-channel strip:
 * - Upper channel: Fetal Heart Rate (FHR, 60–200 bpm, normal band 110–160 shaded)
 * - Lower channel: Uterine Tocodynamometer (0–100 mmHg contractions)
 */
@Composable
fun CtgTraceSpotterCanvas(
    isLateDecel: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFFFFBF4))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CTG · FHR (60–200 bpm) & TOCO (mmHg)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1F2B)
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1A1F2B)
            ) {
                Text(
                    text = "1 cm/min",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val fhrTop = 2f
            val fhrBottom = h * 0.58f
            val fhrH = fhrBottom - fhrTop
            val tocoTop = h * 0.65f
            val tocoBottom = h - 2f
            val tocoH = tocoBottom - tocoTop

            val gridLight = Color(0xFFE8D5C4)
            val gridMajor = Color(0xFFD6B296)

            // Normal FHR band (110-160 bpm inside 60-200 range)
            val y160 = fhrTop + fhrH * ((200f - 160f) / 140f)
            val y110 = fhrTop + fhrH * ((200f - 110f) / 140f)
            drawRect(
                color = Color(0x284CAF50),
                topLeft = Offset(0f, y160),
                size = Size(w, y110 - y160)
            )

            // Vertical time grid lines
            val cols = 20
            for (c in 0..cols) {
                val x = w * c / cols.toFloat()
                val isMajor = c % 4 == 0
                drawLine(
                    color = if (isMajor) gridMajor else gridLight,
                    start = Offset(x, fhrTop),
                    end = Offset(x, fhrBottom),
                    strokeWidth = if (isMajor) 1.5f else 0.8f
                )
                drawLine(
                    color = if (isMajor) gridMajor else gridLight,
                    start = Offset(x, tocoTop),
                    end = Offset(x, tocoBottom),
                    strokeWidth = if (isMajor) 1.5f else 0.8f
                )
            }

            // Horizontal FHR grid lines (every 20 bpm from 60 to 200)
            for (step in 0..7) {
                val y = fhrTop + fhrH * step / 7f
                drawLine(gridLight, Offset(0f, y), Offset(w, y), strokeWidth = 0.9f)
            }
            for (step in 0..4) {
                val y = tocoTop + tocoH * step / 4f
                drawLine(gridLight, Offset(0f, y), Offset(w, y), strokeWidth = 0.9f)
            }

            // Channel divider
            drawLine(Color(0xFF8C7A6B), Offset(0f, (fhrBottom + tocoTop) / 2f), Offset(w, (fhrBottom + tocoTop) / 2f), strokeWidth = 1.6f)

            val contractionCenters = listOf(0.20f, 0.52f, 0.82f)

            // Dashed vertical markers at contraction peaks to highlight timing relationship
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            contractionCenters.forEach { cx ->
                val x = w * cx
                drawLine(
                    color = Color(0x88D32F2F),
                    start = Offset(x, fhrTop),
                    end = Offset(x, tocoBottom),
                    strokeWidth = 1.3f,
                    pathEffect = dashEffect
                )
            }

            val samples = 260
            val fhrPath = Path()
            val tocoPath = Path()

            for (i in 0..samples) {
                val u = i.toFloat() / samples.toFloat()
                val x = u * w

                // Uterine contractions (smooth symmetric hills)
                var toco = 14f + 2f * sin(u * 2f * PI.toFloat() * 9f)
                contractionCenters.forEach { c ->
                    toco += 68f * spotterGaussian(u, c, 0.055f)
                }
                val tocoY = tocoBottom - (toco.coerceIn(0f, 100f) / 100f) * tocoH

                // FHR baseline (~142 bpm) + moderate variability
                val variability = 3.2f * sin(u * 2f * PI.toFloat() * 29f) +
                    2.0f * sin(u * 2f * PI.toFloat() * 47f + 0.8f)
                var fhr = 142f + variability

                if (isLateDecel) {
                    // Late decelerations: gradual symmetric dip whose nadir LAGS AFTER contraction peak
                    contractionCenters.forEach { c ->
                        fhr -= 44f * spotterGaussian(u, c + 0.075f, 0.052f)
                    }
                } else {
                    // Variable decelerations: abrupt steep V/W drops varying relative to contractions
                    val varDrops = listOf(0.16f to 58f, 0.56f to 64f, 0.77f to 52f)
                    varDrops.forEach { (center, depth) ->
                        val dist = abs(u - center)
                        if (dist < 0.042f) {
                            val vShape = 1f - (dist / 0.042f)
                            fhr -= depth * vShape * vShape
                        }
                    }
                }

                val fhrY = fhrTop + ((200f - fhr.coerceIn(60f, 200f)) / 140f) * fhrH
                if (i == 0) {
                    fhrPath.moveTo(x, fhrY)
                    tocoPath.moveTo(x, tocoY)
                } else {
                    fhrPath.lineTo(x, fhrY)
                    tocoPath.lineTo(x, tocoY)
                }
            }

            drawPath(
                path = fhrPath,
                color = Color(0xFF0D47A1),
                style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = tocoPath,
                color = Color(0xFF4E342E),
                style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Green band: 110–160 bpm · Red dashed line: contraction peak",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4A4E5A)
            )
        }
    }
}

/**
 * WHO Modified Partograph cervicograph (Active phase 4–10 cm) showing:
 * - Green Alert Line (1 cm/hr from 4 cm)
 * - Red Action Line (4 hours right of Alert Line)
 * - Plotted patient dilatation curve (×) crossing RIGHT of the Alert Line
 */
@Composable
fun PartographSpotterCanvas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFFFAFBFD))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WHO MODIFIED PARTOGRAPH · CERVICOGRAPH (4–10 cm)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF141822)
            )
            Text(
                text = "Active Phase",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val leftPad = 18f
            val rightPad = 10f
            val topPad = 8f
            val bottomPad = 14f
            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            val hours = 12 // 0 to 12 hours
            val cmMin = 4f
            val cmMax = 10f

            fun xForHour(hr: Float): Float = leftPad + (hr / hours.toFloat()) * plotW
            fun yForCm(cm: Float): Float = topPad + ((cmMax - cm) / (cmMax - cmMin)) * plotH

            // Grid
            for (h in 0..hours) {
                val x = xForHour(h.toFloat())
                drawLine(Color(0xFFD6DCE5), Offset(x, topPad), Offset(x, topPad + plotH), strokeWidth = 1f)
            }
            for (cm in 4..10) {
                val y = yForCm(cm.toFloat())
                drawLine(Color(0xFFD6DCE5), Offset(leftPad, y), Offset(leftPad + plotW, y), strokeWidth = 1f)
            }

            // Border axes
            drawLine(Color(0xFF263238), Offset(leftPad, topPad), Offset(leftPad, topPad + plotH), strokeWidth = 2f)
            drawLine(Color(0xFF263238), Offset(leftPad, topPad + plotH), Offset(leftPad + plotW, topPad + plotH), strokeWidth = 2f)

            // Alert Line: (0h, 4cm) -> (6h, 10cm)
            drawLine(
                color = Color(0xFF2E7D32),
                start = Offset(xForHour(0f), yForCm(4f)),
                end = Offset(xForHour(6f), yForCm(10f)),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )

            // Action Line: (4h, 4cm) -> (10h, 10cm)
            drawLine(
                color = Color(0xFFC62828),
                start = Offset(xForHour(4f), yForCm(4f)),
                end = Offset(xForHour(10f), yForCm(10f)),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )

            // Plotted patient dilatation curve crossing RIGHT of Alert line
            val points = listOf(
                0f to 4f,
                2f to 5f,
                4f to 5.5f,
                6f to 6f,
                7.5f to 6.5f
            )
            val curve = Path()
            points.forEachIndexed { idx, (hr, cm) ->
                val px = xForHour(hr)
                val py = yForCm(cm)
                if (idx == 0) curve.moveTo(px, py) else curve.lineTo(px, py)
            }
            drawPath(
                path = curve,
                color = Color(0xFF1565C0),
                style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Draw "X" cervical dilatation markers
            points.forEach { (hr, cm) ->
                val px = xForHour(hr)
                val py = yForCm(cm)
                val r = 6.5f
                drawLine(Color(0xFF0D47A1), Offset(px - r, py - r), Offset(px + r, py + r), strokeWidth = 2.6f)
                drawLine(Color(0xFF0D47A1), Offset(px - r, py + r), Offset(px + r, py - r), strokeWidth = 2.6f)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("— Alert (1 cm/hr)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Text("— Action (+4 hr)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            Text("× Patient Curve", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * High-contrast radiology lightbox schematic for classic NORCET chest X-ray emergencies:
 * - Tension Pneumothorax: hyperlucent right hemithorax, collapsed visceral lung edge, contralateral tracheal shift
 * - Pleural Effusion: dense right basal white-out with blunted costophrenic angle & upward concave meniscus sign
 */
@Composable
fun ChestXraySchematicCanvas(
    isTensionPneumo: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF090D12))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val topY = size.height * 0.08f
            val botY = size.height * 0.88f
            val ribW = size.width * 0.38f

            // Outer ribcage silhouette
            val cagePath = Path().apply {
                moveTo(cx - ribW * 0.45f, topY)
                cubicTo(cx - ribW * 1.05f, size.height * 0.28f, cx - ribW * 1.08f, size.height * 0.72f, cx - ribW * 0.88f, botY)
                lineTo(cx + ribW * 0.88f, botY)
                cubicTo(cx + ribW * 1.08f, size.height * 0.72f, cx + ribW * 1.05f, size.height * 0.28f, cx + ribW * 0.45f, topY)
                close()
            }
            drawPath(cagePath, color = Color(0xFF1A222D))
            drawPath(cagePath, color = Color(0x66B0BEC5), style = Stroke(width = 2f))

            // Patient's RIGHT hemithorax (viewer's LEFT side!)
            val rightLungPath = Path().apply {
                moveTo(cx - ribW * 0.14f, topY + 12f)
                cubicTo(cx - ribW * 0.88f, size.height * 0.26f, cx - ribW * 0.92f, size.height * 0.68f, cx - ribW * 0.78f, botY - 8f)
                quadraticTo(cx - ribW * 0.45f, botY - 22f, cx - ribW * 0.14f, botY - 12f)
                close()
            }
            // Patient's LEFT hemithorax (viewer's RIGHT side)
            val leftLungPath = Path().apply {
                moveTo(cx + ribW * 0.14f, topY + 12f)
                cubicTo(cx + ribW * 0.88f, size.height * 0.26f, cx + ribW * 0.92f, size.height * 0.68f, cx + ribW * 0.78f, botY - 8f)
                quadraticTo(cx + ribW * 0.45f, botY - 22f, cx + ribW * 0.14f, botY - 12f)
                close()
            }

            if (isTensionPneumo) {
                // Dark jet-black hyperlucent right hemithorax (trapped air, NO vascular markings)
                drawPath(rightLungPath, color = Color(0xFF040608))
                // Left lung normal grey aerated field
                drawPath(leftLungPath, color = Color(0xFF283442))

                // Collapsed right lung edge huddled near hilum
                val collapsedEdge = Path().apply {
                    moveTo(cx - ribW * 0.22f, size.height * 0.30f)
                    cubicTo(cx - ribW * 0.44f, size.height * 0.44f, cx - ribW * 0.42f, size.height * 0.66f, cx - ribW * 0.18f, size.height * 0.74f)
                }
                drawPath(
                    path = collapsedEdge,
                    color = AcidLime,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )

                // Shifted trachea & mediastinum deviated to viewer's RIGHT (patient's LEFT)
                val shift = ribW * 0.20f
                val tracheaPath = Path().apply {
                    moveTo(cx, topY - 4f)
                    quadraticTo(cx + shift * 0.8f, size.height * 0.26f, cx + shift, size.height * 0.42f)
                }
                drawPath(
                    path = tracheaPath,
                    color = Color(0xFFFF7043),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                // Shifted cardiac shadow
                drawOval(
                    color = Color(0xCCCFD8DC),
                    topLeft = Offset(cx + shift * 0.3f - ribW * 0.12f, size.height * 0.46f),
                    size = Size(ribW * 0.50f, size.height * 0.32f)
                )
            } else {
                // Pleural effusion: both lungs aerated above, dense white basal fluid meniscus on patient's RIGHT (viewer's LEFT)
                drawPath(rightLungPath, color = Color(0xFF26323F))
                drawPath(leftLungPath, color = Color(0xFF26323F))

                // Central trachea (midline)
                drawLine(
                    color = Color(0xAA90A4AE),
                    start = Offset(cx, topY - 4f),
                    end = Offset(cx, size.height * 0.38f),
                    strokeWidth = 4.5f,
                    cap = StrokeCap.Round
                )
                // Normal cardiac shadow
                drawOval(
                    color = Color(0xBBCFD8DC),
                    topLeft = Offset(cx - ribW * 0.16f, size.height * 0.48f),
                    size = Size(ribW * 0.48f, size.height * 0.32f)
                )

                // Dense white effusion with concave upward Meniscus Sign obliterating right costophrenic angle
                val effusionPath = Path().apply {
                    moveTo(cx - ribW * 0.86f, size.height * 0.54f)
                    // Concave upward meniscus curving down toward mediastinum
                    quadraticTo(cx - ribW * 0.52f, size.height * 0.70f, cx - ribW * 0.14f, size.height * 0.66f)
                    lineTo(cx - ribW * 0.14f, botY - 8f)
                    lineTo(cx - ribW * 0.82f, botY - 6f)
                    close()
                }
                drawPath(effusionPath, color = Color(0xFFE3EDF5))
                // Highlight meniscus curve
                val meniscusLine = Path().apply {
                    moveTo(cx - ribW * 0.86f, size.height * 0.54f)
                    quadraticTo(cx - ribW * 0.52f, size.height * 0.70f, cx - ribW * 0.14f, size.height * 0.66f)
                }
                drawPath(
                    path = meniscusLine,
                    color = AcidLime,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // Faint rib arcs for realistic CXR orientation
            for (r in 1..4) {
                val ry = topY + (botY - topY) * (r * 0.18f)
                drawLine(Color(0x28CFD8DC), Offset(cx - ribW * 0.82f, ry), Offset(cx + ribW * 0.82f, ry), strokeWidth = 1.4f)
            }
        }

        // Radiograph orientation markers (R / L)
        Text(
            text = "R",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
            fontWeight = FontWeight.ExtraBold,
            color = AcidLime,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )
        Text(
            text = "L",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
            fontWeight = FontWeight.ExtraBold,
            color = PureWhite.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xDD1A1D24),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = if (isTensionPneumo) {
                    "PA CXR · Collapsed Edge (Lime) · Tracheal Shift (Orange)"
                } else {
                    "ERECT PA CXR · Right Basal Opacity & Meniscus Sign (Lime)"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Wallace Rule of Nines adult burn diagram highlighting:
 * - Anterior Trunk (18%)
 * - Entire Right Arm (9%)
 * - Anterior Right Leg (9%)
 * Total shaded = 36% TBSA.
 */
@Composable
fun RuleOfNinesBurnCanvas(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF0E141B))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            val cx = size.width * 0.5f
            val h = size.height
            val burnFill = Color(0xFFE65100)
            val burnStroke = Color(0xFFFFB74D)
            val normalFill = Color(0xFF263238)
            val normalStroke = Color(0xFF78909C)

            // Head (unburned: 9% total)
            drawCircle(
                color = normalFill,
                radius = h * 0.095f,
                center = Offset(cx, h * 0.13f)
            )
            drawCircle(
                color = normalStroke,
                radius = h * 0.095f,
                center = Offset(cx, h * 0.13f),
                style = Stroke(width = 2f)
            )

            // Anterior Trunk (BURNED: 18%)
            val trunkW = h * 0.25f
            val trunkH = h * 0.30f
            val trunkTop = h * 0.25f
            drawRoundRect(
                color = burnFill,
                topLeft = Offset(cx - trunkW / 2f, trunkTop),
                size = Size(trunkW, trunkH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = burnStroke,
                topLeft = Offset(cx - trunkW / 2f, trunkTop),
                size = Size(trunkW, trunkH),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2.4f)
            )

            // Patient's RIGHT arm (viewer's LEFT - BURNED ENTIRE: 9%)
            val armW = h * 0.085f
            val armH = h * 0.31f
            drawRoundRect(
                color = burnFill,
                topLeft = Offset(cx - trunkW / 2f - armW - 6f, trunkTop + 4f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = burnStroke,
                topLeft = Offset(cx - trunkW / 2f - armW - 6f, trunkTop + 4f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2.2f)
            )

            // Patient's LEFT arm (viewer's RIGHT - unburned)
            drawRoundRect(
                color = normalFill,
                topLeft = Offset(cx + trunkW / 2f + 6f, trunkTop + 4f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = normalStroke,
                topLeft = Offset(cx + trunkW / 2f + 6f, trunkTop + 4f),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2f)
            )

            // Patient's RIGHT leg anterior (viewer's LEFT - BURNED: 9%)
            val legW = trunkW * 0.44f
            val legH = h * 0.35f
            val legTop = trunkTop + trunkH + 6f
            drawRoundRect(
                color = burnFill,
                topLeft = Offset(cx - trunkW / 2f, legTop),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = burnStroke,
                topLeft = Offset(cx - trunkW / 2f, legTop),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2.2f)
            )

            // Patient's LEFT leg (viewer's RIGHT - unburned)
            drawRoundRect(
                color = normalFill,
                topLeft = Offset(cx + trunkW / 2f - legW, legTop),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = normalStroke,
                topLeft = Offset(cx + trunkW / 2f - legW, legTop),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2f)
            )
        }

        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x33FF9800),
                border = BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Text(
                    text = "SHADED BURN ZONES",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC80),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "• Anterior Trunk (Chest + Abd)\n• Entire Right Arm (Ant + Post)\n• Anterior Right Leg",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.9f)
            )
            Text(
                text = "Patient Weight: 70 kg\nCompute %TBSA & Parkland 1st 8h RL",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = AcidLime,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Non-Rebreather (NRB) Oxygen Mask with pre-inflated reservoir bag and one-way valves.
 */
@Composable
fun NonRebreatherMaskSpotterCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF0E141B))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxSize()
        ) {
            val cx = size.width * 0.5f
            val h = size.height
            val maskW = size.width * 0.42f

            // Mask cone
            val maskPath = Path().apply {
                moveTo(cx, h * 0.06f)
                cubicTo(cx + maskW, h * 0.14f, cx + maskW * 0.95f, h * 0.40f, cx + maskW * 0.40f, h * 0.44f)
                lineTo(cx - maskW * 0.40f, h * 0.44f)
                cubicTo(cx - maskW * 0.95f, h * 0.40f, cx - maskW, h * 0.14f, cx, h * 0.06f)
                close()
            }
            drawPath(maskPath, color = Color(0x444DD0E1))
            drawPath(maskPath, color = Color(0xDD80DEEA), style = Stroke(width = 2.4f))

            // One-way side exhalation flap valve (highlighted)
            drawCircle(
                color = AcidLime,
                radius = 7.5f,
                center = Offset(cx + maskW * 0.52f, h * 0.28f)
            )
            // One-way inspiratory valve neck between mask and bag
            drawRoundRect(
                color = Color(0xFFCFD8DC),
                topLeft = Offset(cx - 12f, h * 0.44f),
                size = Size(24f, h * 0.10f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Large inflated O2 reservoir bag below
            val bagW = maskW * 1.15f
            drawRoundRect(
                color = Color(0x4481D4FA),
                topLeft = Offset(cx - bagW / 2f, h * 0.54f),
                size = Size(bagW, h * 0.38f),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = Color(0xDD4FC3F7),
                topLeft = Offset(cx - bagW / 2f, h * 0.54f),
                size = Size(bagW, h * 0.38f),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 2.4f)
            )
        }

        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AcidLime
            ) {
                Text(
                    text = "RESERVOIR MASK + 1-WAY VALVES",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = PureBlack,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "• High-concentration O₂ mask\n• One-way flap prevents room air entry\n• Reservoir bag must stay ≥ 2/3 inflated",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.88f)
            )
            Text(
                text = "Identify mandatory flow rate & nursing setup",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = AcidLime
            )
        }
    }
}
