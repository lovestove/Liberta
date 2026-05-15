package com.liberta.vpn.ui

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
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
private val DiffractionA = Color(0x66A5D9FF)
private val DiffractionB = Color(0x55F4C87D)
private val DiffractionC = Color(0x55A7F2CF)

@Composable
private fun rememberDeviceParallax(): Offset {
    val context = LocalContext.current
    var offset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            onDispose {}
        } else {
            val listener = object : SensorEventListener {
                private var x = 0f
                private var y = 0f
                private val rotationMatrix = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    val (nextX, nextY) = if (
                        event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                        event.sensor.type == Sensor.TYPE_ROTATION_VECTOR
                    ) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val roll = orientation[2]
                        val pitch = orientation[1]
                        (roll / 0.50f).coerceIn(-1f, 1f) to (-pitch / 0.62f).coerceIn(-1f, 1f)
                    } else {
                        (-event.values[0] / 7.5f).coerceIn(-1f, 1f) to
                            (event.values[1] / 7.5f).coerceIn(-1f, 1f)
                    }
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

private fun performDensePressHaptic(
    view: android.view.View,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val handled = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    if (!handled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
}

private fun performSoftPulseHaptic(
    view: android.view.View,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val handled = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    if (!handled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

@Composable
private fun AmbientAudioLandscape(status: VpnStatus, enabled: Boolean) {
    val latestStatus by rememberUpdatedState(status)

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sampleRate = 16_000
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(sampleRate / 2, minBuffer.takeIf { it > 0 } ?: sampleRate / 2)
            val buffer = ShortArray(bufferSize / 2)
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@withContext

            var phase = 0f
            var noise = 0x1D2C3B4A
            runCatching {
                track.setVolume(0f)
                track.play()
                while (currentCoroutineContext().isActive) {
                    val current = latestStatus
                    val active = current.isConnected || current.isBusy
                    val latency = current.activeServer?.latencyMs?.coerceIn(40L, 420L) ?: 96L
                    val load = current.trafficPulse.coerceIn(0f, 1f)
                    val targetVolume = when {
                        !active -> 0f
                        current.phase == ConnectionPhase.ERROR -> 0.010f
                        else -> 0.006f + load * 0.010f
                    }
                    val baseFrequency = when (current.phase) {
                        ConnectionPhase.CONNECTED -> 92f + (420L - latency).toFloat() * 0.09f + load * 34f
                        ConnectionPhase.ERROR -> 64f + sin(phase * 0.031f) * 10f
                        else -> 132f + load * 26f
                    }
                    track.setVolume(targetVolume)
                    for (index in buffer.indices) {
                        noise = noise * 1_664_525 + 1_013_904_223
                        val hiss = (((noise ushr 16) and 0xffff) / 32768f) - 1f
                        val hum = sin(phase) * 0.64f + sin(phase * 2.01f + 0.7f) * 0.18f
                        buffer[index] = ((hum + hiss * 0.11f) * 1800f).roundToInt().toShort()
                        phase += (2f * PI.toFloat() * baseFrequency) / sampleRate
                        if (phase > 2f * PI.toFloat()) phase -= 2f * PI.toFloat()
                    }
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            }
            track.release()
        }
    }
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
    var fusionKey by remember { mutableStateOf(0) }
    var fusionOrigin by remember { mutableStateOf(Offset.Zero) }
    var fusionAccent by remember { mutableStateOf(Azure) }
    var touchKey by remember { mutableStateOf(0) }
    var touchOrigin by remember { mutableStateOf(Offset.Zero) }
    val surge = remember { Animatable(1f) }
    val fusion = remember { Animatable(1f) }
    val touch = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 28) }.getOrNull()
    }
    val parallax = rememberDeviceParallax()
    AmbientAudioLandscape(status, settings.labs.homeostasis)

    LaunchedEffect(surgeKey) {
        if (surgeKey > 0) {
            surge.snapTo(0f)
            surge.animateTo(1f, tween(1_360, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(fusionKey) {
        if (fusionKey > 0) {
            fusion.snapTo(0f)
            fusion.animateTo(1f, tween(860, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(touchKey) {
        if (touchKey > 0) {
            touch.snapTo(0f)
            touch.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
        }
    }

    DisposableEffect(tone) {
        onDispose { tone?.release() }
    }

    LaunchedEffect(status.phase) {
        if (status.phase == ConnectionPhase.CONNECTED) {
            performSoftPulseHaptic(view, haptics)
            tone?.startTone(ToneGenerator.TONE_PROP_ACK, 64)
            delay(96)
            performSoftPulseHaptic(view, haptics)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 42)
        } else if (status.phase == ConnectionPhase.ERROR) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    MaterialTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                            if (down != null) {
                                touchOrigin = down.position
                                touchKey += 1
                            }
                        }
                    }
                }
        ) {
            LivingBackground(
                status = status,
                meshEnabled = settings.labs.sovereignRelay,
                surgeProgress = surge.value,
                surgeOrigin = surgeOrigin,
                parallax = parallax,
                touchProgress = touch.value,
                touchOrigin = touchOrigin
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
                            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 46)
                            scope.launch {
                                delay(130)
                                onPower()
                            }
                        },
                        onConnectionModePower = { method, origin ->
                            fusionOrigin = origin
                            fusionAccent = method.fluxAccent()
                            fusionKey += 1
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onConnectionModePower(method)
                        },
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

            FusionDropOverlay(
                progress = fusion.value,
                origin = fusionOrigin,
                accent = fusionAccent
            )

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
    onConnectionModePower: (ConnectionMethod, Offset) -> Unit,
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
            onFusionDrop = onFusionDrop,
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
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
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
        val ribbonShape = RoundedCornerShape(999.dp)
        Box(
            Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = ribbonShape,
                    clip = false,
                    ambientColor = Color(0xFF132C3B),
                    spotColor = Color(0xFF132C3B)
                )
                .clip(ribbonShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.62f),
                            Emerald.copy(alpha = 0.28f),
                            Azure.copy(alpha = 0.24f),
                            Color.White.copy(alpha = 0.50f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.86f), ribbonShape)
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
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
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
    val flowSpeed by animateFloatAsState(
        targetValue = when (status.phase) {
            ConnectionPhase.RACING -> 3.4f
            ConnectionPhase.CONNECTING -> 2.6f
            ConnectionPhase.RECOVERING -> 2.0f
            ConnectionPhase.REFRESHING -> 1.5f
            else -> 1.0f
        },
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "lensFlowSpeed"
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
                detectTapGestures { local ->
                    performDensePressHaptic(view, haptics)
                    onPower(rootTopLeft + local)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.38f * scale
            val active = status.isConnected || status.isBusy
            val accent = status.accent()
            val turbulence = status.networkTurbulence()
            val flowSpeed = status.networkFlowSpeed()

            drawPowerBloom(center, radius, status.trafficPulse, accent, active)
            with(DiffractionLensShader) {
                drawDistortionField(center, radius * 1.13f, flow * 6.28f, active, status.trafficPulse)
                drawLens(center, radius, flow * 6.28f, active, status.trafficPulse)
            }
            drawInternalFluidDynamics(center, radius, flow, active, status.trafficPulse, turbulence, accent)
            drawRefractionCaustics(center, radius, flow, active, status.trafficPulse, turbulence, flowSpeed)
            drawLensFieldArcs(center, radius, flow, active, status.trafficPulse, turbulence, accent)
            drawCircle(
                color = Color.White.copy(alpha = 0.56f),
                radius = radius * 1.005f,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )
            drawCircle(
                color = Gold.copy(alpha = 0.34f + status.trafficPulse * 0.20f),
                radius = radius * (0.94f + breathe * 0.04f),
                center = center,
                style = Stroke(width = (1.4.dp + 2.dp * status.trafficPulse).toPx(), cap = StrokeCap.Round)
            )
            drawDiffractionRim(center, radius, flow, active, status.trafficPulse, accent)
            with(DiffractionLensShader) {
                drawPowerGlyph(center, radius * 0.27f, flow * 6.28f, active, status.trafficPulse)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPowerBloom(
    center: Offset,
    radius: Float,
    traffic: Float,
    accent: Color,
    active: Boolean
) {
    val bloom = if (active) 0.10f + traffic * 0.20f else 0.05f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = bloom * 0.72f),
                accent.copy(alpha = bloom),
                Gold.copy(alpha = traffic * 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = radius * (1.85f + traffic * 0.34f)
        ),
        radius = radius * (1.85f + traffic * 0.34f),
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInternalFluidDynamics(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float,
    turbulence: Float,
    accent: Color
) {
    val laminar = 1f - turbulence
    val lines = (6 + traffic * 8f + turbulence * 5f).roundToInt().coerceIn(6, 18)
    repeat(lines) { index ->
        val lane = (index / (lines - 1f)).coerceIn(0f, 1f)
        val angle = -0.44f + lane * 0.88f
        val x0 = center.x - radius * (0.68f - traffic * 0.10f)
        val x3 = center.x + radius * (0.66f + traffic * 0.18f)
        val y = center.y + sin(angle) * radius * 0.50f
        val boil = sin(phase * 6.28f * (1.2f + turbulence) + index * 1.37f)
        val bend = radius * (0.08f + turbulence * 0.18f) * boil
        val path = Path().apply {
            moveTo(x0, y - bend * 0.20f)
            cubicTo(
                center.x - radius * (0.26f + laminar * 0.10f),
                y - radius * 0.10f + bend,
                center.x + radius * (0.25f + traffic * 0.10f),
                y + radius * 0.10f - bend,
                x3,
                y + bend * 0.18f
            )
        }
        drawPath(
            path = path,
            color = listOf(Azure, Emerald, Gold, accent)[index % 4]
                .copy(alpha = (0.06f + traffic * 0.12f + laminar * 0.035f) * if (active) 1f else 0.55f),
            style = Stroke(
                width = (0.70f + traffic * 1.20f + turbulence * 0.55f).dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }

    val eddies = (2 + turbulence * 8f + traffic * 3f).roundToInt().coerceIn(2, 12)
    repeat(eddies) { index ->
        val angle = phase * 6.28f * (0.8f + turbulence) + index * 2.03f
        val p = Offset(
            center.x + cos(angle) * radius * (0.20f + (index % 4) * 0.10f),
            center.y + sin(angle * 0.83f) * radius * (0.18f + (index % 3) * 0.12f)
        )
        drawCircle(
            color = Color.White.copy(alpha = (0.06f + turbulence * 0.11f + traffic * 0.07f) * if (active) 1f else 0.45f),
            radius = radius * (0.018f + (index % 3) * 0.006f),
            center = p,
            style = Stroke(width = (0.6f + turbulence).dp.toPx())
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRefractionCaustics(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float,
    turbulence: Float,
    flowSpeed: Float
) {
    val streamAlpha = if (active) 0.24f + traffic * 0.18f else 0.12f
    repeat(9) { index ->
        val wobble = sin(phase * 6.28f * (1.4f + turbulence) + index * 1.31f) * turbulence
        val angle = (phase * 360f * (0.62f + flowSpeed * 0.86f) + index * 43f + wobble * 22f) * (PI.toFloat() / 180f)
        val start = Offset(
            center.x + cos(angle) * radius * (0.50f + index * 0.014f),
            center.y + sin(angle) * radius * (0.40f + index * 0.018f)
        )
        val c1 = Offset(
            center.x + cos(angle + 0.82f + wobble * 0.22f) * radius * (1.04f + traffic * 0.14f + turbulence * 0.10f),
            center.y + sin(angle + 0.42f - wobble * 0.18f) * radius * (0.80f + traffic * 0.09f + turbulence * 0.08f)
        )
        val end = Offset(
            center.x + cos(angle + 1.42f) * radius * (1.28f + traffic * 0.22f + flowSpeed * 0.08f),
            center.y + sin(angle + 1.06f + wobble * 0.24f) * radius * (1.00f + traffic * 0.15f + turbulence * 0.10f)
        )
        val baseColor = listOf(Azure, Emerald, Gold)[index % 3]
        val split = radius * (0.004f + traffic * 0.004f + turbulence * 0.003f)
        listOf(
            Triple(Offset(-split, 0f), Azure, 0.55f),
            Triple(Offset(split * 0.55f, split * 0.35f), Gold, 0.42f),
            Triple(Offset.Zero, baseColor, 1f)
        ).forEach { (shift, color, weight) ->
            val path = Path().apply {
                moveTo(start.x + shift.x, start.y + shift.y)
                quadraticTo(c1.x + shift.x, c1.y + shift.y, end.x + shift.x, end.y + shift.y)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = streamAlpha * weight),
                style = Stroke(width = (0.70f + index * 0.07f + traffic * 0.55f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGroundShadow(center: Offset, radius: Float) {
    val shadowCenter = Offset(center.x + radius * 0.05f, center.y + radius * 0.62f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF132C3B).copy(alpha = 0.16f),
                Color(0xFF132C3B).copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = shadowCenter,
            radius = radius * 1.30f
        ),
        radius = radius * 1.30f,
        center = shadowCenter
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOuterHalo(
    center: Offset,
    radius: Float,
    accent: Color,
    active: Boolean,
    traffic: Float,
    breathe: Float
) {
    val haloAlpha = if (active) 0.32f + traffic * 0.22f else 0.16f + breathe * 0.06f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = haloAlpha),
                accent.copy(alpha = haloAlpha * 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.78f
        ),
        radius = radius * 1.78f,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.36f
        ),
        radius = radius * 1.36f,
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSphereBody(
    center: Offset,
    radius: Float,
    active: Boolean,
    traffic: Float,
    accent: Color
) {
    val highlightCenter = Offset(center.x - radius * 0.30f, center.y - radius * 0.36f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF7FCFF).copy(alpha = 0.96f),
                Color(0xFFE0EEF7).copy(alpha = 0.86f),
                Color(0xFFB6CFE0).copy(alpha = 0.74f),
                Color(0xFF6E8FA6).copy(alpha = 0.62f),
                Color(0xFF3F5C77).copy(alpha = 0.48f)
            ),
            stops = listOf(0f, 0.30f, 0.60f, 0.86f, 1f),
            center = highlightCenter,
            radius = radius * 1.08f
        ),
        radius = radius,
        center = center
    )
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.20f + traffic * 0.16f),
                    accent.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.96f
            ),
            radius = radius,
            center = center
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSphereBottomShadow(center: Offset, radius: Float) {
    val shadowCenter = Offset(center.x + radius * 0.32f, center.y + radius * 0.40f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0E2334).copy(alpha = 0.32f),
                Color(0xFF1F3A52).copy(alpha = 0.16f),
                Color.Transparent
            ),
            center = shadowCenter,
            radius = radius * 0.94f
        ),
        radius = radius,
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSphereSpecularHighlight(center: Offset, radius: Float) {
    val hp = Offset(center.x - radius * 0.34f, center.y - radius * 0.46f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.36f),
                Color.Transparent
            ),
            center = hp,
            radius = radius * 0.34f
        ),
        radius = radius * 0.34f,
        center = hp
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = radius * 0.055f,
        center = Offset(center.x - radius * 0.42f, center.y - radius * 0.52f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.36f),
                Color.Transparent
            ),
            center = Offset(center.x + radius * 0.18f, center.y - radius * 0.04f),
            radius = radius * 0.18f
        ),
        radius = radius * 0.18f,
        center = Offset(center.x + radius * 0.18f, center.y - radius * 0.04f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFresnelRim(
    center: Offset,
    radius: Float,
    active: Boolean,
    traffic: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.62f + traffic * 0.18f),
                Color.White.copy(alpha = 0.10f)
            ),
            stops = listOf(0f, 0.84f, 0.96f, 1f),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = if (active) 0.88f else 0.66f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF132C3B).copy(alpha = 0.22f),
        radius = radius * 0.985f,
        center = center,
        style = Stroke(width = 0.6.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDispersionRing(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean
) {
    val baseAlpha = if (active) 0.52f else 0.34f
    val colors = listOf(
        Color(0xFF77B8FF),
        Color(0xFFA9F1FF),
        Color(0xFFFFFFFF),
        Color(0xFFFFE6A6),
        Color(0xFF5FE2B3)
    )
    colors.forEachIndexed { index, color ->
        val ringR = radius * (0.882f + index * 0.018f)
        drawCircle(
            color = color.copy(alpha = baseAlpha * (1f - index * 0.12f)),
            radius = ringR,
            center = center,
            style = Stroke(width = (0.7f + index * 0.05f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    repeat(7) { index ->
        val angle = (phase * 360f + index * 51.4f) * (PI.toFloat() / 180f)
        val ringR = radius * 0.985f
        val tip = Offset(
            center.x + cos(angle) * ringR,
            center.y + sin(angle) * ringR
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = (1.6f + sin(phase * 6.28f + index) * 0.6f).dp.toPx().coerceAtLeast(0.8f),
            center = tip
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLensFieldArcs(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float,
    turbulence: Float,
    accent: Color
) {
    val fieldAlpha = if (active) 0.20f + traffic * 0.14f else 0.10f
    repeat(9) { index ->
        val ring = radius * (0.82f + index * 0.055f + sin(phase * 6.28f + index) * turbulence * 0.014f)
        val start = phase * 360f * (0.86f + traffic * 0.42f) + index * 39f + turbulence * sin(index + phase * 6.28f) * 18f
        drawArc(
            color = listOf(Azure, Emerald, Gold, accent)[index % 4].copy(alpha = fieldAlpha * (1f - index * 0.055f)),
            startAngle = start,
            sweepAngle = 18f + index * 2.8f + traffic * 16f - turbulence * 4f,
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiffractionRim(
    center: Offset,
    radius: Float,
    phase: Float,
    active: Boolean,
    traffic: Float,
    accent: Color
) {
    val alpha = (if (active) 0.22f else 0.12f) + traffic * 0.16f
    val colors = listOf(Azure, Emerald, Gold)
    repeat(3) { index ->
        val shift = (index - 1) * radius * 0.006f
        drawArc(
            color = colors[index].copy(alpha = alpha * (0.72f - index * 0.08f)),
            startAngle = phase * 360f + index * 21f,
            sweepAngle = 236f + traffic * 40f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.018f + shift, center.y - radius * 1.018f - shift),
            size = Size(radius * 2.036f, radius * 2.036f),
            style = Stroke(width = (0.70f + traffic * 0.80f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawCircle(
        color = accent.copy(alpha = alpha * 0.45f),
        radius = radius * (1.055f + traffic * 0.025f),
        center = center,
        style = Stroke(width = (0.55f + traffic * 0.55f).dp.toPx())
    )
}

@Composable
private fun FloatingControlDock(
    settings: LibertaSettings,
    status: VpnStatus,
    onSettingsChange: ((LibertaSettings) -> LibertaSettings) -> Unit,
    onLabsChange: ((LabSettings) -> LabSettings) -> Unit,
    onConnectionModePower: (ConnectionMethod, Offset) -> Unit,
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
                    onClick = { origin ->
                        onSettingsChange { current ->
                            current.copy(
                                connectionMethod = mode.method,
                                profile = mode.method.profile
                            )
                        }
                        onConnectionModePower(mode.method, origin)
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
    onClick: (Offset) -> Unit,
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
    var magnetSpot by remember { mutableStateOf<Offset?>(null) }
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else if (magnetSpot != null) 0.62f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 360f),
        label = "dockPressGlow"
    )
    val alpha = if (enabled) 1f else 0.52f
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "dockScale"
    )
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .onGloballyPositioned { coordinates ->
                centerInRoot = coordinates.localToRoot(
                    Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 6.dp,
                shape = dockShape,
                clip = false,
                ambientColor = Color(0xFF132C3B),
                spotColor = Color(0xFF132C3B)
            )
            .clip(dockShape)
            .onGloballyPositioned { coords ->
                val topLeft = coords.localToRoot(Offset.Zero)
                rootCenter = Offset(
                    topLeft.x + coords.size.width / 2f,
                    topLeft.y + coords.size.height / 2f
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        magnetSpot = when {
                            change?.pressed == true -> change.position
                            event.type == PointerEventType.Move || event.type == PointerEventType.Enter -> change?.position
                            event.type == PointerEventType.Release || event.type == PointerEventType.Exit -> null
                            else -> magnetSpot
                        }
                    }
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick(centerInRoot)
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
                press = pressGlow,
                magnetSpot = magnetSpot
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
private fun FusionDropOverlay(progress: Float, origin: Offset, accent: Color) {
    if (progress >= 1f || origin == Offset.Zero) return
    Canvas(Modifier.fillMaxSize()) {
        val target = Offset(size.width * 0.50f, size.height * 0.30f)
        val eased = progress * progress * (3f - 2f * progress)
        val control = Offset(
            (origin.x + target.x) * 0.5f,
            minOf(origin.y, target.y) - size.height * 0.10f
        )
        val oneMinus = 1f - eased
        val current = Offset(
            oneMinus * oneMinus * origin.x + 2f * oneMinus * eased * control.x + eased * eased * target.x,
            oneMinus * oneMinus * origin.y + 2f * oneMinus * eased * control.y + eased * eased * target.y
        )
        val radius = (18.dp.toPx() * (1f - eased * 0.44f)).coerceAtLeast(7.dp.toPx())
        val bridgeAlpha = (1f - progress).coerceIn(0f, 1f) * 0.28f
        val bridge = Path().apply {
            moveTo(origin.x, origin.y)
            quadraticTo(control.x, control.y, current.x, current.y)
        }
        drawPath(
            path = bridge,
            color = accent.copy(alpha = bridgeAlpha),
            style = Stroke(width = (9.dp.toPx() * (1f - progress * 0.55f)).coerceAtLeast(2.dp.toPx()), cap = StrokeCap.Round)
        )
        drawPath(
            path = bridge,
            color = Color.White.copy(alpha = bridgeAlpha * 1.25f),
            style = Stroke(width = (3.dp.toPx() * (1f - progress * 0.40f)).coerceAtLeast(1.dp.toPx()), cap = StrokeCap.Round)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = 0.78f * (1f - progress * 0.18f)),
                    accent.copy(alpha = 0.48f),
                    Gold.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = current,
                radius = radius * 2.8f
            ),
            radius = radius * 2.8f,
            center = current
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.72f * (1f - progress)),
            radius = radius,
            center = current,
            style = Stroke(width = 1.2.dp.toPx())
        )
        if (progress > 0.70f) {
            val merge = ((progress - 0.70f) / 0.30f).coerceIn(0f, 1f)
            drawCircle(
                color = accent.copy(alpha = (1f - merge) * 0.22f),
                radius = radius * (2.0f + merge * 4.8f),
                center = target,
                style = Stroke(width = (4.dp.toPx() * (1f - merge)).coerceAtLeast(0.5.dp.toPx()))
            )
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
    press: Float = 0f,
    magnetSpot: Offset? = null
) {
    val active = selected && enabled
    val cornerR = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    val magneticCenter = magnetSpot?.let {
        Offset(
            it.x.coerceIn(size.width * 0.08f, size.width * 0.92f),
            it.y.coerceIn(size.height * 0.12f, size.height * 0.88f)
        )
    }
    val highlightCenter = magneticCenter?.let { it + Offset(-size.width * 0.06f, -size.height * 0.18f) }
        ?: Offset(size.width * 0.28f, size.height * 0.18f)

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.62f + press * 0.18f),
                Color.White.copy(alpha = 0.42f + press * 0.10f),
                Color(0xFFE6F1FA).copy(alpha = 0.34f),
                Color(0xFFC9DCEC).copy(alpha = 0.30f)
            ),
            start = Offset(size.width * 0.1f, 0f),
            end = Offset(size.width * 0.9f, size.height)
        ),
        cornerRadius = cornerR
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.66f + press * 0.18f),
                Color.White.copy(alpha = 0.20f),
                Color.Transparent
            ),
            center = highlightCenter,
            radius = size.maxDimension * 0.86f
        ),
        cornerRadius = cornerR
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f + press * 0.18f),
                Color.White.copy(alpha = 0.16f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.55f
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height * 0.55f),
        cornerRadius = cornerR
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF14283A).copy(alpha = 0.10f)
            ),
            startY = size.height * 0.55f,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height * 0.55f),
        size = Size(size.width, size.height * 0.45f),
        cornerRadius = CornerRadius(cornerRadiusPx * 0.6f, cornerRadiusPx * 0.6f)
    )
    if (active) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Azure.copy(alpha = 0.22f + press * 0.10f),
                    Azure.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.maxDimension * 0.78f
            ),
            cornerRadius = cornerR
        )
    }
    magneticCenter?.let { spot ->
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f * press),
                    Azure.copy(alpha = 0.18f * press),
                    Color.Transparent
                ),
                center = spot,
                radius = size.minDimension * 1.05f
            ),
            radius = size.minDimension * 1.05f,
            center = spot
        )
    }
    repeat(4) { index ->
        val y = size.height * (0.22f + index * 0.20f + sin(phase * 6.28f + index) * 0.022f)
        val pullX = magneticCenter?.let { (it.x - size.width / 2f) * 0.12f } ?: 0f
        val pullY = magneticCenter?.let { (it.y - size.height / 2f) * 0.04f } ?: 0f
        val path = Path().apply {
            moveTo(size.width * -0.06f, y + pullY)
            cubicTo(
                size.width * (0.26f + phase * 0.06f) + pullX,
                y - size.height * (0.14f + press * 0.04f),
                size.width * (0.62f - phase * 0.05f) + pullX * 1.4f,
                y + size.height * (0.14f + press * 0.04f),
                size.width * 1.06f,
                y - size.height * 0.03f
            )
        }
        drawPath(
            path = path,
            color = listOf(Azure, Emerald, Gold)[index % 3].copy(
                alpha = (if (active) 0.22f else 0.10f) + press * 0.10f
            ),
            style = Stroke(width = (0.7f + index * 0.07f + press * 0.55f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = 0.85f + press * 0.10f),
        radius = (1.6f + size.minDimension * 0.012f).coerceAtMost(size.minDimension * 0.04f),
        center = Offset(size.width * 0.18f, size.height * 0.22f)
    )
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color(0xFF77B8FF).copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.92f),
                Color(0xFFFFD86F).copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.85f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(0.6.dp.toPx(), 0.6.dp.toPx()),
        size = Size(size.width - 1.2.dp.toPx(), size.height - 1.2.dp.toPx()),
        style = Stroke(width = 1.4.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = if (active) 0.55f else 0.34f),
        topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
        size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
        cornerRadius = CornerRadius(cornerRadiusPx * 0.86f, cornerRadiusPx * 0.86f),
        style = Stroke(width = 0.6.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF132C3B).copy(alpha = 0.18f),
        topLeft = Offset(0.4.dp.toPx(), 0.4.dp.toPx()),
        size = Size(size.width - 0.8.dp.toPx(), size.height - 0.8.dp.toPx()),
        cornerRadius = cornerR,
        style = Stroke(width = 0.5.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLensCircleSurface(
    selected: Boolean,
    enabled: Boolean,
    phase: Float
) {
    val active = selected && enabled
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f - 0.6.dp.toPx()
    val highlightCenter = Offset(center.x - radius * 0.30f, center.y - radius * 0.36f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF7FCFF).copy(alpha = if (active) 0.92f else 0.78f),
                Color(0xFFE0EEF7).copy(alpha = if (active) 0.78f else 0.62f),
                Color(0xFFB0CADC).copy(alpha = 0.62f),
                Color(0xFF6E8FA6).copy(alpha = 0.50f)
            ),
            stops = listOf(0f, 0.40f, 0.78f, 1f),
            center = highlightCenter,
            radius = radius * 1.10f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0E2334).copy(alpha = 0.30f),
                Color(0xFF1F3A52).copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(center.x + radius * 0.28f, center.y + radius * 0.36f),
            radius = radius * 0.92f
        ),
        radius = radius,
        center = center
    )
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Azure.copy(alpha = 0.24f),
                    Azure.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.92f
            ),
            radius = radius,
            center = center
        )
    }
    repeat(3) { index ->
        drawArc(
            color = listOf(Azure, Gold, Emerald)[index].copy(alpha = if (active) 0.34f else 0.16f),
            startAngle = phase * 360f + index * 94f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.84f, center.y - radius * 0.84f),
            size = Size(radius * 1.68f, radius * 1.68f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.30f),
                Color.Transparent
            ),
            center = Offset(center.x - radius * 0.36f, center.y - radius * 0.46f),
            radius = radius * 0.36f
        ),
        radius = radius * 0.36f,
        center = Offset(center.x - radius * 0.36f, center.y - radius * 0.46f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = (radius * 0.06f).coerceAtMost(2.4.dp.toPx()),
        center = Offset(center.x - radius * 0.44f, center.y - radius * 0.50f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = if (active) 0.78f else 0.56f),
                Color.White.copy(alpha = 0.10f)
            ),
            stops = listOf(0f, 0.84f, 0.96f, 1f),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = if (active) 0.92f else 0.72f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.1.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF132C3B).copy(alpha = 0.22f),
        radius = radius * 0.985f,
        center = center,
        style = Stroke(width = 0.5.dp.toPx())
    )
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

private fun ConnectionMethod.fusionTint(): Color =
    when (this) {
        ConnectionMethod.BLACKLISTS -> Azure
        ConnectionMethod.WHITELISTS -> Gold
        ConnectionMethod.PHANTOM_CALL -> Rose
        ConnectionMethod.MESH_ACCESS -> Emerald
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
            LabsPanel("Лаборатория Liberta") {
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
                    description = "Адаптирует активность обфускации, дыхание интерфейса и тихий аудио-гул под качество сети.",
                    enabled = settings.labs.homeostasis,
                    onMasterChange = { checked -> onLabsChange { it.copy(homeostasis = checked) } }
                ) { available ->
                    SettingLine("Аудио-ландшафт", "Низкий гул зависит от ping и нагрузки", "Активен только при гомеостазе.")
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
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.82f),
                        Azure.copy(alpha = 0.22f),
                        Color(0xDDEAF3F8)
                    ),
                    radius = 1200f
                )
            )
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
                        .border(
                            1.2.dp,
                            Brush.linearGradient(listOf(DiffractionA, DiffractionB, DiffractionC, Color.White.copy(alpha = 0.8f))),
                            RoundedCornerShape(18.dp)
                        )
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
    parallax: Offset,
    touchProgress: Float,
    touchOrigin: Offset
) {
    val infinite = rememberInfiniteTransition(label = "livingBackground")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_500), RepeatMode.Reverse),
        label = "drift"
    )

    Canvas(
        Modifier.fillMaxSize()
    ) {
        val accent = status.accent()
        val pulse = status.trafficPulse
        val livingTone = if (status.isConnected) 0.5f + 0.5f * sin(drift * 6.28f) else 0f
        val errorBeat = status.errorBeat(drift)
        val ambientMiddle = when (status.phase) {
            ConnectionPhase.CONNECTED -> lerp(Azure, Emerald, livingTone * 0.72f).copy(alpha = 0.26f)
            ConnectionPhase.ERROR -> lerp(Color(0xFFF4E7EA), Rose, errorBeat * 0.34f)
            else -> Color(0xFFEAF2F7)
        }
        val ambientTail = when (status.phase) {
            ConnectionPhase.CONNECTED -> lerp(Color(0xFFF9FBF8), Color(0xFFE7FFF6), livingTone)
            ConnectionPhase.ERROR -> lerp(Color(0xFFFFF7F7), Color(0xFFFFE3E7), errorBeat)
            else -> Color(0xFFF9FBF8)
        }
        drawRect(
            Brush.linearGradient(
                listOf(
                    Color(0xFFF8FBFD),
                    ambientMiddle,
                    ambientTail
                )
            )
        )
        val parallaxLarge = Offset(parallax.x * size.width * 0.045f, parallax.y * size.height * 0.030f)
        val parallaxMedium = Offset(parallax.x * size.width * 0.025f, parallax.y * size.height * 0.018f)
        val parallaxSmall = Offset(parallax.x * size.width * 0.012f, parallax.y * size.height * 0.010f)
        val orbAlpha = when {
            status.isConnected -> 0.36f + pulse * 0.22f + livingTone * 0.06f
            status.phase == ConnectionPhase.ERROR -> 0.20f + errorBeat * 0.22f
            else -> 0.13f
        }
        val left = Offset(size.width * (0.18f + drift * 0.14f), size.height * 0.34f) + parallaxLarge
        val right = Offset(size.width * (0.82f - drift * 0.08f), size.height * 0.67f) - parallaxMedium
        drawEnvironmentCaustics(status, drift, parallax)
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
            brush = Brush.radialGradient(
                listOf(
                    lerp(Gold, Emerald, livingTone).copy(alpha = 0.18f + livingTone * 0.08f),
                    Color.Transparent
                ),
                right,
                size.maxDimension * 0.44f
            ),
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
                color = listOf(Azure, Emerald, Gold, Rose)[index % 4].copy(
                    alpha = when {
                        status.phase == ConnectionPhase.ERROR -> 0.06f + errorBeat * 0.18f
                        status.isConnected -> 0.16f + pulse * 0.10f
                        else -> 0.055f
                    }
                ),
                style = Stroke(
                    width = (1.2f + index * 0.10f + if (status.phase == ConnectionPhase.ERROR) errorBeat * 1.2f else 0f).dp.toPx(),
                    cap = StrokeCap.Round
                )
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

        if (touchProgress < 1f && touchOrigin != Offset.Zero) {
            drawTactileRefractionWave(touchOrigin, touchProgress, accent)
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

        if (fusionProgress < 1f) {
            val origin = if (fusionOrigin == Offset.Zero) Offset(size.width / 2f, size.height * 0.86f) else fusionOrigin
            val maxRadius = size.maxDimension * 1.18f
            val curRadius = maxRadius * fusionProgress
            val fade = (1f - fusionProgress).coerceAtLeast(0f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        fusionTint.copy(alpha = 0.32f * fade),
                        fusionTint.copy(alpha = 0.16f * fade),
                        Color.Transparent
                    ),
                    center = origin,
                    radius = curRadius
                ),
                radius = curRadius,
                center = origin
            )
            drawCircle(
                color = fusionTint.copy(alpha = 0.30f * fade),
                radius = curRadius * 0.92f,
                center = origin,
                style = Stroke(width = (4.dp.toPx() * (1f - fusionProgress * 0.55f)).coerceAtLeast(0.6f), cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f * fade),
                radius = curRadius * 0.68f,
                center = origin,
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnvironmentCaustics(
    status: VpnStatus,
    phase: Float,
    parallax: Offset
) {
    val active = status.isConnected || status.isBusy
    val pulse = status.trafficPulse
    val turbulence = status.networkTurbulence()
    val lensShadow = Offset(
        size.width * 0.50f + parallax.x * size.width * 0.040f,
        size.height * 0.50f + parallax.y * size.height * 0.030f
    )
    val shadowSize = Size(size.width * 0.62f, size.height * 0.21f)
    drawOval(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.25f + pulse * 0.13f),
                Azure.copy(alpha = if (active) 0.080f + pulse * 0.050f else 0.035f),
                Gold.copy(alpha = pulse * 0.045f),
                Color.Transparent
            ),
            center = lensShadow,
            radius = size.width * 0.48f
        ),
        topLeft = Offset(lensShadow.x - shadowSize.width / 2f, lensShadow.y - shadowSize.height / 2f),
        size = shadowSize
    )

    val alpha = if (active) 0.13f + pulse * 0.13f else 0.045f
    repeat(7) { index ->
        val y = lensShadow.y + sin(phase * 6.28f + index * 0.7f) * size.height * (0.010f + turbulence * 0.010f)
        val path = Path().apply {
            moveTo(size.width * (0.16f + index * 0.015f), y)
            cubicTo(
                size.width * (0.32f + phase * 0.05f),
                y - size.height * (0.035f + index * 0.002f),
                size.width * (0.62f - phase * 0.04f),
                y + size.height * (0.030f + turbulence * 0.020f),
                size.width * (0.86f - index * 0.012f),
                y - size.height * 0.010f
            )
        }
        drawPath(
            path = path,
            color = listOf(Color.White, Azure, Emerald, Gold)[index % 4].copy(alpha = alpha * (1f - index * 0.08f)),
            style = Stroke(width = (0.75f + pulse * 1.20f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTactileRefractionWave(
    origin: Offset,
    progress: Float,
    accent: Color
) {
    val fade = (1f - progress).coerceIn(0f, 1f)
    val radius = (size.maxDimension * 0.38f * progress).coerceAtLeast(1f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.18f * fade),
                accent.copy(alpha = 0.13f * fade),
                Color.Transparent
            ),
            center = origin,
            radius = radius
        ),
        radius = radius,
        center = origin
    )
    listOf(Azure, Emerald, Gold).forEachIndexed { index, color ->
        val offset = Offset((index - 1) * 2.5.dp.toPx(), (1 - index) * 1.5.dp.toPx())
        drawCircle(
            color = color.copy(alpha = fade * (0.36f - index * 0.045f)),
            radius = radius * (0.78f + index * 0.055f),
            center = origin + offset,
            style = Stroke(width = (1.2f + index * 0.45f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
    repeat(6) { index ->
        val ring = radius * (0.32f + index * 0.10f)
        drawArc(
            color = listOf(Azure, Gold, Emerald)[index % 3].copy(alpha = fade * 0.22f),
            startAngle = progress * 240f + index * 58f,
            sweepAngle = 30f + progress * 28f,
            useCenter = false,
            topLeft = Offset(origin.x - ring, origin.y - ring),
            size = Size(ring * 2f, ring * 2f),
            style = Stroke(width = (0.9f + progress * 0.8f).dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataParticles(
    status: VpnStatus,
    phase: Float,
    parallax: Offset
) {
    val active = status.isConnected || status.isBusy
    if (!active && status.trafficPulse < 0.05f) return

    val load = status.trafficPulse.coerceIn(0f, 1f)
    val density = (6 + load * 34f + if (status.isBusy) 8f else 0f).roundToInt()
    val start = Offset(size.width * 0.50f, size.height * 0.31f) +
        Offset(parallax.x * size.width * 0.020f, parallax.y * size.height * 0.014f)
    val end = Offset(size.width * 0.50f, size.height * 0.74f)
    repeat(density.coerceIn(4, 46)) { index ->
        val lane = ((index % 7) - 3) / 3f
        val localPhase = (phase * (0.86f + load * 1.24f) + index / density.toFloat()) % 1f
        val wave = sin((localPhase * 6.28f) + index * 1.7f)
        val x = start.x + (end.x - start.x) * localPhase + wave * size.width * (0.018f + load * 0.028f) +
            lane * size.width * (0.010f + load * 0.018f)
        val y = start.y + (end.y - start.y) * localPhase
        val alpha = sin(localPhase * PI.toFloat()).coerceAtLeast(0f) *
            (0.045f + load * 0.22f + if (status.isBusy) 0.05f else 0f)
        val particleRadius = (1.1f + load * 2.8f + (index % 3) * 0.55f).dp.toPx()
        val color = listOf(Azure, Emerald, Gold)[index % 3]
        val previous = Offset(
            start.x + (end.x - start.x) * (localPhase - 0.045f) +
                lane * size.width * (0.010f + load * 0.018f),
            start.y + (end.y - start.y) * (localPhase - 0.045f)
        )
        drawLine(
            color = color.copy(alpha = alpha * 0.52f),
            start = previous,
            end = Offset(x, y),
            strokeWidth = particleRadius * (1.2f + load),
            cap = StrokeCap.Round
        )
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
private fun LabsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.78f),
                        Gold.copy(alpha = 0.20f),
                        Azure.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.58f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(28.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(DiffractionA, DiffractionB, DiffractionC, Color.Transparent),
                    start = Offset(size.width * 0.05f, 0f),
                    end = Offset(size.width * 0.95f, size.height)
                ),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 1.1.dp.toPx())
            )
        }
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
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.48f),
                        Gold.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.36f)
                    )
                )
            )
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
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).alpha(if (enabled) 1f else 0.42f).toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChecked), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.Info, contentDescription = help, tint = Muted, modifier = Modifier.padding(start = 6.dp).size(16.dp))
            }
            Text(help, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Emerald.copy(alpha = 0.88f),
                checkedBorderColor = Emerald.copy(alpha = 0.96f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.94f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.56f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.84f),
                disabledCheckedTrackColor = Emerald.copy(alpha = 0.34f),
                disabledUncheckedTrackColor = Color.White.copy(alpha = 0.26f)
            )
        )
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
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color(0xFF132C3B),
                spotColor = Color(0xFF132C3B)
            )
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
    val panelShape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(
                elevation = 18.dp,
                shape = panelShape,
                clip = false,
                ambientColor = Color(0xFF132C3B),
                spotColor = Color(0xFF132C3B)
            )
            .clip(panelShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.78f),
                        Glass,
                        Azure.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.50f)
                    )
                )
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawLensButtonSurface(
                selected = true,
                enabled = true,
                phase = phase,
                cornerRadiusPx = 28.dp.toPx()
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        DiffractionA.copy(alpha = 0.70f),
                        DiffractionB.copy(alpha = 0.52f),
                        DiffractionC.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * 0.04f, 0f),
                    end = Offset(size.width * 0.94f, size.height)
                ),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 1.05.dp.toPx())
            )
        }
        content()
    }
}

private fun statusText(status: VpnStatus): String =
    when (status.phase) {
        ConnectionPhase.CONNECTED -> status.message.ifBlank { "VPN активен" }
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
        status.phase == ConnectionPhase.CONNECTED -> status.activeServer?.endpoint?.let { "Активный сервер $it" } ?: "Туннель запущен"
        status.phase == ConnectionPhase.DEGRADED -> status.message
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

private fun ConnectionMethod.fluxAccent(): Color =
    when (this) {
        ConnectionMethod.BLACKLISTS -> Azure
        ConnectionMethod.WHITELISTS -> Emerald
        ConnectionMethod.PHANTOM_CALL -> Gold
        ConnectionMethod.MESH_ACCESS -> Color(0xFF8FE6FF)
    }

private fun VpnStatus.networkTurbulence(): Float {
    val latencyPressure = activeServer?.latencyMs
        ?.takeIf { phase != ConnectionPhase.DISCONNECTED }
        ?.let { ((it - 90L).coerceAtLeast(0L).toFloat() / 520f).coerceIn(0f, 1f) }
        ?: when {
            phase == ConnectionPhase.ERROR -> 0.95f
            isBusy -> 0.58f
            phase == ConnectionPhase.DEGRADED -> 0.82f
            else -> 0.26f
        }
    val phasePressure = when (phase) {
        ConnectionPhase.CONNECTED -> 0.08f
        ConnectionPhase.DEGRADED -> 0.72f
        ConnectionPhase.ERROR -> 0.95f
        ConnectionPhase.RECOVERING -> 0.78f
        ConnectionPhase.RACING,
        ConnectionPhase.CONNECTING,
        ConnectionPhase.REFRESHING -> 0.52f
        ConnectionPhase.DISCONNECTED -> 0.18f
    }
    return (latencyPressure * 0.56f + phasePressure * 0.34f + trafficPulse * 0.10f).coerceIn(0f, 1f)
}

private fun VpnStatus.networkFlowSpeed(): Float {
    val latencyBoost = activeServer?.latencyMs
        ?.takeIf { phase != ConnectionPhase.DISCONNECTED }
        ?.let { (1f - (it.toFloat() / 650f).coerceIn(0f, 1f)) }
        ?: if (isBusy) 0.64f else 0.34f
    return (0.24f + latencyBoost * 0.42f + trafficPulse * 0.34f).coerceIn(0f, 1f)
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
