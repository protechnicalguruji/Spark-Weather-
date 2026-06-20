package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CurrentWeather
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherReport
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.components.WeatherBackdrop
import com.example.ui.components.FrostedGlassCard
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val viewModel: WeatherViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    var selectedTab by remember { mutableStateOf("Home") }

    // Derive active background state condition
    val activeCondition = when (val state = uiState) {
        is WeatherUiState.Success -> state.report.current.condition.main
        else -> "Sunny"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_box")
    ) {
        // 1. DYNAMIC HIGH-END BACKPLATE SHADERS & EQUATIONAL WIND PARTICLES
        WeatherBackdrop(condition = activeCondition)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let the custom backdrop show
            bottomBar = {
                FloatingGlassNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.testTag("main_bottom_nav")
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Route views elegantly
                Crossfade(targetState = uiState, label = "state_crossfade") { state ->
                    when (state) {
                        is WeatherUiState.Loading -> {
                            LoadingSkeletonView()
                        }
                        is WeatherUiState.Success -> {
                            when (selectedTab) {
                                "Home" -> HomeScreen(
                                    report = state.report,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "Map" -> MapScreen(
                                    weather = state.report.current,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "Saved" -> SavedCitiesScreen(
                                    viewModel = viewModel,
                                    onCitySelected = { name, lat, lon ->
                                        viewModel.loadCityWeather(name, lat, lon)
                                    },
                                    onNavigateHome = { selectedTab = "Home" },
                                    modifier = Modifier.fillMaxSize()
                                )
                                "Settings" -> SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is WeatherUiState.Error -> {
                            ErrorStateView(
                                message = state.message,
                                onRetry = {
                                    viewModel.loadCityWeather("Tokyo", 35.6762, 139.6503)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// LUXURIOUS GLASS SHIMMER LOADING VISUALS (SKELETON)
// --------------------------------------------------
@Composable
fun LoadingSkeletonView() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_skeletons")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.08f)
        ),
        start = Offset(shimmerOffset, shimmerOffset),
        end = Offset(shimmerOffset + 150f, shimmerOffset + 150f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        // Top Location node skeleton
        Box(
            modifier = Modifier
                .size(width = 160.dp, height = 28.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(14.dp))
        // Big Temp counter skeleton
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Large glass cards layout skeletons
        FrostedGlassCard(
            modifier = Modifier.fillMaxWidth().height(115.dp),
            cornerRadius = 24.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grid skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.22f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.22f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// --------------------------------------------------
// ERROR FAIL-SAFE RECOVERY VIEW
// --------------------------------------------------
@Composable
fun ErrorStateView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            cornerRadius = 28.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Atmosphere Deflection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Recalibrate", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Legacy Greeting component to preserve compatibility with screenshot tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("greeting_test_tag"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Greeting observer: $name",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Box(modifier = Modifier.background(Color.Black)) {
            Greeting("Preview Mode")
        }
    }
}
