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
            float sphere = sqrt(max(0.0, 1.0 - r * r));

            // 3D normal of glass sphere
            float3 normal = float3(d.x, d.y, sphere);

            // Light from upper-left
            float3 lightDir = float3(-0.45, -0.55, 0.78);
            float3 lightN = lightDir / length(lightDir);
            float lambert = max(0.0, dot(normal, lightN));
            float spec = pow(lambert, 28.0) * 1.4;

            // Fresnel: bright at edge
            float fresnel = pow(1.0 - sphere, 2.6);

            // Caustics from refraction (rotating prism patterns)
            float causticA = pow(max(0.0, sin(angle * 7.0 + r * 17.0 - time * 2.2)), 9.0);
            float causticB = pow(max(0.0, cos(angle * 11.0 - r * 21.0 + time * 1.6)), 12.0);
            float caustic = (causticA + causticB) * (0.4 + traffic * 0.6);

            // Top-left specular bright spot
            float2 hp = float2(-0.32, -0.42);
            float topHi = exp(-pow(length(d - hp), 2.0) * 9.0) * 0.85;

            // Bottom-right inner shadow
            float2 sh = float2(0.32, 0.40);
            float botSh = exp(-pow(length(d - sh), 2.0) * 4.0) * 0.55;

            // Color palette
            half3 ice = half3(0.96, 0.99, 1.0);
            half3 mid = half3(0.62, 0.82, 0.96);
            half3 deep = half3(0.18, 0.34, 0.50);
            half3 mint = half3(0.62, 0.96, 0.84);
            half3 gold = half3(1.0, 0.86, 0.45);
            half3 blue = half3(0.46, 0.74, 1.0);

            // Body shading: deep -> mid based on lambert
            half3 base = mix(deep, mid, lambert);
            // Active mode tint
            base = mix(base, mint, active * 0.34 + traffic * 0.22);
            // Top highlight
            base = mix(base, ice, topHi);
            // Bottom shadow
            base = mix(base, half3(0.05, 0.10, 0.18), botSh * 0.55);
            // Specular dot
            base += ice * spec;

            // Chromatic dispersion ring near edge
            float prism = 0.5 + 0.5 * sin(angle * 3.0 + r * 34.0 + time * 1.8);
            half3 split = mix(blue, gold, prism);
            float dispersionRing = exp(-pow(r - 0.92, 2.0) * 80.0);
            base = mix(base, split, dispersionRing * 0.55);

            // Edge fresnel glow
            base = mix(base, ice, fresnel * 0.50);
            // Caustics overlay
            base += half3(caustic * 0.16, caustic * 0.13, caustic * 0.10);

            // Alpha: visible glass sphere
            float core = 0.45 + lambert * 0.30 + topHi * 0.40 + fresnel * 0.30 + spec * 0.30;
            float alpha = min(0.92, core);
            return half4(base * alpha, alpha);
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
            float halo = 1.0 - smoothstep(0.012, 0.080, abs(r - 0.72));
            float diffraction = pow(max(0.0, sin(r * 48.0 - time * 2.6 + angle * 4.0)), 8.0);
            float caustic = pow(max(0.0, cos(angle * 9.0 + r * 22.0 + time * 1.8)), 10.0);
            float chroma = 0.5 + 0.5 * sin(angle * 5.0 + r * 36.0 - time);
            float gated = halo + (1.0 - smoothstep(0.0, 0.14, abs(r - 0.52))) * 0.36;
            float alpha = (halo * 0.18 + diffraction * 0.14 * gated + caustic * 0.10 * gated) * (0.65 + active * 0.40 + traffic * 0.50);
            half3 blue = half3(0.42, 0.78, 1.0);
            half3 mint = half3(0.52, 1.0, 0.82);
            half3 gold = half3(1.0, 0.82, 0.36);
            half3 color = mix(mix(blue, mint, chroma), gold, diffraction * 0.45);
            return half4(color, min(alpha, 0.55));
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
}
