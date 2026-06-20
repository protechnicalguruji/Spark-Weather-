package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// --------------------------------------------------
// DYNAMIC ADVANCED GLASS SHADER CARD
// --------------------------------------------------
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    testTag: String = "",
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(100f, 400f)
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                // If a neon glow parameter is supplied, render a subtle back glow.
                if (glowColor != null) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.15f),
                        size = size,
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 8.dp.toPx())
                    )
                }
            }
    ) {
        // High-end inner reflections (Aesthetic gloss diagonal overlay)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.45f, 0f)
                    lineTo(0f, size.height * 0.75f)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent)
                )
            )
        }
        
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

// --------------------------------------------------
// PROCEDURAL ATMO BACKDROPS (NATIVE CANVAS IMPLEMENTATION)
// --------------------------------------------------
@Composable
fun WeatherBackdrop(
    condition: String,
    modifier: Modifier = Modifier
) {
    // Determine dynamic ambient glowing accent colors matching the weather type
    val glow1Color = when (condition) {
        "Sunny" -> Color(0xFFF59E0B).copy(alpha = 0.16f)   // Amber gold top-left glow
        "Cloudy" -> Color(0xFF475569).copy(alpha = 0.18f)  // Cool slate top-left glow
        "Rain" -> Color(0xFF2563EB).copy(alpha = 0.16f)    // Deep ocean blue top-left glow
        "Storm" -> Color(0xFF4F46E5).copy(alpha = 0.20f)   // Indigo storm top-left glow
        "Snow" -> Color(0xFFE2E8F0).copy(alpha = 0.18f)    // Pale frost silver top-left glow
        "Night" -> Color(0xFF1E3A8A).copy(alpha = 0.18f)   // Deep space sapphire top-left glow
        else -> Color(0xFF4F46E5).copy(alpha = 0.18f)      // Sophisticated Indigo default
    }

    val glow2Color = when (condition) {
        "Sunny" -> Color(0xFFEC4899).copy(alpha = 0.12f)   // Sunset magenta bottom-right glow
        "Cloudy" -> Color(0xFF334155).copy(alpha = 0.14f)  // Dark sky blue-grey bottom-right glow
        "Rain" -> Color(0xFF0D9488).copy(alpha = 0.12f)    // Emerald moisture bottom-right glow
        "Storm" -> Color(0xFF7C3AED).copy(alpha = 0.15f)   // Royal purple lightning bottom-right glow
        "Snow" -> Color(0xFF38BDF8).copy(alpha = 0.12f)    // Clear glacier teal bottom-right glow
        "Night" -> Color(0xFF701A75).copy(alpha = 0.14f)   // Cosmic nebula violet bottom-right glow
        else -> Color(0xFF7C3AED).copy(alpha = 0.14f)      // Sophisticated Violet default
    }

    // Animate subtle breathing/offset shift for organic ambient lighting motion
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glows")

    val driftY1 by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_drift_y1"
    )

    val driftX2 by infiniteTransition.animateFloat(
        initialValue = -90f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_drift_x2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Ground backdrop in luxurious carbon-slate black matching '#08090D'
                drawRect(color = Color(0xFF08090D))

                // Render Top-Left Ambient Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow1Color, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.18f + driftY1),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.15f, size.height * 0.18f + driftY1)
                )

                // Render Bottom-Right Ambient Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow2Color, Color.Transparent),
                        center = Offset(size.width * 0.85f + driftX2, size.height * 0.82f),
                        radius = size.width * 0.95f
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width * 0.85f + driftX2, size.height * 0.82f)
                )
            }
    ) {
        // Run customized procedural simulations based on active weather state
        when (condition) {
            "Sunny" -> SunnySimulation()
            "Cloudy" -> CloudySimulation()
            "Rain" -> RainSimulation(isStorm = false)
            "Storm" -> RainSimulation(isStorm = true)
            "Snow" -> SnowSimulation()
            "Night" -> NightSimulation()
        }
    }
}

@Composable
fun SunnySimulation() {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    
    // Pulsing core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Ray rotation
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width * 0.85f
        val centerY = size.height * 0.18f
        val sunRadius = 60.dp.toPx()

        // Rotating rays
        rotate(rayRotation, pivot = Offset(centerX, centerY)) {
            for (i in 0 until 8) {
                val angle = i * (2 * PI / 8)
                val startDist = sunRadius + 12.dp.toPx()
                val endDist = sunRadius + 45.dp.toPx()
                val thickness = 4.dp.toPx()
                
                val startX = centerX + startDist * cos(angle).toFloat()
                val startY = centerY + startDist * sin(angle).toFloat()
                val endX = centerX + endDist * cos(angle).toFloat()
                val endY = centerY + endDist * sin(angle).toFloat()

                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round
                )
            }
        }

        // Concentric sun flares
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color(0xFFFFF275).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = sunRadius * 2.8f * pulseScale
            ),
            center = Offset(centerX, centerY),
            radius = sunRadius * 2.8f * pulseScale
        )

        // Golden sun core
        drawCircle(
            color = Color.White,
            center = Offset(centerX, centerY),
            radius = sunRadius * pulseScale
        )
    }
}

@Composable
fun CloudySimulation() {
    // 3 animated cloud plates moving across screen
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    
    val movement1 by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "move1"
    )

    val movement2 by infiniteTransition.animateFloat(
        initialValue = 950f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(34000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "move2"
    )

    val verticalWave by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Cloud 1
        val x1 = (movement1 / 800f) * width
        val y1 = height * 0.12f + verticalWave.dp.toPx()
        drawCloudPlate(center = Offset(x1, y1), scale = 1.1f, opacity = 0.18f)

        // Cloud 2
        val x2 = (movement2 / 800f) * width
        val y2 = height * 0.22f - verticalWave.dp.toPx()
        drawCloudPlate(center = Offset(x2, y2), scale = 0.85f, opacity = 0.14f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudPlate(
    center: Offset,
    scale: Float,
    opacity: Float
) {
    val cloudBase = 45.dp.toPx() * scale
    drawCircle(
        color = Color.White.copy(alpha = opacity),
        radius = cloudBase,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = opacity),
        radius = cloudBase * 0.8f,
        center = Offset(center.x - cloudBase * 0.7f, center.y + cloudBase * 0.2f)
    )
    drawCircle(
        color = Color.White.copy(alpha = opacity),
        radius = cloudBase * 0.9f,
        center = Offset(center.x + cloudBase * 0.7f, center.y + cloudBase * 0.1f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = opacity),
        topLeft = Offset(center.x - cloudBase * 1.2f, center.y + cloudBase * 0.2f),
        size = Size(cloudBase * 2.4f, cloudBase * 0.7f),
        cornerRadius = CornerRadius(cloudBase, cloudBase)
    )
}

@Composable
fun RainSimulation(isStorm: Boolean) {
    val frameCount = remember { mutableStateOf(0) }
    
    // Coroutine ticker driving particles
    LaunchedEffect(Unit) {
        while (isActive) {
            frameCount.value++
            delay(16) // ~60 FPS
        }
    }

    // Retain particle lists
    val drops = remember {
        List(40) {
            Raindrop(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.02f + Random.nextFloat() * 0.03f,
                length = 15f + Random.nextFloat() * 25f
            )
        }
    }

    val lightningAlpha = remember { Animatable(0f) }

    if (isStorm) {
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(Random.nextLong(4000, 9000))
                // Execute a split dramatic lightning flash
                lightningAlpha.animateTo(0.6f, tween(80))
                lightningAlpha.animateTo(0.1f, tween(50))
                lightningAlpha.animateTo(0.8f, tween(100))
                lightningAlpha.animateTo(0.0f, tween(400))
            }
        }
    }

    // Compute frame variations
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // If lightning holds active value, color background sheet in premium violet lightning glow
        if (lightningAlpha.value > 0f) {
            drawRect(
                color = Color(0xFFA5C9FF).copy(alpha = lightningAlpha.value * 0.35f),
                size = size
            )
            // Draw a quick lightning branch
            if (lightningAlpha.value > 0.4f) {
                val boltPath = Path().apply {
                    moveTo(width * 0.7f, 0f)
                    lineTo(width * 0.65f, height * 0.15f)
                    lineTo(width * 0.72f, height * 0.2f)
                    lineTo(width * 0.61f, height * 0.42f)
                    lineTo(width * 0.66f, height * 0.45f)
                    lineTo(width * 0.55f, height * 0.65f)
                }
                drawPath(
                    path = boltPath,
                    color = Color.White.copy(alpha = lightningAlpha.value * 0.9f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Draw and update falling particles
        drops.forEach { drop ->
            // Advance vertical state
            drop.y += drop.speed
            if (drop.y > 1f) {
                drop.y = -0.05f
                drop.x = Random.nextFloat()
            }

            val dx = drop.x * width
            val dy = drop.y * height
            
            // Draw rain vector
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(dx, dy),
                end = Offset(dx - 3f, dy + drop.length),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Ripple circles when landing near bottom container
            if (drop.y > 0.85f && Random.nextFloat() > 0.94f) {
                val rippleRadius = (1f - drop.y) * 80.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = (1f - drop.y) * 0.25f),
                    radius = rippleRadius,
                    center = Offset(dx, height * 0.9f),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }
    }
}

private class Raindrop(var x: Float, var y: Float, var speed: Float, var length: Float)

@Composable
fun SnowSimulation() {
    val frameCount = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            frameCount.value++
            delay(20)
        }
    }

    val flakes = remember {
        List(30) {
            Snowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.003f + Random.nextFloat() * 0.005f,
                radius = 2.dp + 4.dp * Random.nextFloat(),
                angleOffset = Random.nextFloat() * 2f * 3.14159f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        flakes.forEach { flake ->
            // Advance vertical position and horizontal wiggle
            flake.y += flake.speed
            flake.angleOffset += 0.03f
            
            if (flake.y > 1.0f) {
                flake.y = -0.05f
                flake.x = Random.nextFloat()
            }

            val wiggleX = sin(flake.angleOffset.toDouble()).toFloat() * 15f
            val dx = flake.x * width + wiggleX
            val dy = flake.y * height

            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = flake.radius.toPx(),
                center = Offset(dx, dy)
            )
        }
    }
}

private class Snowflake(var x: Float, var y: Float, var speed: Float, var radius: Dp, var angleOffset: Float)

@Composable
fun NightSimulation() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    
    val pulsingStars = (0..5).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500 + index * 400, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_$index"
        )
    }

    // Rare aurora waving line
    val auroraPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw glowing Aurora curves (Dreamy visual polish)
        val auroraPath = Path()
        auroraPath.moveTo(0f, height * 0.25f)
        for (x in 0 until width.toInt() step 20) {
            val y = height * 0.28f + sin(x * 0.004f + auroraPhase) * 60f + cos(x * 0.002f + auroraPhase * 0.5f) * 20f
            auroraPath.lineTo(x.toFloat(), y)
        }
        
        drawPath(
            path = auroraPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00FFCC).copy(alpha = 0.0f),
                    Color(0xFF00FF88).copy(alpha = 0.16f),
                    Color(0xFF8800FF).copy(alpha = 0.12f),
                    Color(0xFF00FFCC).copy(alpha = 0.0f)
                )
            ),
            style = Stroke(width = 48.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Stars
        val seedPoints = listOf(
            Offset(0.15f, 0.08f), Offset(0.42f, 0.12f), Offset(0.78f, 0.05f),
            Offset(0.88f, 0.22f), Offset(0.62f, 0.32f), Offset(0.28f, 0.25f),
            Offset(0.08f, 0.35f), Offset(0.51f, 0.04f), Offset(0.93f, 0.14f)
        )

        seedPoints.forEachIndexed { i, point ->
            val pulse = pulsingStars[i % pulsingStars.size].value
            drawCircle(
                color = Color.White.copy(alpha = pulse),
                radius = 1.8.dp.toPx() + (if (i % 3 == 0) 1.dp.toPx() else 0f),
                center = Offset(point.x * width, point.y * height)
            )
        }

        // 3. Perfect moonlit crescent core
        val moonX = width * 0.8f
        val moonY = height * 0.15f
        val r = 24.dp.toPx()

        // Outer halo glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFE0).copy(alpha = 0.2f), Color.Transparent),
                center = Offset(moonX, moonY),
                radius = r * 2.2f
            ),
            center = Offset(moonX, moonY),
            radius = r * 2.2f
        )

        // Light background crescent
        drawCircle(
            color = Color(0xFFFFFFE0).copy(alpha = 0.95f),
            radius = r,
            center = Offset(moonX, moonY)
        )

        // Overlapping shadow to compose crescent
        drawCircle(
            color = Color(0xFF000428), // Matches dark top gradient color perfectly
            radius = r * 0.95f,
            center = Offset(moonX - r * 0.5f, moonY - r * 0.1f)
        )
    }
}

// --------------------------------------------------
// ELITE DYNAMIC COMPOSE WEATHER ICONS
// --------------------------------------------------
@Composable
fun DynamicWeatherIcon(
    condition: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp
) {
    val transition = rememberInfiniteTransition(label = "icon_anim")
    
    // Smooth hover float
    val floatOffset by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Mild rotation specs
    val rotateAngle by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3100, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate_icon"
    )

    Box(
        modifier = modifier
            .size(iconSize)
            .graphicsLayer {
                translationY = floatOffset.dp.toPx()
                rotationZ = rotateAngle
            },
        contentAlignment = Alignment.Center
    ) {
        val vectorIcon = when (condition) {
            "Sunny" -> Icons.Outlined.WbSunny
            "Cloudy" -> Icons.Outlined.Cloud
            "Rain" -> Icons.Outlined.Grain
            "Storm" -> Icons.Outlined.Thunderstorm
            "Snow" -> Icons.Outlined.SevereCold
            "Night" -> Icons.Outlined.NightsStay
            else -> Icons.Outlined.Pattern
        }

        val iconTypeAccent = when (condition) {
            "Sunny" -> Color(0xFFF39C12)
            "Cloudy" -> Color(0xFFECF0F1)
            "Rain" -> Color(0xFF3498DB)
            "Storm" -> Color(0xFF9B59B6)
            "Snow" -> Color(0xFFEFFFFA)
            "Night" -> Color(0xFFF1C40F)
            else -> Color.White
        }

        // Draw a soft glass circle backdrop inside icon
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )

        Icon(
            imageVector = vectorIcon,
            contentDescription = "$condition weather icon",
            modifier = Modifier.fillMaxSize(0.65f),
            tint = iconTypeAccent
        )
    }
}

// --------------------------------------------------
// SUSPENDED GLASS BOTTOM DOCK NAVIGATION
// --------------------------------------------------
@Composable
fun FloatingGlassNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem("Home", Icons.Filled.WbCloudy, Icons.Outlined.WbCloudy, "tab_home"),
        NavItem("Map", Icons.Filled.Map, Icons.Outlined.Map, "tab_map"),
        NavItem("Saved", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "tab_saved"),
        NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars) // Prerequisite: Prevent overlapping Android navigation pills
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1A1C24).copy(alpha = 0.60f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val isSelected = selectedTab == item.title
            val activeColor = Color.White
            val inactiveColor = Color.White.copy(alpha = 0.45f)
            
            // Background interactive indicator animation
            val tabWidthScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.8f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "bar_active"
            )

            Column(
                modifier = Modifier
                    .testTag(item.tag)
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTabSelected(item.title) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                    contentDescription = item.title,
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    color = if (isSelected) activeColor else inactiveColor,
                    style = MaterialTheme.typography.labelSmall
                )

                // High-fidelity active dot indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(width = (12 * tabWidthScale).dp, height = 3.dp)
                            .background(Color(0xFF3B82F6), CircleShape) // Spark luxury electric blue accent
                    )
                } else {
                    Spacer(modifier = Modifier.height(7.dp))
                }
            }
        }
    }
}

private data class NavItem(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val tag: String
)
