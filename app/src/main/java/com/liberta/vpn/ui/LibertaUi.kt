package com.liberta.vpn.ui

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.liberta.vpn.BuildConfig
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionPhase
import com.liberta.vpn.data.DnsProvider
import com.liberta.vpn.data.LabSettings
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.PhantomMimicryType
import com.liberta.vpn.data.PhantomNoiseProfile
import com.liberta.vpn.data.PhantomTransportService
import com.liberta.vpn.data.RelayRole
import com.liberta.vpn.data.TlsFingerprintProfile
import com.liberta.vpn.data.VpnStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GithubRepoUrl = "https://github.com/lovestove/Liberta"

private val Ink = Color(0xFFF7FAFC)
private val Deep = Color(0xFFE7EEF4)
private val Graphite = Color(0xFF203545)
private val Glass = Color(0xB8FFFFFF)
private val GlassSoft = Color(0x8EFFFFFF)
private val Line = Color(0x99FFFFFF)
private val Gold = Color(0xFFDDB85B)
private val Pearl = Color(0xFFF5FAFC)
private val TextPrimary = Color(0xFF132C3B)
private val TextSecondary = Color(0xFF587080)
private val Muted = Color(0xFF8195A2)
private val Emerald = Color(0xFF5FE2B3)
private val Azure = Color(0xFF77B8FF)
private val Amber = Color(0xFFE8B452)
private val Rose = Color(0xFFD96C79)

@Composable
private fun rememberDeviceParallax(): Offset {
    val context = LocalContext.current
    var offset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            onDispose {}
        } else {
            val listener = object : SensorEventListener {
                private var x = 0f
                private var y = 0f

                override fun onSensorChanged(event: SensorEvent) {
                    val nextX = (-event.values[0] / 7.5f).coerceIn(-1f, 1f)
                    val nextY = (event.values[1] / 7.5f).coerceIn(-1f, 1f)
                    x += (nextX - x) * 0.10f
                    y += (nextY - y) * 0.10f
                    offset = Offset(x, y)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { manager.unregisterListener(listener) }
        }
    }

    return offset
}

@Composable
fun LibertaApp(
    settings: LibertaSettings,
    status: VpnStatus,
    onPower: () -> Unit,
    onConnectionModePower: (ConnectionMethod) -> Unit,
    onRecover: () -> Unit,
    onRefresh: () -> Unit,
    onShareApp: () -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onAutoRefreshOnLaunchChange: (Boolean) -> Unit,
    onAutoRefreshIntervalChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDnsProviderChange: (DnsProvider) -> Unit,
    onMtuChange: (Int) -> Unit,
    onSettingsChange: ((LibertaSettings) -> LibertaSettings) -> Unit,
    onLabsChange: ((LabSettings) -> LabSettings) -> Unit
) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var showQr by rememberSaveable { mutableStateOf(false) }
    var surgeKey by remember { mutableStateOf(0) }
    var surgeOrigin by remember { mutableStateOf(Offset.Zero) }
    val surge = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 28) }.getOrNull()
    }
    val parallax = rememberDeviceParallax()

    LaunchedEffect(surgeKey) {
        if (surgeKey > 0) {
            surge.snapTo(0f)
            surge.animateTo(1f, tween(1_360, easing = FastOutSlowInEasing))
        }
    }

    DisposableEffect(tone) {
        onDispose { tone?.release() }
    }

    LaunchedEffect(status.phase) {
        if (status.phase == ConnectionPhase.CONNECTED) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            tone?.startTone(ToneGenerator.TONE_PROP_ACK, 64)
            delay(120)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 42)
        } else if (status.phase == ConnectionPhase.ERROR) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Ink)) {
            LivingBackground(
                status = status,
                meshEnabled = settings.labs.sovereignRelay,
                surgeProgress = surge.value,
                surgeOrigin = surgeOrigin,
                parallax = parallax
            )

            Crossfade(targetState = screen, label = "screen") { target ->
                if (target == "home") {
                    HomeScreen(
                        settings = settings,
                        status = status,
                        onSettings = { screen = "settings" },
                        onQr = { showQr = true },
                        onPower = { origin ->
                            surgeOrigin = origin
                            surgeKey += 1
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 46)
                            scope.launch {
                                delay(130)
                                onPower()
                            }
                        },
                        onConnectionModePower = onConnectionModePower,
                        onSettingsChange = onSettingsChange,
                        onLabsChange = onLabsChange,
                        onRecover = onRecover
                    )
                } else {
                    SettingsScreen(
                        settings = settings,
                        status = status,
                        onBack = { screen = "home" },
                        onRefresh = onRefresh,
                        onAutoRefreshChange = onAutoRefreshChange,
                        onAutoRefreshOnLaunchChange = onAutoRefreshOnLaunchChange,
                        onAutoRefreshIntervalChange = onAutoRefreshIntervalChange,
                        onAutoStartChange = onAutoStartChange,
                        onDnsProviderChange = onDnsProviderChange,
                        onMtuChange = onMtuChange,
                        onSettingsChange = onSettingsChange,
                        onLabsChange = onLabsChange
                    )
                }
            }

            if (showQr) {
                QrOverlay(onClose = { showQr = false }, onShareApp = onShareApp)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    settings: LibertaSettings,
    status: VpnStatus,
    onSettings: () -> Unit,
    onQr: () -> Unit,
    onPower: (Offset) -> Unit,
    onConnectionModePower: (ConnectionMethod) -> Unit,
    onSettingsChange: ((LibertaSettings) -> LibertaSettings) -> Unit,
    onLabsChange: ((LabSettings) -> LabSettings) -> Unit,
    onRecover: () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        val compact = maxHeight < 720.dp
        val lensTop = if (compact) 82.dp else 112.dp
        val lensWidth = if (compact) 0.48f else 0.56f

        FloatingTopActions(
            onSettings = onSettings,
            onQr = onQr,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        PowerLens(
            status = status,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = lensTop)
                .fillMaxWidth(lensWidth),
            onPower = onPower
        )

        HelpGratitudeRibbon(
            visible = settings.labs.sovereignRelay,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = lensTop + if (compact) 205.dp else 248.dp)
        )

        if (status.phase == ConnectionPhase.ERROR) {
            LensActionButton(
                onClick = onRecover,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = lensTop + if (compact) 252.dp else 295.dp)
                    .height(48.dp)
                    .width(172.dp)
            ) {
                Text("Восстановить", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        FloatingControlDock(
            settings = settings,
            status = status,
            onSettingsChange = onSettingsChange,
            onLabsChange = onLabsChange,
            onConnectionModePower = onConnectionModePower,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FloatingTopActions(onSettings: () -> Unit, onQr: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Настройки", tint = TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Liberta",
            color = TextPrimary.copy(alpha = 0.86f),
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        GlassIconButton(onClick = onQr) {
            Icon(Icons.Rounded.QrCode2, contentDescription = "QR", tint = TextPrimary)
        }
    }
}

@Composable
private fun HelpGratitudeRibbon(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.54f),
                            Emerald.copy(alpha = 0.20f),
                            Azure.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.44f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.76f), RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Вы помогаете людям, спасибо",
                color = TextPrimary.copy(alpha = 0.86f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PowerLens(status: VpnStatus, modifier: Modifier, onPower: (Offset) -> Unit) {
    val infinite = rememberInfiniteTransition(label = "power")
    val breathe by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200), RepeatMode.Reverse),
        label = "breathe"
    )
    val flow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_600), RepeatMode.Restart),
        label = "lensFlow"
    )
    val scale by animateFloatAsState(
        targetValue = if (status.isConnected) 1.025f else 0.99f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 130f),
        label = "powerScale"
    )
    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .aspectRatio(1f)
            .onGloballyPositioned { rootTopLeft = it.localToRoot(Offset.Zero) }
            .pointerInput(status.phase) {
                detectTapGestures { local -> onPower(rootTopLeft + local) }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.38f * scale
            val active = status.isConnected || status.isBusy
            val accent = status.accent()

            drawRefractionCaustics(center, radius, flow, active, status.trafficPulse)
            drawLensFieldArcs(center, radius, flow, active, status.trafficPulse, accent)
            drawCircle(
                color = Color.White.copy(alpha = 0.56f),
                radius = radius * 1.005f,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )
            drawCircle(
                color = Gold.copy(alpha = 0.30f + status.trafficPulse * 0.18f),
                radius = radius * (0.91f + breathe * 0.04f),
                center = center,
                style = Stroke(width = (1.1.dp + 2.dp * status.trafficPulse).toPx(), cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = radius * 0.18f,
                center = Offset(center.x - radius * 0.30f, center.y - radius * 0.36f)
            )
            with(DiffractionLensShader) {
                drawPowerGlyph(center, radius * 0.27f, flow * 6.28f, active, status.trafficPulse)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRefractionCaustics(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float
) {
    val streamAlpha = if (active) 0.24f + traffic * 0.18f else 0.12f
    repeat(7) { index ->
        val angle = (phase * 360f + index * 51f) * (PI.toFloat() / 180f)
        val start = Offset(
            center.x + cos(angle) * radius * (0.58f + index * 0.012f),
            center.y + sin(angle) * radius * (0.44f + index * 0.018f)
        )
        val c1 = Offset(
            center.x + cos(angle + 0.82f) * radius * (1.06f + traffic * 0.10f),
            center.y + sin(angle + 0.42f) * radius * (0.82f + traffic * 0.08f)
        )
        val end = Offset(
            center.x + cos(angle + 1.42f) * radius * (1.34f + traffic * 0.16f),
            center.y + sin(angle + 1.06f) * radius * (1.04f + traffic * 0.12f)
        )
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(c1.x, c1.y, end.x, end.y)
        }
        drawPath(
            path = path,
            color = listOf(Azure, Emerald, Gold)[index % 3].copy(alpha = streamAlpha),
            style = Stroke(width = (0.9f + index * 0.08f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    repeat(6) { index ->
        val angle = (phase * 2f * PI.toFloat()) + index * 1.08f
        val bubbleCenter = Offset(
            center.x + cos(angle) * radius * (0.74f + 0.05f * sin(phase * 6.28f + index)),
            center.y + sin(angle * 0.88f) * radius * (0.62f + 0.04f * cos(index.toFloat()))
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.32f), Azure.copy(alpha = 0.10f), Color.Transparent),
                center = bubbleCenter,
                radius = radius * 0.12f
            ),
            radius = radius * (0.030f + index * 0.006f),
            center = bubbleCenter
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLensFieldArcs(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float,
    accent: Color
) {
    val fieldAlpha = if (active) 0.20f + traffic * 0.14f else 0.10f
    repeat(9) { index ->
        val ring = radius * (0.82f + index * 0.055f)
        val start = phase * 360f + index * 39f
        drawArc(
            color = listOf(Azure, Emerald, Gold, accent)[index % 4].copy(alpha = fieldAlpha * (1f - index * 0.055f)),
            startAngle = start,
            sweepAngle = 18f + index * 2.8f,
            useCenter = false,
            topLeft = Offset(center.x - ring, center.y - ring),
            size = Size(ring * 2f, ring * 2f),
            style = Stroke(width = (0.70f + traffic * 1.1f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    repeat(5) { index ->
        val angle = phase * 6.28f + index * 1.18f
        val p = Offset(
            center.x + cos(angle) * radius * (0.58f + index * 0.045f),
            center.y + sin(angle * 0.92f) * radius * (0.52f + index * 0.030f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.20f), Azure.copy(alpha = 0.07f), Color.Transparent),
                center = p,
                radius = radius * 0.11f
            ),
            radius = radius * (0.018f + index * 0.004f),
            center = p
        )
    }
}

@Composable
private fun FloatingControlDock(
    settings: LibertaSettings,
    status: VpnStatus,
    onSettingsChange: ((LibertaSettings) -> LibertaSettings) -> Unit,
    onLabsChange: ((LabSettings) -> LabSettings) -> Unit,
    onConnectionModePower: (ConnectionMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var infoMode by rememberSaveable { mutableStateOf<ConnectionMode?>(null) }
    val selectedMode = settings.connectionMode()
    GlassPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                statusText(status),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                statusDetail(status),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            ConnectionMode.entries.forEach { mode ->
                DockButton(
                    title = mode.title,
                    selected = selectedMode == mode,
                    enabled = !status.isBusy && !status.isConnected,
                    onClick = {
                        onSettingsChange { current ->
                            current.copy(
                                connectionMethod = mode.method,
                                profile = mode.method.profile
                            )
                        }
                        onConnectionModePower(mode.method)
                    },
                    onInfo = {
                        infoMode = if (infoMode == mode) null else mode
                    }
                )
            }

            AnimatedVisibility(infoMode != null) {
                Text(
                    infoMode?.info.orEmpty(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.36f))
                        .border(1.dp, Color.White.copy(alpha = 0.66f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DockButton(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onInfo: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "dockLens")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_800), RepeatMode.Restart),
        label = "dockPhase"
    )
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 360f),
        label = "dockPressGlow"
    )
    val alpha = if (enabled) 1f else 0.52f
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "dockScale"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .alpha(alpha)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLensButtonSurface(
                selected = selected,
                enabled = enabled,
                phase = phase,
                cornerRadiusPx = 22.dp.toPx(),
                press = pressGlow
            )
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            )
            LensIconButton(onClick = onInfo, enabled = true, selected = selected) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "Что делает $title",
                    tint = if (selected) Azure else Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LensIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "lensIcon")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_200), RepeatMode.Restart),
        label = "lensIconPhase"
    )
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.42f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLensCircleSurface(selected, enabled, phase)
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLensButtonSurface(
    selected: Boolean,
    enabled: Boolean,
    phase: Float,
    cornerRadiusPx: Float,
    press: Float = 0f
) {
    val active = selected && enabled
    val center = Offset(
        size.width * (0.46f + sin(phase * 6.28f) * 0.035f + press * 0.10f),
        size.height * (0.48f - press * 0.08f)
    )
    val radius = size.maxDimension * 0.94f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (active) 0.30f + press * 0.16f else 0.16f + press * 0.10f),
                Azure.copy(alpha = if (active) 0.11f + press * 0.08f else 0.040f + press * 0.045f),
                Emerald.copy(alpha = if (active) 0.08f + press * 0.08f else 0.028f + press * 0.040f),
                Color.White.copy(alpha = 0.035f)
            ),
            center = Offset(size.width * 0.28f, size.height * 0.18f),
            radius = radius
        ),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.20f + press * 0.18f),
                Color.White.copy(alpha = 0.025f),
                Gold.copy(alpha = if (active) 0.12f else 0.035f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )
    repeat(5) { index ->
        val y = size.height * (0.18f + index * 0.17f + sin(phase * 6.28f + index) * 0.025f)
        val path = Path().apply {
            moveTo(size.width * -0.08f, y)
            cubicTo(
                size.width * (0.24f + phase * 0.08f),
                y - size.height * 0.18f,
                size.width * (0.58f - phase * 0.05f),
                y + size.height * 0.18f,
                size.width * 1.08f,
                y - size.height * 0.04f
            )
        }
        drawPath(
            path = path,
            color = listOf(Azure, Emerald, Gold)[index % 3].copy(alpha = (if (active) 0.18f else 0.08f) + press * 0.08f),
            style = Stroke(width = (0.75f + index * 0.08f + press * 0.60f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    repeat(3) { index ->
        drawCircle(
            color = listOf(Azure, Gold, Emerald)[index].copy(alpha = if (active) 0.18f else 0.08f),
            radius = size.minDimension * (0.50f + index * 0.10f),
            center = center + Offset(index * 9.dp.toPx(), -index * 3.dp.toPx()),
            style = Stroke(width = (0.8f + index * 0.22f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawRoundRect(
        color = Color.White.copy(alpha = if (active) 0.78f else 0.46f),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(width = 1.dp.toPx())
    )
    drawRoundRect(
        color = Azure.copy(alpha = if (active) 0.18f else 0.08f),
        topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
        size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
        cornerRadius = CornerRadius(cornerRadiusPx * 0.86f, cornerRadiusPx * 0.86f),
        style = Stroke(width = 0.8.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLensCircleSurface(
    selected: Boolean,
    enabled: Boolean,
    phase: Float
) {
    val active = selected && enabled
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = if (active) 0.44f else 0.25f),
                Azure.copy(alpha = if (active) 0.18f else 0.065f),
                Color.White.copy(alpha = 0.035f)
            ),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.34f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = center
    )
    drawCircle(Color.White.copy(alpha = 0.52f), radius = radius * 0.96f, center = center, style = Stroke(width = 1.dp.toPx()))
    repeat(3) { index ->
        drawArc(
            color = listOf(Azure, Gold, Emerald)[index].copy(alpha = if (active) 0.22f else 0.10f),
            startAngle = phase * 360f + index * 94f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
            size = Size(radius * 1.64f, radius * 1.64f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private enum class ConnectionMode(
    val title: String,
    val method: ConnectionMethod,
    val info: String
) {
    BLACKLISTS(
        "Черные списки",
        ConnectionMethod.BLACKLISTS,
        "Подключает интернет через публичный VPN-маршрут для сайтов и сервисов, которые обычно блокируются."
    ),
    WHITELISTS(
        "Белые списки",
        ConnectionMethod.WHITELISTS,
        "Подключает интернет через более строгий белый источник, когда обычный маршрут не подходит."
    ),
    PHANTOM_CALL(
        "Мимикрия под звонки",
        ConnectionMethod.PHANTOM_CALL,
        "Запускает VPN и автоматически создает бесплатную комнату звонка. Ничего вручную настраивать не нужно."
    ),
    MESH(
        "Меш-сеть",
        ConnectionMethod.MESH_ACCESS,
        "Подключает вас к mesh-доступу, чтобы пользоваться интернетом через доступные узлы. Это не включает помощь другим."
    )
}

private fun LibertaSettings.connectionMode(): ConnectionMode =
    when (connectionMethod) {
        ConnectionMethod.BLACKLISTS -> ConnectionMode.BLACKLISTS
        ConnectionMethod.WHITELISTS -> ConnectionMode.WHITELISTS
        ConnectionMethod.PHANTOM_CALL -> ConnectionMode.PHANTOM_CALL
        ConnectionMethod.MESH_ACCESS -> ConnectionMode.MESH
    }

@Composable
private fun SettingsScreen(
    settings: LibertaSettings,
    status: VpnStatus,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onAutoRefreshOnLaunchChange: (Boolean) -> Unit,
    onAutoRefreshIntervalChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDnsProviderChange: (DnsProvider) -> Unit,
    onMtuChange: (Int) -> Unit,
    onSettingsChange: ((LibertaSettings) -> LibertaSettings) -> Unit,
    onLabsChange: ((LabSettings) -> LabSettings) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
                Text(
                    "Настройки",
                    color = TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        item {
            SettingsPanel("Работоспособность") {
                SettingLine("Состояние", statusText(status), "Проверяется по реальному запуску туннеля и интернет-пробам.")
                SettingLine("Обновлено", status.lastUpdatedEpochMs?.formatTime() ?: "Нет данных", "Время последней успешной загрузки всех источников.")
                ToggleLine("Автообновление", "Периодически проверяет подписки без ручного запуска.", settings.autoRefresh, onAutoRefreshChange)
                ToggleLine("При запуске", "Обновляет серверы при открытии приложения.", settings.autoRefreshOnLaunch, onAutoRefreshOnLaunchChange)
                IntervalSelector(settings.autoRefreshIntervalMinutes, settings.autoRefresh, onAutoRefreshIntervalChange)
                LensActionButton(
                    onClick = onRefresh,
                    enabled = !status.isBusy,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить все", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            SettingsPanel("Фундамент сети") {
                ToggleLine("Автозапуск", "Запускает Liberta вместе с системой Android.", settings.autoStart, onAutoStartChange)
                ToggleLine("Kill Switch", "Блокирует трафик при внезапном разрыве туннеля.", settings.killSwitch, onChecked = { checked ->
                    onSettingsChange { it.copy(killSwitch = checked) }
                })
                ToggleLine("IPv6 внутри туннеля", "Включает IPv6-маршрут; выключение снижает риск утечек.", settings.ipv6Enabled, onChecked = { checked ->
                    onSettingsChange { it.copy(ipv6Enabled = checked) }
                })
                ToggleLine("Авто MTU", "Использует стабильное значение для Wi-Fi и мобильных сетей.", settings.autoMtu, onChecked = { checked ->
                    onSettingsChange { it.copy(autoMtu = checked) }
                })
                StepLine(
                    title = "MTU",
                    subtitle = "Ручная подстройка, если провайдер режет пакеты.",
                    value = settings.mtu.toString(),
                    enabled = !settings.autoMtu,
                    onMinus = { onMtuChange(settings.mtu - 20) },
                    onPlus = { onMtuChange(settings.mtu + 20) }
                )
                Text("DNS-провайдер", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                EnumPills(DnsProvider.entries, settings.dnsProvider, { it.label }, onDnsProviderChange)
                AnimatedVisibility(settings.dnsProvider == DnsProvider.CUSTOM) {
                    OutlinedTextField(
                        value = settings.customDns,
                        onValueChange = { value -> onSettingsChange { it.copy(customDns = value) } },
                        label = { Text("DoH/DoT или IP DNS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            SettingsPanel("Поведение и комфорт") {
                ToggleLine("Раздельное туннелирование", "Готовит режим выбора приложений для прямого маршрута.", settings.splitTunneling, onChecked = { checked ->
                    onSettingsChange { it.copy(splitTunneling = checked) }
                })
                ToggleLine("Локальный прокси", "Открывает SOCKS/HTTP вход для устройств в одной Wi-Fi сети.", settings.proxyEnabled, onChecked = { checked ->
                    onSettingsChange { it.copy(proxyEnabled = checked) }
                })
                StepLine(
                    title = "Порт прокси",
                    subtitle = "Используется только если локальный прокси включен.",
                    value = settings.proxyPort.toString(),
                    enabled = settings.proxyEnabled,
                    onMinus = { onSettingsChange { it.copy(proxyPort = it.proxyPort - 1) } },
                    onPlus = { onSettingsChange { it.copy(proxyPort = it.proxyPort + 1) } }
                )
                ToggleLine("Динамическая тема", "Подстраивает акценты под систему, когда это безопасно для читаемости.", settings.dynamicTheme, onChecked = { checked ->
                    onSettingsChange { it.copy(dynamicTheme = checked) }
                })
                ToggleLine("Тихое уведомление", "Держит foreground VPN заметным, но без визуального шума.", settings.compactNotification, onChecked = { checked ->
                    onSettingsChange { it.copy(compactNotification = checked) }
                })
                SettingLine("Статистика помощи", "${status.helpedUsers} сессий", "Локальная метрика mesh relay.")
            }
        }

        item {
            SettingsPanel("Лаборатория Liberta") {
                LabsSection(
                    title = "Мимикрия под звонки",
                    description = "Автоматически создает комнату звонка и включает бесплатный auto-bridge без ручной ссылки.",
                    enabled = settings.labs.phantomCall,
                    onMasterChange = { checked -> onLabsChange { it.copy(phantomCall = checked) } }
                ) { available ->
                    EnumPills(PhantomTransportService.entries, settings.labs.phantomTransportService, { it.label }, { value ->
                        onLabsChange { it.copy(phantomTransportService = value) }
                    }, enabled = available)
                    EnumPills(PhantomMimicryType.entries, settings.labs.phantomMimicryType, { it.label }, { value ->
                        onLabsChange { it.copy(phantomMimicryType = value) }
                    }, enabled = available)
                    ToggleLine("Автогенерация комнат", "Создает новую встречу для каждой VPN-сессии.", settings.labs.phantomAutoGenerateRooms, { checked ->
                        onLabsChange { it.copy(phantomAutoGenerateRooms = checked) }
                    }, enabled = available)
                    if (!settings.labs.phantomAutoGenerateRooms) {
                        LabsTextField("Своя ссылка комнаты", settings.labs.phantomCustomRoomUrl, available) { value ->
                            onLabsChange { it.copy(phantomCustomRoomUrl = value) }
                        }
                    }
                    EnumPills(PhantomNoiseProfile.entries, settings.labs.phantomCamouflageNoise, { it.label }, { value ->
                        onLabsChange { it.copy(phantomCamouflageNoise = value) }
                    }, enabled = available)
                    StepLine(
                        title = "Длительность сессии",
                        subtitle = "Через сколько минут пересоздавать комнату.",
                        value = "${settings.labs.phantomSessionMinutes} мин",
                        enabled = available,
                        onMinus = { onLabsChange { it.copy(phantomSessionMinutes = it.phantomSessionMinutes - 5) } },
                        onPlus = { onLabsChange { it.copy(phantomSessionMinutes = it.phantomSessionMinutes + 5) } }
                    )
                }

                LabsSection(
                    title = "Нейро-стеганография",
                    description = "Резервное получение конфигов из скрытых данных в изображениях соцсетей.",
                    enabled = settings.labs.socialSteganography,
                    onMasterChange = { checked -> onLabsChange { it.copy(socialSteganography = checked) } }
                ) { available ->
                    ToggleLine("Telegram-каналы", "Мониторинг последних публикаций.", settings.labs.stegoTelegram, { checked -> onLabsChange { it.copy(stegoTelegram = checked) } }, available)
                    ToggleLine("Reddit", "Поиск скрытых ключей в публичных постах.", settings.labs.stegoReddit, { checked -> onLabsChange { it.copy(stegoReddit = checked) } }, available)
                    ToggleLine("Twitter", "Резервный источник для коротких обновлений.", settings.labs.stegoTwitter, { checked -> onLabsChange { it.copy(stegoTwitter = checked) } }, available)
                    ToggleLine("Фото-хостинги", "Сканирование локальных зеркал изображений.", settings.labs.stegoPhotoHosts, { checked -> onLabsChange { it.copy(stegoPhotoHosts = checked) } }, available)
                    StepLine("Глубина сканирования", "Количество последних постов.", settings.labs.stegoScanDepth.toString(), available, { onLabsChange { it.copy(stegoScanDepth = it.stegoScanDepth - 5) } }, { onLabsChange { it.copy(stegoScanDepth = it.stegoScanDepth + 5) } })
                    LensActionButton(
                        onClick = {},
                        enabled = available,
                        selected = false,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(42.dp)
                    ) {
                        Text("Найти обновления", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    LensActionButton(
                        onClick = {},
                        enabled = available,
                        selected = false,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(42.dp)
                    ) {
                        Text("Проверить изображение", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                LabsSection(
                    title = "Полиморфное ядро",
                    description = "Меняет отпечатки TLS и форму пакетов там, где это поддерживает текущий sing-box профиль.",
                    enabled = settings.labs.polymorphicCore,
                    onMasterChange = { checked -> onLabsChange { it.copy(polymorphicCore = checked) } }
                ) { available ->
                    StepLine("Уровень мутации", "0 - стандарт, 3 - максимум.", settings.labs.mutationLevel.toString(), available, { onLabsChange { it.copy(mutationLevel = it.mutationLevel - 1) } }, { onLabsChange { it.copy(mutationLevel = it.mutationLevel + 1) } })
                    EnumPills(TlsFingerprintProfile.entries, settings.labs.tlsFingerprintProfile, { it.label }, { value -> onLabsChange { it.copy(tlsFingerprintProfile = value) } }, available)
                    ToggleLine("Динамический padding", "Скрывает характерные размеры VPN-пакетов.", settings.labs.dynamicPadding, { checked -> onLabsChange { it.copy(dynamicPadding = checked) } }, available)
                    ToggleLine("Формирование джиттера", "Рандомизирует задержки между пакетами.", settings.labs.jitterShaping, { checked -> onLabsChange { it.copy(jitterShaping = checked) } }, available)
                }

                LabsSection(
                    title = "Суверенная меш-сеть",
                    description = "Режим подключения через доступные узлы сообщества и ретрансляции зашифрованного трафика.",
                    enabled = settings.labs.sovereignRelay,
                    onMasterChange = { checked -> onLabsChange { it.copy(sovereignRelay = checked) } }
                ) { available ->
                    EnumPills(RelayRole.entries, settings.labs.relayRole, { it.label }, { value -> onLabsChange { it.copy(relayRole = value) } }, available)
                    ToggleLine("Только на зарядке", "Отключает relay при питании от батареи.", settings.labs.relayOnlyCharging, { checked -> onLabsChange { it.copy(relayOnlyCharging = checked) } }, available)
                    ToggleLine("Только Wi-Fi", "Не тратит мобильный трафик на mesh.", settings.labs.relayWifiOnly, { checked -> onLabsChange { it.copy(relayWifiOnly = checked) } }, available)
                    StepLine("Порог отключения", "Минимальный заряд батареи.", "${settings.labs.relayStopBelowPercent}%", available, { onLabsChange { it.copy(relayStopBelowPercent = it.relayStopBelowPercent - 5) } }, { onLabsChange { it.copy(relayStopBelowPercent = it.relayStopBelowPercent + 5) } })
                    StepLine("Лимит помощи", "Месячный лимит ретрансляции.", "${settings.labs.relayBandwidthGb} ГБ", available, { onLabsChange { it.copy(relayBandwidthGb = it.relayBandwidthGb - 1) } }, { onLabsChange { it.copy(relayBandwidthGb = it.relayBandwidthGb + 1) } })
                }

                LabsSection(
                    title = "Стелс-джиттер и гомеостаз",
                    description = "Адаптирует активность обфускации под экран, перегрев и качество сети.",
                    enabled = settings.labs.homeostasis,
                    onMasterChange = { checked -> onLabsChange { it.copy(homeostasis = checked) } }
                ) { available ->
                    ToggleLine("Адаптивная частота", "Снижает активность при выключенном экране.", settings.labs.adaptiveFrequency, { checked -> onLabsChange { it.copy(adaptiveFrequency = checked) } }, available)
                    ToggleLine("Умное переподключение", "Использует экспоненциальную задержку переподключений.", settings.labs.smartReconnect, { checked -> onLabsChange { it.copy(smartReconnect = checked) } }, available)
                    ToggleLine("Термозащита", "Переходит на легкие алгоритмы при перегреве.", settings.labs.thermalGuard, { checked -> onLabsChange { it.copy(thermalGuard = checked) } }, available)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QrOverlay(onClose: () -> Unit, onShareApp: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCCEEF6FA))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            Modifier
                .fillMaxWidth(0.86f)
                .clickable(enabled = false) {}
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(48.dp))
                    Text(
                        "GitHub Liberta",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    LensIconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = TextPrimary)
                    }
                }
                val qr = remember { makeQr(GithubRepoUrl) }
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "QR GitHub",
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(230.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                LensActionButton(
                    onClick = onShareApp,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть ссылку", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LivingBackground(
    status: VpnStatus,
    meshEnabled: Boolean,
    surgeProgress: Float,
    surgeOrigin: Offset,
    parallax: Offset
) {
    val infinite = rememberInfiniteTransition(label = "livingBackground")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_500), RepeatMode.Reverse),
        label = "drift"
    )

    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    val touchRipple = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchOffset = offset
                    scope.launch {
                        touchRipple.snapTo(0f)
                        touchRipple.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                        touchOffset = null
                    }
                }
            }
    ) {
        drawRect(
            Brush.linearGradient(
                listOf(
                    Color(0xFFF8FBFD),
                    Color(0xFFEAF2F7),
                    Color(0xFFF9FBF8)
                )
            )
        )
        val accent = status.accent()
        val pulse = status.trafficPulse
        val livingTone = if (status.isConnected) 0.5f + 0.5f * sin(drift * 6.28f) else 0f
        val parallaxLarge = Offset(parallax.x * size.width * 0.045f, parallax.y * size.height * 0.030f)
        val parallaxMedium = Offset(parallax.x * size.width * 0.025f, parallax.y * size.height * 0.018f)
        val parallaxSmall = Offset(parallax.x * size.width * 0.012f, parallax.y * size.height * 0.010f)
        val orbAlpha = if (status.isConnected) 0.36f + pulse * 0.22f else 0.13f
        val left = Offset(size.width * (0.18f + drift * 0.14f), size.height * 0.34f) + parallaxLarge
        val right = Offset(size.width * (0.82f - drift * 0.08f), size.height * 0.67f) - parallaxMedium
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    accent.copy(alpha = orbAlpha),
                    Emerald.copy(alpha = livingTone * 0.08f),
                    Color.Transparent
                ),
                left,
                size.maxDimension * 0.52f
            ),
            radius = size.maxDimension * 0.52f,
            center = left
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Gold.copy(alpha = 0.18f), Color.Transparent), right, size.maxDimension * 0.44f),
            radius = size.maxDimension * 0.44f,
            center = right
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.72f), Color.Transparent), Offset(size.width * 0.5f, size.height * 0.5f), size.maxDimension * 0.78f),
            radius = size.maxDimension * 0.78f,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        repeat(9) { index ->
            val y = size.height * (0.16f + index * 0.085f + sin(drift * 6.28f + index) * 0.018f)
            val path = Path().apply {
                moveTo(-size.width * 0.10f + parallaxSmall.x, y + parallaxSmall.y)
                cubicTo(
                    size.width * (0.18f + drift * 0.10f) + parallaxMedium.x,
                    y - size.height * (0.05f + index * 0.002f) + parallaxMedium.y,
                    size.width * (0.62f - drift * 0.08f) - parallaxMedium.x,
                    y + size.height * 0.055f - parallaxMedium.y,
                    size.width * 1.10f - parallaxSmall.x,
                    y - size.height * 0.015f - parallaxSmall.y
                )
            }
            drawPath(
                path = path,
                color = listOf(Azure, Emerald, Gold)[index % 3].copy(alpha = if (status.isConnected) 0.16f + pulse * 0.10f else 0.055f),
                style = Stroke(width = (1.2f + index * 0.10f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
        repeat(10) { index ->
            val phase = drift * 6.28f + index * 0.74f
            val bubble = Offset(
                size.width * (0.10f + (index % 5) * 0.20f + sin(phase) * 0.018f),
                size.height * (0.12f + (index / 5) * 0.46f + cos(phase * 0.8f) * 0.035f)
            ) + if (index % 2 == 0) parallaxMedium else -parallaxSmall
            val radius = (10 + index * 1.7f).dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.26f), Azure.copy(alpha = 0.08f), Color.Transparent),
                    bubble,
                    radius * 1.6f
                ),
                radius = radius,
                center = bubble,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        if (meshEnabled) {
            val edgeAlpha = 0.22f + drift * 0.10f
            drawLine(Gold.copy(alpha = edgeAlpha), Offset(0f, size.height * 0.10f), Offset(0f, size.height * 0.90f), strokeWidth = 3.dp.toPx())
            drawLine(Emerald.copy(alpha = edgeAlpha), Offset(size.width, size.height * 0.12f), Offset(size.width, size.height * 0.88f), strokeWidth = 3.dp.toPx())
            repeat(6) { index ->
                val y = size.height * (0.18f + index * 0.11f + drift * 0.025f)
                drawCircle(Gold.copy(alpha = 0.22f), radius = 2.5.dp.toPx(), center = Offset(size.width * (0.08f + index * 0.14f), y))
            }
        }
        drawDataParticles(status, drift, parallax)

        touchOffset?.let { offset ->
            val rippleRadius = (size.maxDimension * 0.3f * touchRipple.value).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.15f * (1f - touchRipple.value)), Color.Transparent),
                    center = offset,
                    radius = rippleRadius
                ),
                radius = rippleRadius,
                center = offset
            )
        }

        if (surgeProgress < 1f) {
            val origin = if (surgeOrigin == Offset.Zero) Offset(size.width / 2f, size.height / 2f) else surgeOrigin
            val base = size.maxDimension * surgeProgress
            repeat(3) { index ->
                val wobble = 1f + index * 0.13f + drift * 0.04f
                val alpha = (1f - surgeProgress) * (0.40f - index * 0.07f)
                drawCircle(
                    color = listOf(Color.White, accent, Gold)[index].copy(alpha = alpha.coerceAtLeast(0f)),
                    radius = base * wobble,
                    center = origin + Offset(index * 5.dp.toPx(), -index * 3.dp.toPx()),
                    style = Stroke(width = (12 - index * 2).dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataParticles(
    status: VpnStatus,
    phase: Float,
    parallax: Offset
) {
    val active = status.isConnected || status.isBusy
    if (!active && status.trafficPulse < 0.05f) return

    val density = (8 + status.trafficPulse * 18f + if (status.isBusy) 6f else 0f).roundToInt()
    val start = Offset(size.width * 0.50f, size.height * 0.31f) +
        Offset(parallax.x * size.width * 0.020f, parallax.y * size.height * 0.014f)
    val end = Offset(size.width * 0.50f, size.height * 0.72f)
    repeat(density.coerceIn(4, 30)) { index ->
        val localPhase = (phase * (0.75f + status.trafficPulse * 0.75f) + index / density.toFloat()) % 1f
        val wave = sin((localPhase * 6.28f) + index * 1.7f)
        val x = start.x + (end.x - start.x) * localPhase + wave * size.width * 0.035f
        val y = start.y + (end.y - start.y) * localPhase
        val alpha = sin(localPhase * PI.toFloat()).coerceAtLeast(0f) *
            (0.06f + status.trafficPulse * 0.18f + if (status.isBusy) 0.05f else 0f)
        val particleRadius = (1.5f + status.trafficPulse * 2.4f + (index % 3) * 0.55f).dp.toPx()
        val color = listOf(Azure, Emerald, Gold)[index % 3]
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(x, y),
                radius = particleRadius * 4.5f
            ),
            radius = particleRadius * 4.5f,
            center = Offset(x, y)
        )
        drawCircle(color.copy(alpha = alpha * 1.35f), radius = particleRadius, center = Offset(x, y))
    }
}

@Composable
private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun LabsSection(
    title: String,
    description: String,
    enabled: Boolean,
    onMasterChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.68f), RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LensIconButton(onClick = { expanded = !expanded }, selected = enabled) {
                Icon(Icons.Rounded.Info, contentDescription = "Что делает $title", tint = if (enabled) Azure else TextSecondary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Открыть",
                tint = TextPrimary,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .clickable(enabled = false) {}
            ) {
                Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 15.sp)
                ToggleLine("Включить функцию", "Активирует функцию и разблокирует параметры.", enabled, onMasterChange)
                Column(Modifier.alpha(if (enabled) 1f else 0.40f)) {
                    content(enabled)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingLine(label: String, value: String, help: String) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.End)
            Icon(Icons.Rounded.Info, contentDescription = help, tint = Muted, modifier = Modifier.padding(start = 8.dp).size(17.dp))
        }
        Text(help, color = Muted, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ToggleLine(
    label: String,
    help: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).alpha(if (enabled) 1f else 0.42f), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.Info, contentDescription = help, tint = Muted, modifier = Modifier.padding(start = 6.dp).size(16.dp))
            }
            Text(help, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun IntervalSelector(valueMinutes: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val options = listOf(5, 10, 15, 30, 60, 180)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).alpha(if (enabled) 1f else 0.42f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Интервал", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(intervalLabel(valueMinutes), color = TextSecondary, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.take(3).forEach { option ->
                Pill(
                    text = intervalLabel(option),
                    selected = valueMinutes == option,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    onClick = { onChange(option) }
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.drop(3).forEach { option ->
                Pill(
                    text = intervalLabel(option),
                    selected = valueMinutes == option,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    onClick = { onChange(option) }
                )
            }
        }
    }
}

@Composable
private fun StepLine(
    title: String,
    subtitle: String,
    value: String,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).alpha(if (enabled) 1f else 0.42f), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
        }
        LensSmallButton("-", enabled, onMinus)
        Text(value, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
        LensSmallButton("+", enabled, onPlus)
    }
}

@Composable
private fun <T> EnumPills(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (enabled) 1f else 0.42f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    Pill(
                        text = label(value),
                        selected = selected == value,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        onClick = { onSelect(value) }
                    )
                }
                if (rowValues.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LabsTextField(label: String, value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun Pill(text: String, selected: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "pillLens")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_400), RepeatMode.Restart),
        label = "pillLensPhase"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 360f),
        label = "pillPressGlow"
    )
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().alpha(if (enabled) 1f else 0.45f)) {
            drawLensButtonSurface(
                selected = selected,
                enabled = enabled,
                phase = phase,
                cornerRadiusPx = 16.dp.toPx(),
                press = pressGlow
            )
        }
        Text(
            text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun LensActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "actionLens")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_900), RepeatMode.Restart),
        label = "actionLensPhase"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 360f),
        label = "actionPressGlow"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.42f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLensButtonSurface(selected, enabled, phase, 18.dp.toPx(), pressGlow)
        }
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun LensSmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    LensActionButton(
        onClick = onClick,
        enabled = enabled,
        selected = false,
        modifier = Modifier.width(40.dp).height(36.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GlassIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "topLens")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_000), RepeatMode.Restart),
        label = "topLensPhase"
    )
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLensCircleSurface(selected = true, enabled = true, phase = phase)
        }
        content()
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "panelLens")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7_200), RepeatMode.Restart),
        label = "panelLensPhase"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.76f),
                        Glass,
                        Azure.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.46f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(28.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawLensButtonSurface(
                selected = true,
                enabled = true,
                phase = phase,
                cornerRadiusPx = 28.dp.toPx()
            )
        }
        content()
    }
}

private fun statusText(status: VpnStatus): String =
    when (status.phase) {
        ConnectionPhase.CONNECTED -> "Работоспособность подтверждена"
        ConnectionPhase.DEGRADED -> "Работоспособность не подтверждена"
        ConnectionPhase.DISCONNECTED -> "Готов к подключению"
        ConnectionPhase.REFRESHING -> "Обновляю подписки"
        ConnectionPhase.RACING -> status.message
        ConnectionPhase.CONNECTING -> status.message
        ConnectionPhase.RECOVERING -> "Восстанавливаю туннель"
        ConnectionPhase.ERROR -> "Нужна проверка"
    }

private fun statusDetail(status: VpnStatus): String {
    return when {
        status.error != null -> status.error
        status.isConnected -> "Интернет проверен через защищенный туннель"
        status.lastUpdatedEpochMs != null -> "Последнее обновление ${status.lastUpdatedEpochMs.formatTime()}"
        else -> "Выберите режим и коснитесь центральной линзы"
    }
}

private fun VpnStatus.accent(): Color =
    when (phase) {
        ConnectionPhase.CONNECTED -> Emerald
        ConnectionPhase.DEGRADED -> Amber
        ConnectionPhase.ERROR -> Rose
        ConnectionPhase.DISCONNECTED -> Gold
        else -> Azure
    }

private fun Long.formatTime(): String =
    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(this))

private fun intervalLabel(minutes: Int): String =
    if (minutes < 60) "${minutes} мин" else "${minutes / 60} ч"

private fun makeQr(content: String): Bitmap {
    val size = 720
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.MARGIN, 1)
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
    }
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.rgb(19, 44, 59) else android.graphics.Color.rgb(247, 250, 252)
            )
        }
    }
    return bitmap
}
