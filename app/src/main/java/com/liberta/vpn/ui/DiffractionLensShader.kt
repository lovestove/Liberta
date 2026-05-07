package com.liberta.vpn.ui

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

internal object DiffractionLensShader {
    private const val BioticAgsl = """
        uniform float2 resolution;
        uniform float time;
        uniform float active;
        uniform float traffic;

        float hash(float n) { return fract(sin(n) * 43758.5453123); }
        float noise(float3 x) {
            float3 p = floor(x);
            float3 f = fract(x);
            f = f * f * (3.0 - 2.0 * f);
            float n = p.x + p.y * 57.0 + 113.0 * p.z;
            return mix(mix(mix(hash(n + 0.0), hash(n + 1.0), f.x),
                           mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y),
                       mix(mix(hash(n + 113.0), hash(n + 114.0), f.x),
                           mix(hash(n + 170.0), hash(n + 171.0), f.x), f.y), f.z);
        }

        half4 main(float2 p) {
            float2 uv = p / resolution.xy;
            float2 q = (p - resolution.xy * 0.5) / min(resolution.x, resolution.y);
            
            // Background organic depth
            float d = noise(float3(q * 2.0, time * 0.1));
            half3 base = mix(half3(0.04, 0.07, 0.12), half3(0.02, 0.04, 0.08), d);
            
            // Mycelium threads (using noise as distance field)
            float threads = 0.0;
            for(float i = 1.0; i < 4.0; i++) {
                float freq = pow(2.0, i);
                float n = noise(float3(q * freq + time * 0.05 * i, i * 13.0));
                threads += smoothstep(0.48, 0.52, n) * (1.0 / i);
            }
            
            // Bioluminescence
            float glow = pow(threads, 3.0) * (0.8 + 0.5 * sin(time * 2.0 + length(q) * 10.0));
            half3 cyan = half3(0.3, 0.9, 1.0);
            half3 emerald = half3(0.2, 1.0, 0.6);
            half3 biolum = mix(cyan, emerald, sin(time + q.x));
            
            half3 color = base + biolum * glow * (0.3 + active * 0.4 + traffic * 0.5);
            
            // Subtle "metabolism" pulse
            color *= 1.0 + 0.05 * sin(time * 1.5);
            
            return half4(color, 1.0);
        }
    """

    private const val Agsl = """
        uniform float2 center;
        uniform float radius;
        uniform float time;
        uniform float active;
        uniform float traffic;

        half4 main(float2 p) {
            float2 d = (p - center) / radius;
            float r = length(d);
            if (r > 1.0) return half4(0.0);
            float angle = atan(d.y, d.x);
            
            // Sphere physics
            float sphere = sqrt(max(0.0, 1.0 - r * r));
            float rim = 1.0 - smoothstep(0.88, 1.05, r);
            
            // Chromatic dispersion (spectral split)
            float dispR = 1.0 - smoothstep(0.01, 0.08, abs(r - 0.81));
            float dispG = 1.0 - smoothstep(0.01, 0.08, abs(r - 0.83));
            float dispB = 1.0 - smoothstep(0.01, 0.08, abs(r - 0.85));
            
            // Organic caustics
            float causticA = pow(max(0.0, sin(angle * 6.0 + r * 14.0 - time * 1.8 + sin(time * 0.5))), 10.0);
            float causticB = pow(max(0.0, cos(angle * 13.0 - r * 24.0 + time * 2.2 + cos(time * 0.7))), 14.0);
            float prism = 0.5 + 0.5 * sin(angle * 4.0 + r * 42.0 + time * 1.4);
            
            float light = (0.22 + sphere * 0.72) + (dispR + dispG + dispB) * 0.15 + (causticA + causticB) * 0.18;
            float alpha = min(0.32, rim * (0.04 + light * 0.22 + active * 0.06 + traffic * 0.12));
            
            half3 crystal = half3(0.96, 0.99, 1.0);
            half3 spectral = mix(half3(1.0, 0.4, 0.4), half3(0.4, 1.0, 0.4), dispG);
            spectral = mix(spectral, half3(0.4, 0.4, 1.0), dispB);
            
            half3 color = mix(crystal, spectral, (dispR + dispG + dispB) * 0.4);
            color = mix(color, half3(0.6, 0.9, 1.0), active * 0.35 + traffic * 0.28);
            color += (causticA + causticB) * 0.12;
            
            return half4(color, alpha);
        }
    """

    private const val FieldAgsl = """
        uniform float2 center;
        uniform float radius;
        uniform float time;
        uniform float active;
        uniform float traffic;

        half4 main(float2 p) {
            float2 d = (p - center) / radius;
            float r = length(d);
            float angle = atan(d.y, d.x);
            
            float ripple = sin(r * 42.0 - time * 3.4 + angle * 3.0);
            float halo = 1.0 - smoothstep(0.010, 0.090, abs(r - 0.76 + ripple * 0.015));
            float diffraction = pow(max(0.0, ripple), 12.0);
            float caustic = pow(max(0.0, cos(angle * 11.0 + r * 28.0 + time * 2.1)), 11.0);
            
            float gated = halo + (1.0 - smoothstep(0.0, 0.18, abs(r - 0.56))) * 0.42;
            float alpha = (halo * 0.12 + diffraction * 0.10 * gated + caustic * 0.07 * gated) * (0.60 + active * 0.35 + traffic * 0.45);
            
            half3 violet = half3(0.7, 0.6, 1.0);
            half3 cyan = half3(0.4, 1.0, 1.0);
            half3 gold = half3(1.0, 0.85, 0.4);
            
            half3 color = mix(cyan, violet, r);
            color = mix(color, gold, diffraction * 0.5 + caustic * 0.3);
            
            return half4(color, min(alpha, 0.42));
        }
    """

    fun DrawScope.drawDistortionField(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(FieldAgsl).apply {
                    setFloatUniform("center", center.x, center.y)
                    setFloatUniform("radius", radius)
                    setFloatUniform("time", time)
                    setFloatUniform("active", if (active) 1f else 0f)
                    setFloatUniform("traffic", traffic)
                }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
                }
            }.getOrElse {
                drawFallbackDistortionField(center, radius, time, active, traffic)
            }
        } else {
            drawFallbackDistortionField(center, radius, time, active, traffic)
        }
    }

    fun DrawScope.drawLens(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawAgslLens(center, radius, time, active, traffic)
        } else {
            drawFallbackLens(center, radius, time, active, traffic)
        }
    }

    fun DrawScope.drawPowerGlyph(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        val tint = if (active) Color(0xFF4D9DFF) else Color(0xFF203545)
        val glow = if (active) Color(0xFF83F6D0) else Color.White
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    glow.copy(alpha = 0.20f + traffic * 0.18f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.55f
            ),
            radius = radius * 1.55f,
            center = center
        )
        drawArc(
            color = tint.copy(alpha = 0.62f),
            startAngle = 132f,
            sweepAngle = 276f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = radius * 0.13f, cap = StrokeCap.Round)
        )
        drawLine(
            color = tint.copy(alpha = 0.68f),
            start = Offset(center.x, center.y - radius * 1.10f),
            end = Offset(center.x, center.y - radius * 0.18f),
            strokeWidth = radius * 0.13f,
            cap = StrokeCap.Round
        )
        drawArc(
            color = Color(0xFFFFD86F).copy(alpha = 0.36f),
            startAngle = 222f + time * 8f,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.06f, center.y - radius * 1.06f),
            size = androidx.compose.ui.geometry.Size(radius * 2.12f, radius * 2.12f),
            style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF60F4FF).copy(alpha = 0.26f),
            startAngle = 22f - time * 10f,
            sweepAngle = 54f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.84f, center.y - radius * 0.84f),
            size = androidx.compose.ui.geometry.Size(radius * 1.68f, radius * 1.68f),
            style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawAgslLens(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        runCatching {
            val shader = RuntimeShader(Agsl).apply {
                setFloatUniform("center", center.x, center.y)
                setFloatUniform("radius", radius)
                setFloatUniform("time", time)
                setFloatUniform("active", if (active) 1f else 0f)
                setFloatUniform("traffic", traffic)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
            }
        }.getOrElse {
            drawFallbackLens(center, radius, time, active, traffic)
        }
    }

    private fun DrawScope.drawFallbackLens(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color(0xFFCFF8FF).copy(alpha = 0.08f + traffic * 0.08f),
                    Color(0xFFE7FFF4).copy(alpha = if (active) 0.08f else 0.035f),
                    Color.White.copy(alpha = 0.010f)
                ),
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.34f),
                radius = radius * 1.34f
            ),
            radius = radius,
            center = center
        )
        repeat(5) { index ->
            drawArc(
                color = listOf(Color(0xFF6AD7FF), Color(0xFFFFD86F), Color(0xFF8EFFD4))[index % 3]
                    .copy(alpha = 0.18f + traffic * 0.08f),
                startAngle = 18f + index * 47f + time * (index + 1f),
                sweepAngle = 28f + index * 3f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
                size = androidx.compose.ui.geometry.Size(radius * 1.84f, radius * 1.84f),
                style = Stroke(width = radius * (0.018f + index * 0.003f), cap = StrokeCap.Round)
            )
        }
    }

    private fun DrawScope.drawFallbackDistortionField(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        val alpha = if (active) 0.20f + traffic * 0.12f else 0.11f
        repeat(7) { index ->
            drawArc(
                color = listOf(Color(0xFF6AD7FF), Color(0xFFFFD86F), Color(0xFF8EFFD4))[index % 3]
                    .copy(alpha = alpha * (1f - index * 0.08f)),
                startAngle = index * 31f + time * (7f + index),
                sweepAngle = 22f + index * 5f,
                useCenter = false,
                topLeft = Offset(center.x - radius * (0.42f + index * 0.07f), center.y - radius * (0.42f + index * 0.07f)),
                size = androidx.compose.ui.geometry.Size(radius * (0.84f + index * 0.14f), radius * (0.84f + index * 0.14f)),
                style = Stroke(width = radius * 0.010f, cap = StrokeCap.Round)
            )
        }
    }

    fun DrawScope.drawBioticBackground(time: Float, active: Boolean, traffic: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(BioticAgsl).apply {
                    setFloatUniform("resolution", size.width, size.height)
                    setFloatUniform("time", time)
                    setFloatUniform("active", if (active) 1f else 0f)
                    setFloatUniform("traffic", traffic)
                }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
            }.getOrElse {
                // Fallback handled in LivingBackground
            }
        }
    }
}
