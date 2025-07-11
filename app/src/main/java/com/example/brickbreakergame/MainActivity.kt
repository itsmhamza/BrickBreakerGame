package com.example.brickbreakergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brickbreakergame.ui.theme.BrickBreakerGameTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrickBreakerGameTheme {
                GameScreen()
            }
        }
    }
}


data class RectBrick(val offset: Offset, val isVisible: Boolean)

@Composable
fun GameScreen() {
    val rows = 8
    val cols = 5
    val spacing = 10f
    val brickHeight = 50f
    val ballRadius = 20f
    val paddleHeight = 20f

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var paddleWidth by remember { mutableStateOf(100f) }
    var paddleX by remember { mutableStateOf(0f) }
    var ballOffset by remember { mutableStateOf(Offset.Zero) }
    var ballSpeed by remember { mutableStateOf(Offset(8f, -8f)) }
    var bricks by remember { mutableStateOf(emptyList<RectBrick>()) }
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    val insets = WindowInsets.systemBars.asPaddingValues()
    var highScores by remember { mutableStateOf(listOf<Int>()) }
    var isPaused by remember { mutableStateOf(false) }

    fun resetGame() {
        val screenWidth = canvasSize.width
        val screenHeight = canvasSize.height
        paddleWidth = screenWidth * 0.25f
        paddleX = screenWidth / 2 - paddleWidth / 2
        ballOffset = Offset(screenWidth / 2, screenHeight / 2)
        val randomAngle = (-6..6).random()
        ballSpeed = Offset(7f + randomAngle, -7f)
        score = 0
        lives = 3
        isGameOver = false

        val brickWidth = (screenWidth - (cols + 1) * spacing) / cols
        bricks = List(rows * cols) { index ->
            val row = index / cols
            val col = index % cols
            val x = spacing + col * (brickWidth + spacing)
            val y = 100f + row * (brickHeight + spacing)
            RectBrick(Offset(x, y), true)
        }
    }

    // Game loop
    LaunchedEffect(canvasSize) {
        if (canvasSize == Size.Zero) return@LaunchedEffect
        resetGame()

        while (true) {
            delay(16L)
            if (!isPaused) {
                if (isGameOver) continue

                ballOffset += ballSpeed

                val screenWidth = canvasSize.width
                val screenHeight = canvasSize.height
                val brickWidth = (screenWidth - (cols + 1) * spacing) / cols

                // Wall collision
                if (ballOffset.x < 0 || ballOffset.x > screenWidth) {
                    ballSpeed = ballSpeed.copy(x = -ballSpeed.x)
                }
                if (ballOffset.y < 0) {
                    ballSpeed = ballSpeed.copy(y = -ballSpeed.y)
                }

                // Paddle collision
                val paddleY = screenHeight - 40f
                if (
                    ballOffset.y + ballRadius >= paddleY &&
                    ballOffset.y + ballRadius <= paddleY + paddleHeight &&
                    ballOffset.x in paddleX..(paddleX + paddleWidth)
                ) {
                    ballSpeed = ballSpeed.copy(y = -abs(ballSpeed.y))
                }

                // Brick collision
                bricks = bricks.map {
                    if (it.isVisible &&
                        ballOffset.x in it.offset.x..(it.offset.x + brickWidth) &&
                        ballOffset.y in it.offset.y..(it.offset.y + brickHeight)
                    ) {
                        ballSpeed = ballSpeed.copy(y = -ballSpeed.y)
                        score += 10
                        it.copy(isVisible = false)
                    } else it
                }

                // Missed the ball
                if (ballOffset.y > screenHeight) {
                    lives--
                    if (lives <= 0) {
                        isGameOver = true
                        highScores = (highScores + score)
                            .sortedDescending()
                            .take(5)
                    } else {
                        ballOffset = Offset(screenWidth / 2, screenHeight / 2)
                        ballSpeed = Offset(4f, -4f)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()
        .padding(
            top = insets.calculateTopPadding(),
            start = insets.calculateLeftPadding(LayoutDirection.Ltr),
            end = insets.calculateRightPadding(LayoutDirection.Ltr),
            bottom = insets.calculateBottomPadding()
        )
        .background(brush = Brush.verticalGradient(
            colors = listOf(Color.DarkGray, Color.Black))
    )) {

        TopBar(score = score, lives = lives, isPaused = isPaused) {
            isPaused = !isPaused
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        paddleX = (paddleX + dragAmount.x)
                            .coerceIn(0f, canvasSize.width - paddleWidth)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                canvasSize = size
                if (canvasSize == Size.Zero) return@Canvas

                val paddleY = size.height - 40f
                val brickWidth = (size.width - (cols + 1) * spacing) / cols

                drawCircle(Color.White, ballRadius, ballOffset)

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Cyan, Color.Blue),
                        start = Offset(paddleX, paddleY),
                        end = Offset(paddleX + paddleWidth, paddleY + paddleHeight)
                    ),
                    topLeft = Offset(paddleX, paddleY),
                    size = Size(paddleWidth, paddleHeight)
                )

                bricks.forEach {
                    if (it.isVisible) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Cyan, Color.Blue),
                                start = it.offset,
                                end = Offset(it.offset.x + brickWidth, it.offset.y + brickHeight)
                            ),
                            topLeft = it.offset,
                            size = Size(brickWidth, brickHeight)
                        )
                    }
                }
            }

            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)), // Semi-transparent background
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Game Over",
                            fontSize = 30.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Top Scores:", fontSize = 20.sp, color = Color.Yellow)
                        Spacer(modifier = Modifier.height(8.dp))

                        highScores.forEachIndexed { index, hs ->
                            Text(
                                "${index + 1}. $hs",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            // Reset game
                            resetGame()
                        }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(score: Int, lives: Int, isPaused: Boolean, onPauseToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Score: $score",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Pause/Play Icon
        Icon(
            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = if (isPaused) "Resume" else "Pause",
            tint = Color.Yellow,
            modifier = Modifier
                .size(32.dp)
                .clickable { onPauseToggle() }
        )

        // Lives
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(lives) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Life",
                    tint = Color.Red,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp)
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BrickBreakerGameTheme {
        GameScreen()
    }
}