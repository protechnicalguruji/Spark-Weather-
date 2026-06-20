package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeather
import com.example.data.model.WeatherReport
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.DynamicWeatherIcon

@Composable
fun HomeScreen(
    report: WeatherReport,
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val weather = report.current
    val hourlyList = report.hourly
    val dailyList = report.daily
    
    val settings by viewModel.settings.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. TOP MAIN HEADER SECTION (Inspired by HIG & Apple Weather)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_header"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = weather.cityName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("home_city_name")
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                // Grand Premium Temp Display
                Text(
                    text = "${weather.temp.toInt()}${settings.tempUnit}",
                    fontSize = 90.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("home_current_temp")
                )

                // Large Animated Weather Icon
                DynamicWeatherIcon(
                    condition = weather.condition.main,
                    iconSize = 100.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = weather.condition.description,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feels like ${weather.feelsLike.toInt()}°",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "H:${weather.high.toInt()}°  L:${weather.low.toInt()}°",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. PREMIUM AI BRIEFING CARD FUELED BY GEMINI (VisionOS Design Language)
            FrostedGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                cornerRadius = 28.dp,
                glowColor = Color(0xFF3B82F6).copy(alpha = 0.25f), // Premium Electric Blue accent
                testTag = "ai_briefing_card"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Text(
                                text = "Vertex AI Briefing Engine",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.triggerAiBriefing(weather) },
                            modifier = Modifier.size(24.dp).testTag("regenerate_ai_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "Regenerate summary",
                                tint = Color(0xFFA5C9FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAiLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Decrypting cloud matrices...",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        Text(
                            text = aiSummary ?: "Formulating ambient atmospheric briefing...",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"Weather, Beautifully Reimagined.\"",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. HOURLY FORECAST (Horizontal glass scrolling blocks)
            Text(
                text = "Chronology Hourly Forecast",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_forecast_row")
            ) {
                items(hourlyList) { forecast ->
                    Box(
                        modifier = Modifier
                            .testTag("hourly_card_${forecast.time}")
                            .width(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = forecast.time,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            DynamicWeatherIcon(
                                condition = forecast.condition.main,
                                iconSize = 28.dp
                            )

                            Text(
                                text = "${forecast.temp.toInt()}°",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (forecast.chanceOfRain > 0) {
                                Text(
                                    text = "${forecast.chanceOfRain}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF52B3D9)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(13.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. CHARTS (Continuous spline curves)
            InteractiveSplineChart(hourly = hourlyList, tempUnit = settings.tempUnit)

            Spacer(modifier = Modifier.height(24.dp))

            // 5. METEOROLOGY CARD GRID (Humidity, Wind Speed, Pressure, Visibility, UV Index, Air Quality, Sunrise/Sunset, Moon phase, Dew point)
            Text(
                text = "Atmospheric Variables",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "UV INDEX",
                            value = "${weather.uvIndex}",
                            icon = Icons.Outlined.WbSunny,
                            details = if (weather.uvIndex > 5) "High. Apply SPF." else "Low, safe levels.",
                            testTag = "metric_uv"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "HUMIDITY",
                            value = "${weather.humidity}%",
                            icon = Icons.Outlined.WaterDrop,
                            details = "Dew point is ${weather.dewPoint.toInt()}°.",
                            testTag = "metric_humidity"
                        )
                    }
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "WIND",
                            value = "${weather.windSpeed.toInt()} ${settings.windUnit}",
                            icon = Icons.Outlined.Air,
                            details = "Gusts up to ${(weather.windSpeed * 1.2f).toInt()} units.",
                            testTag = "metric_wind"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "PRESSURE",
                            value = "${weather.pressure} hPa",
                            icon = Icons.Outlined.Speed,
                            details = "Barometric density is high.",
                            testTag = "metric_pressure"
                        )
                    }
                }

                // Row 3 (Sunrise / Sunset / Moon phase)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "AIR QUALITY",
                            value = weather.airQuality,
                            icon = Icons.Outlined.FilterHdr,
                            details = "Localized AQI is stable.",
                            testTag = "metric_aqi"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "VISIBILITY",
                            value = "${weather.visibility.toInt()} km",
                            icon = Icons.Outlined.Visibility,
                            details = "Atmosphere clear today.",
                            testTag = "metric_visibility"
                        )
                    }
                }

                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "MOON PHASE",
                            value = weather.moonPhase,
                            icon = Icons.Outlined.Brightness3,
                            details = "Observational tracking.",
                            testTag = "metric_moon"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricGridCell(
                            label = "ASTRONOMY",
                            value = "Sunrise 05:42",
                            icon = Icons.Outlined.HourglassEmpty,
                            details = "Sunset sets at 20:14.",
                            testTag = "metric_sunrise"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. 10 DAY FORECAST BOARD (Premium Spacing List)
            Text(
                text = "10-Day Atmospheric Outlook",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            FrostedGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forecast_10_day_card"),
                cornerRadius = 28.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    dailyList.forEach { daily ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_forecast_row_${daily.dayOfWeek}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = daily.dayOfWeek,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.width(90.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DynamicWeatherIcon(
                                    condition = daily.condition.main,
                                    iconSize = 24.dp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                if (daily.chanceOfRain > 0) {
                                    Text(
                                        text = "${daily.chanceOfRain}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF52B3D9)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(24.dp))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.width(90.dp)
                            ) {
                                Text(
                                    text = "${daily.low.toInt()}°",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${daily.high.toInt()}°",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. WIDGET PLAYGROUND HUDS
            WidgetPlayground(weather = weather, tempUnit = settings.tempUnit)

            Spacer(modifier = Modifier.height(110.dp)) // Spacer for suspended glass dock
        }
    }
}
