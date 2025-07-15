package com.example.brickbreakergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brickbreakergame.ui.theme.BrickBreakerGameTheme
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Brush

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrickBreakerGameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_settings", Context.MODE_PRIVATE) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("soundOn", true)) }
    var vibrationOn by remember { mutableStateOf(prefs.getBoolean("vibrationOn", true)) }
    var selectedDifficulty by remember { mutableStateOf("Normal") }
    val difficulties = listOf("Easy", "Normal", "Hard")
    val accentGreen = Color(0xFF11998E)
    val accentGreenLight = Color(0xFF43EA7F)

    fun saveSettings() {
        prefs.edit().putBoolean("soundOn", soundOn).putBoolean("vibrationOn", vibrationOn).apply()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image with overlay
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        ) {}
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // No white box background, just content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Settings", fontSize = 28.sp, color = accentGreen, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                // Sound
                RowSetting(label = "Sound", checked = soundOn, onCheckedChange = {
                    soundOn = it
                    saveSettings()
                }, accentColor = accentGreenLight)
                Spacer(modifier = Modifier.height(8.dp))
                // Vibration
                RowSetting(label = "Vibration", checked = vibrationOn, onCheckedChange = {
                    vibrationOn = it
                    saveSettings()
                }, accentColor = accentGreenLight)
                Spacer(modifier = Modifier.height(8.dp))
                // Difficulty
                Text("Difficulty", fontSize = 20.sp, color = accentGreen, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                difficulties.forEach { diff ->
                    RowRadioSetting(
                        label = diff,
                        selected = selectedDifficulty == diff,
                        onClick = { selectedDifficulty = diff },
                        accentColor = accentGreenLight
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                FancyBackButton(onClick = onBack, accentColor = accentGreen)
            }
        }
    }
}

@Composable
fun RowSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, accentColor: Color) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp),
            maxLines = 1
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 16.dp),
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun RowRadioSetting(label: String, selected: Boolean, onClick: () -> Unit, accentColor: Color) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
                unselectedColor = Color.White
            )
        )
        Text(label, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
fun FancyBackButton(onClick: () -> Unit, accentColor: Color) {
    Box(
        modifier = Modifier
            .shadow(8.dp, shape = RoundedCornerShape(18.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, Color(0xFF43EA7F))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 36.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Back",
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 18.sp
        )
    }
} 