package com.liberta.vpn.ui

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

internal object BioticFluxShader {
    
    // The Nucleus: A living cell with shifting internal "DNA" and pulsing light
    private const val NucleusAgsl = """
        uniform float2 center;
        uniform float radius;
        uniform float time;
        uniform float active;
        uniform float traffic;

        float noise(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 p) {
            float2 d = (p - center) / radius;
            float r = length(d);
            float angle = atan(d.y, d.x);
            
            // Pulsing nucleus shape
            float pulse = 1.0 + 0.05 * sin(time * 2.0) * (1.0 + traffic);
            float shape = 1.0 - smoothstep(0.7 * pulse, 0.9 * pulse, r);
            
            // Internal "DNA" strands
            float strands = pow(max(0.0, sin(angle * 5.0 + r * 12.0 - time * 1.5)), 8.0);
            strands += pow(max(0.0, cos(angle * 3.0 - r * 8.0 + time * 1.2)), 10.0);
            
            // Bioluminescent glow
            float glow = exp(-r * 2.2) * (0.3 + 0.2 * sin(time * 3.0));
            float alpha = shape * (0.4 + strands * 0.4 + active * 0.2 + traffic * 0.3);
            alpha = max(alpha, glow * 0.15);
            
            half3 coreColor = half3(0.1, 0.4, 0.8); // Deep Blue
            half3 activeColor = half3(0.2, 0.9, 0.6); // Bio Green
            half3 trafficColor = half3(1.0, 0.8, 0.4); // Bio Amber
            
            half3 color = mix(coreColor, activeColor, active);
            color = mix(color, trafficColor, traffic * 0.6);
            color += strands * 0.3;
            
            return half4(color, min(alpha, 0.85));
        }
    """

    // The Mycelium: Glowing synapses that reach out
    private const val MyceliumAgsl = """
        uniform float2 center;
        uniform float radius;
        uniform float time;
        uniform float active;
        uniform float traffic;

        half4 main(float2 p) {
            float2 d = (p - center) / radius;
            float r = length(d);
            float angle = atan(d.y, d.x);
            
            // Growing synapses
            float synapse = pow(max(0.0, sin(r * 24.0 - time * 3.0 + angle * 6.0)), 12.0);
            float pulse = 0.5 + 0.5 * sin(time * 4.0 + r * 10.0);
            
            float alpha = synapse * 0.15 * (active + traffic * 0.5 + 0.2) * pulse;
            
            half3 synapseColor = half3(0.4, 0.8, 1.0);
            if (active > 0.5) synapseColor = half3(0.6, 1.0, 0.8);
            
            return half4(synapseColor, min(alpha, 0.4));
        }
    """

    fun DrawScope.drawNucleus(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(NucleusAgsl).apply {
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
                    canvas.nativeCanvas.drawCircle(center.x, center.y, radius * 1.5f, paint)
                }
            }.getOrElse {
                drawFallbackNucleus(center, radius, time, active, traffic)
            }
        } else {
            drawFallbackNucleus(center, radius, time, active, traffic)
        }
    }

    fun DrawScope.drawMycelium(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(MyceliumAgsl).apply {
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
                    canvas.nativeCanvas.drawCircle(center.x, center.y, radius * 3f, paint)
                }
            }.getOrElse {
                // Fallback handled in LibertaUi
            }
        }
    }

    private fun DrawScope.drawFallbackNucleus(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        val color = if (active) Color(0xFF5FE2B3) else Color(0xFF77B8FF)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.6f + traffic * 0.2f), color.copy(alpha = 0.1f), Color.Transparent),
                center = center,
                radius = radius * 1.2f
            ),
            radius = radius * 1.2f,
            center = center
        )
        // Internal "DNA" fallback
        repeat(3) { index ->
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = time * 50f + index * 120f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
                size = Size(radius * 1.4f, radius * 1.4f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    fun DrawScope.drawBioGlyph(center: Offset, radius: Float, time: Float, active: Boolean, traffic: Float) {
        val tint = if (active) Color(0xFF83F6D0) else Color(0xFF77B8FF)
        // Draw an organic heart-like nucleus core
        val pulse = 1f + 0.1f * kotlin.math.sin(time * 3f)
        drawCircle(
            color = tint.copy(alpha = 0.8f),
            radius = radius * 0.4f * pulse,
            center = center
        )
        drawCircle(
            color = tint.copy(alpha = 0.2f),
            radius = radius * 0.8f * pulse,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
