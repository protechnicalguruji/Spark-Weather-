package com.example.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiSummaryService
import com.example.data.api.WeatherApiService
import com.example.data.database.WeatherDatabase
import com.example.data.database.WeatherRepository
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.SavedCity
import com.example.data.model.UserSettings
import com.example.data.model.WeatherReport
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.DynamicWeatherIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.StateFlow as KotlinStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sin

// --------------------------------------------------
// VIEW MODEL WITH ROOM PERSISTENCE & GEMINI CALLS
// --------------------------------------------------
class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WeatherDatabase.getDatabase(application)
    private val repository = WeatherRepository(db.savedCityDao())

    // UI state flows
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    // Observe saved cities reactively from database
    val savedCities: StateFlow<List<SavedCity>> = repository.allSavedCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<WeatherApiService.PresetCity>>(emptyList())
    val searchResults: StateFlow<List<WeatherApiService.PresetCity>> = _searchResults.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(listOf("Tokyo", "San Francisco", "Zermatt"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    init {
        // Load initial city (Tokyo as luxurious default)
        loadCityWeather("Tokyo", 35.6762, 139.6503)
        preseedInitialCitiesToDatabase()
    }

    private fun preseedInitialCitiesToDatabase() {
        viewModelScope.launch {
            // Seed a couple of favorites so first-launch is beautiful
            val initialSeeds = listOf(
                SavedCity("Tokyo", "Japan", 35.6762, 139.6503, 24.5f, "Cloudy"),
                SavedCity("San Francisco", "United States", 37.7749, -122.4194, 18.2f, "Sunny"),
                SavedCity("Zermatt", "Switzerland", 46.0207, 7.7491, -2.5f, "Snow")
            )
            initialSeeds.forEach { city ->
                repository.insertCity(city)
            }
        }
    }

    fun loadCityWeather(cityName: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _aiSummary.value = null // reset
            try {
                val report = WeatherApiService.getWeatherReport(cityName, lat, lon)
                _uiState.value = WeatherUiState.Success(report)
                
                // Add to recent searches if not present
                val currentRecents = _recentSearches.value.toMutableList()
                currentRecents.remove(cityName)
                currentRecents.add(0, cityName)
                _recentSearches.value = currentRecents.take(5)
                
                // Fetch Gemini AI Briefing
                triggerAiBriefing(report.current)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Failed to fetch. Tap to retry.")
            }
        }
    }

    fun triggerAiBriefing(weather: CurrentWeather) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val tempUnit = _settings.value.tempUnit
            val summary = GeminiSummaryService.fetchWeatherSummary(weather, tempUnit)
            _aiSummary.value = summary
            _isAiLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.trim().isEmpty()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = WeatherApiService.searchCities(query)
            }
        }
    }

    fun toggleSaveCity(cityName: String, lat: Double, lon: Double, currentTemp: Float, condition: String) {
        viewModelScope.launch {
            val isCurrentlySaved = savedCities.value.any { it.cityName.equals(cityName, ignoreCase = true) }
            if (isCurrentlySaved) {
                repository.deleteCity(cityName)
            } else {
                repository.insertCity(
                    SavedCity(
                        cityName = cityName,
                        country = "Saved Region",
                        latitude = lat,
                        longitude = lon,
                        currentTemp = currentTemp,
                        conditionMain = condition
                    )
                )
            }
        }
    }

    fun deleteSavedCity(cityName: String) {
        viewModelScope.launch {
            repository.deleteCity(cityName)
        }
    }

    // Settings actions
    fun setTempUnit(unit: String) {
        _settings.value = _settings.value.copy(tempUnit = unit)
        // Refresh weather data presentation if loaded
        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            triggerAiBriefing(currentState.report.current)
        }
    }

    fun setWindUnit(unit: String) {
        _settings.value = _settings.value.copy(windUnit = unit)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun setAlertsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(alertsEnabled = enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(isDarkMode = enabled)
    }
}

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val report: WeatherReport) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

// --------------------------------------------------
// HIGH-END SPLINE CHART COMPONENT
// --------------------------------------------------
@Composable
fun InteractiveSplineChart(
    hourly: List<HourlyForecast>,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    val temps = hourly.take(12).map { it.temp }
    val maxTemp = temps.maxOrNull() ?: 30f
    val minTemp = temps.minOrNull() ?: 10f
    val range = (maxTemp - minTemp).coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Temperature Timeline",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (temps.size - 1)

            val points = temps.mapIndexed { index, temp ->
                val x = index * spacing
                // Map temp to y-axis inverted
                val y = height - ((temp - minTemp) / range) * (height - 35.dp.toPx()) - 15.dp.toPx()
                Offset(x, y)
            }

            // Draw area gradient under curve
            val fillPath = Path().apply {
                moveTo(0f, height)
                lineTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    // Bezier points
                    cubicTo(
                        x1 = (prev.x + curr.x) / 2, y1 = prev.y,
                        x2 = (prev.x + curr.x) / 2, y2 = curr.y,
                        x3 = curr.x, y3 = curr.y
                    )
                }
                lineTo(width, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
                )
            )

            // Draw spline curve path
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    cubicTo(
                        x1 = (prev.x + curr.x) / 2, y1 = prev.y,
                        x2 = (prev.x + curr.x) / 2, y2 = curr.y,
                        x3 = curr.x, y3 = curr.y
                    )
                }
            }

            drawPath(
                path = linePath,
                color = Color.White,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw point rings & temperatures
            points.forEachIndexed { i, point ->
                // Draw glowing node
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                    radius = 7.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Horizontal hourly titles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourly.take(12).filterIndexed { i, _ -> i % 2 == 0 }.forEach { forecast ->
                Text(
                    text = forecast.time,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

// --------------------------------------------------
// WIDGET PLAYGROUND SIMULATOR COMPONENT
// --------------------------------------------------
@Composable
fun WidgetPlayground(
    weather: CurrentWeather,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Station Widgets (Apple-HIG Style)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Small Widget
            FrostedGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = weather.cityName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.WbCloudy,
                            contentDescription = null,
                            tint = Color(0xFFFFF176),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${weather.temp.toInt()}$tempUnit",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                        Text(
                            text = "H:${weather.high.toInt()}° L:${weather.low.toInt()}°",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Medium Widget
            FrostedGlassCard(
                modifier = Modifier
                    .weight(1.8f)
                    .aspectRatio(1.8f),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = weather.cityName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = weather.condition.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = "${weather.temp.toInt()}$tempUnit",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WidgetMetricNode("UV Index", "${weather.uvIndex}", Icons.Outlined.WbSunny)
                        WidgetMetricNode("Wind", "${weather.windSpeed.toInt()} km/h", Icons.Outlined.Air)
                        WidgetMetricNode("Humidity", "${weather.humidity}%", Icons.Outlined.WaterDrop)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetMetricNode(label: String, value: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Column {
            Text(text = label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
            Text(text = value, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// --------------------------------------------------
// WEATHER MAP SCREEN (DETAILED INTERACTIVE RADAR)
// --------------------------------------------------
@Composable
fun MapScreen(
    weather: CurrentWeather,
    modifier: Modifier = Modifier
) {
    var activeLayer by remember { mutableStateOf("Temp") }
    var zoomLevel by remember { mutableStateOf(1.2f) }
    val animatePhase = rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Atmospheric Map Radar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Radar viewport glass frame
            FrostedGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Custom Simulated World Map Plot drawn on Canvas with zoom transformations
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                // Pinch/zoom simulations if requested
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        // Center radar coordinates based on city name hash
                        val centerCoordPoint = Offset(
                            width * 0.5f + (weather.cityName.length % 5) * 20f,
                            height * 0.5f - (weather.cityName.length % 4) * 15f
                        )

                        // Draw Grid lines
                        val verticalLines = (3..12)
                        verticalLines.forEach { index ->
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(width * (index / 12f), 0f),
                                end = Offset(width * (index / 12f), height),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, height * (index / 12f)),
                                end = Offset(width, height * (index / 12f)),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw abstract vector terrain geometries
                        val islandPath = Path().apply {
                            moveTo(width * 0.2f, height * 0.3f)
                            quadraticTo(width * 0.35f, height * 0.25f, width * 0.45f, height * 0.35f)
                            quadraticTo(width * 0.5f, height * 0.5f, width * 0.3f, height * 0.6f)
                            quadraticTo(width * 0.15f, height * 0.45f, width * 0.2f, height * 0.3f)
                        }
                        
                        drawPath(
                            path = islandPath,
                            color = Color.White.copy(alpha = 0.05f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Draw active radar colors matching selected weather layer
                        val layerColors = when (activeLayer) {
                            "Temp" -> listOf(Color(0xFFFF5252), Color(0xFFFFEB3B), Color(0xFF2196F3))
                            "Rain" -> listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0x00000000))
                            "Wind" -> listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0x00000000))
                            "Clouds" -> listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0x00000000))
                            else -> listOf(Color(0xFF7C4DFF), Color(0xFF00B0FF), Color(0x00000000))
                        }

                        // Radar pulses sweeping out from center coordinates
                        val sweepRadius = 150.dp.toPx() * zoomLevel
                        val activePulseRadius = sweepRadius * animatePhase.value

                        // Core layer density
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = layerColors,
                                center = centerCoordPoint,
                                radius = sweepRadius * 1.3f
                            ),
                            center = centerCoordPoint,
                            radius = sweepRadius * 1.3f,
                            alpha = 0.45f
                        )

                        // Expanding boundary pulses
                        drawCircle(
                            color = layerColors.first().copy(alpha = (1f - animatePhase.value) * 0.4f),
                            radius = activePulseRadius,
                            center = centerCoordPoint,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Target coordinate star
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = centerCoordPoint
                        )
                        drawCircle(
                            color = Color(0xFF3B82F6),
                            radius = 12.dp.toPx(),
                            center = centerCoordPoint,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Floating Layer Controls inside card overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val layers = listOf("Temp", "Rain", "Wind", "Clouds")
                        layers.forEach { layer ->
                            Box(
                                modifier = Modifier
                                    .testTag("map_layer_$layer")
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (activeLayer == layer) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable { activeLayer = layer }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = layer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Floating zoom HUD sliders
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(2.5f) },
                            modifier = Modifier.size(28.dp).testTag("map_zoom_in")
                        ) {
                            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.5f) },
                            modifier = Modifier.size(28.dp).testTag("map_zoom_out")
                        ) {
                            Icon(Icons.Filled.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Information tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${weather.cityName} • Radar Live",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Floating Bottom Navigation spacer
        }
    }
}

// --------------------------------------------------
// SAVED / FAVORITES SCREEN COMPONENT
// --------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCitiesScreen(
    viewModel: WeatherViewModel,
    onCitySelected: (String, Double, Double) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedList by viewModel.savedCities.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Saved Stations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Instant premium Glass Search input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search city, country world...", color = Color.White.copy(alpha = 0.45f)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Filled.Clear, null, tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.35f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("city_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search result overlays or dynamic list
        if (searchQuery.isNotEmpty()) {
            Text(
                text = "Global Stations Founded",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { city ->
                    FrostedGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadCityWeather(city.name, city.lat, city.lon)
                                viewModel.updateSearchQuery("")
                                onNavigateHome()
                            }
                            .testTag("search_result_item_${city.name}"),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = city.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = city.country, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                            
                            IconButton(
                                onClick = {
                                    viewModel.toggleSaveCity(city.name, city.lat, city.lon, 20f, city.defaultCondition)
                                },
                                modifier = Modifier.testTag("save_city_toggle_${city.name}")
                            ) {
                                val isSaved = savedList.any { it.cityName.equals(city.name, ignoreCase = true) }
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                                    contentDescription = "Save favorite",
                                    tint = if (isSaved) Color(0xFFE91E63) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Main favorites/saved cities panel list
            if (savedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Saved Cities",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search and bookmark locations above.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(savedList, key = { it.cityName }) { city ->
                        FrostedGlassCard(
                            modifier = Modifier
                                .animateItemPlacement()
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadCityWeather(city.cityName, city.latitude, city.longitude)
                                    onNavigateHome()
                                }
                                .testTag("favorite_card_${city.cityName}"),
                            cornerRadius = 24.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.cityName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = city.country,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = city.conditionMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFA5C9FF)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${city.currentTemp.toInt()}°",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = Color.White,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteSavedCity(city.cityName) },
                                        modifier = Modifier.testTag("delete_saved_${city.cityName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Swipe Delete",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Popular Preset Hub
            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Premium Preset Hub",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(WeatherApiService.popularCities.take(5)) { preset ->
                    Box(
                        modifier = Modifier
                            .testTag("preset_pill_${preset.name}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.loadCityWeather(preset.name, preset.lat, preset.lon)
                                onNavigateHome()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --------------------------------------------------
// SETTINGS SCREEN COMPONENT
// --------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Station Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // General Panel
        SettingsSectionHeader("Visual Theme & Aesthetics")
        FrostedGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsSwitchRow(
                    title = "Premium Dark Aura",
                    subtitle = "Always utilize visionOS ambient backdrop",
                    checked = settings.isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    testTag = "setting_dark_mode"
                )
                SettingsSwitchRow(
                    title = "Automatic Theme sync",
                    subtitle = "Align color scheme with device settings",
                    checked = settings.useSystemTheme,
                    onCheckedChange = { /* Simulated Theme binding */ },
                    testTag = "setting_system_theme"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionHeader("Meteorological Units")
        FrostedGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsSegmentSelector(
                    title = "Temperature scale",
                    options = listOf("°C", "°F"),
                    selectedOption = settings.tempUnit,
                    onOptionSelected = { viewModel.setTempUnit(it) },
                    testTag = "setting_temp_unit"
                )
                SettingsSegmentSelector(
                    title = "Wind metrics speed",
                    options = listOf("km/h", "mph", "m/s"),
                    selectedOption = settings.windUnit,
                    onOptionSelected = { viewModel.setWindUnit(it) },
                    testTag = "setting_wind_unit"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionHeader("Atmosphere Alarms & Push")
        FrostedGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsSwitchRow(
                    title = "Active Rain & Storm alerts",
                    subtitle = "Recieve push warning when moisture vectors rise",
                    checked = settings.alertsEnabled,
                    onCheckedChange = { viewModel.setAlertsEnabled(it) },
                    testTag = "setting_weather_alerts"
                )
                SettingsSwitchRow(
                    title = "Morning Summary briefings",
                    subtitle = "Receive AI-briefings every morning at 07:00 AM",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    testTag = "setting_notifications"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionHeader("Engine & Integrity info")
        FrostedGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsInfoRow("App Variant Name", "Spark Station Weather • Liquid Engine")
                SettingsInfoRow("Vibe Style", "iOS 26 Liquid Glass / VisionOS")
                SettingsInfoRow("Local Persistence", "Room SQLite Enabled (Verified)")
                SettingsInfoRow("API Core Gateway", "OpenWeatherMap + WeatherAPI REST Fail-safe")
                SettingsInfoRow("AI Summarizer Mode", "Vertex Direct AI (gemini-3.5-flash)")
            }
        }
        
        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.65f),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun SettingsSegmentSelector(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// --------------------------------------------------
// WEATHER COMPOSABLE SUB-PANE METEOROLOGICAL CARDS
// --------------------------------------------------
@Composable
fun MetricGridCell(
    label: String,
    value: String,
    icon: ImageVector,
    details: String,
    testTag: String = ""
) {
    FrostedGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.22f),
        cornerRadius = 24.dp,
        testTag = testTag
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
