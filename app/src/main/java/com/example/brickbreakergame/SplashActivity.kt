package com.example.brickbreakergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brickbreakergame.ui.theme.BrickBreakerGameTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import android.content.Intent

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrickBreakerGameTheme {
                SplashScreen(
                    onStart = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onStart: () -> Unit, onSettings: () -> Unit, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App name at the top
            Box(
                modifier = Modifier
                    .padding(bottom = 64.dp, top = 32.dp)
            ) {
                Text(
                    text = "BRICK BREAKER",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00FF99), // bright green
                    letterSpacing = 4.sp,
                    // Optionally, use a shadow for more pop
                    // style = TextStyle(shadow = Shadow(Color.Black, Offset(2f,2f), 4f))
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 3D Start Button
            ThreeDButton(text = "Start", onClick = onStart, gradient = listOf(Color(0xFF43EA7F), Color(0xFF11998E)))
            Spacer(modifier = Modifier.height(24.dp))
            // 3D Settings Button
            ThreeDButton(
                text = "Settings",
                onClick = onSettings,
                gradient = listOf(Color(0xFFB7F8DB), Color(0xFF50C878)),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            // 3D Exit Button
            ThreeDButton(text = "Exit", onClick = onExit, gradient = listOf(Color(0xFF56AB2F), Color(0xFFA8E063)))
        }
    }
}

@Composable
fun ThreeDButton(text: String, onClick: () -> Unit, gradient: List<Color>, icon: (@Composable (() -> Unit))? = null) {
    Box(
        modifier = Modifier
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = gradient),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            )
            .then(Modifier)
            .clickable { onClick() }
            .padding(horizontal = 48.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    BrickBreakerGameTheme {
        Greeting("Android")
    }
}