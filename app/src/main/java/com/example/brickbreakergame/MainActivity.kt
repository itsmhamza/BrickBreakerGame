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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Star
import android.media.SoundPool
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Intent
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Use OnBackPressedDispatcher for back navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@MainActivity, SplashActivity::class.java))
                finish()
            }
        })
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
    val baseRows = 2 // Level 1 rows
    val cols = 5
    val spacing = 10f
    val brickHeight = 50f
    val ballRadius = 20f
    val paddleHeight = 20f

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var paddleWidth by remember { mutableStateOf(100f) }
    var paddleX by remember { mutableStateOf(0f) }
    var ballOffset by remember { mutableStateOf(Offset.Zero) }
    var ballSpeed by remember { mutableStateOf(Offset.Zero) }
    var bricks by remember { mutableStateOf(emptyList<RectBrick>()) }
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    val insets = WindowInsets.systemBars.asPaddingValues()
    val context = LocalContext.current
    // --- Read sound and vibration settings ---
    val prefs = remember { context.getSharedPreferences("game_settings", Context.MODE_PRIVATE) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("soundOn", true)) }
    var vibrationOn by remember { mutableStateOf(prefs.getBoolean("vibrationOn", true)) }

    // --- SoundPool for effects ---
    val soundPool = remember { SoundPool.Builder().setMaxStreams(2).build() }
    val brickSoundId = remember { mutableStateOf(0) }
    val gameOverSoundId = remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        brickSoundId.value = soundPool.load(context, R.raw.brick_hit, 1)
        gameOverSoundId.value = soundPool.load(context, R.raw.game_over, 1)
        onDispose { soundPool.release() }
    }
    // --- Vibration ---
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    // Persistent high scores
    fun loadHighScores(): List<Int> {
        val prefs = context.getSharedPreferences("high_scores", Context.MODE_PRIVATE)
        return (0 until 5).mapNotNull { prefs.getInt("score_$it", -1).takeIf { s -> s >= 0 } }
    }
    fun saveHighScores(scores: List<Int>) {
        val prefs = context.getSharedPreferences("high_scores", Context.MODE_PRIVATE)
        prefs.edit().apply {
            scores.take(5).forEachIndexed { i, s -> putInt("score_$i", s) }
            apply()
        }
    }
    var highScores by remember { mutableStateOf(loadHighScores()) }
    var isPaused by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(1) }
    var showLevelUp by remember { mutableStateOf(false) }
    var levelUpTimer by remember { mutableStateOf(0L) }
    var initialBallSpeed by remember { mutableStateOf(Offset.Zero) }

    // Pause game when app goes to background
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                isPaused = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun resetGame(level: Int = 1) {
        val screenWidth = canvasSize.width
        val screenHeight = canvasSize.height
        // Increase difficulty: more rows, faster ball, smaller paddle
        val levelRows = baseRows + (level - 1)
        paddleWidth = (screenWidth * 0.25f).coerceAtLeast(60f)
        paddleX = screenWidth / 2 - paddleWidth / 2
        ballOffset = Offset(screenWidth / 2, screenHeight / 2)
        val randomAngle = (-6..6).random()
        if (level == 1) {
            val baseSpeed = 12f
            initialBallSpeed = Offset(baseSpeed + randomAngle, -baseSpeed)
        }
        ballSpeed = initialBallSpeed
        score = if (level == 1) 0 else score
        lives = if (level == 1) 3 else lives
        isGameOver = false

        val brickWidth = (screenWidth - (cols + 1) * spacing) / cols
        bricks = List(levelRows * cols) { index ->
            val row = index / cols
            val col = index % cols
            val x = spacing + col * (brickWidth + spacing)
            val y = 100f + row * (brickHeight + spacing)
            RectBrick(Offset(x, y), true)
        }
    }

    fun updateHighScores(newScore: Int) {
        val updated = (highScores + newScore).sortedDescending().take(5)
        highScores = updated
        saveHighScores(updated)
    }

    // Game loop
    LaunchedEffect(canvasSize) {
        if (canvasSize == Size.Zero) return@LaunchedEffect
        resetGame(level)

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
                    if (soundOn) soundPool.play(brickSoundId.value, 1f, 1f, 1, 0, 1f)
                }

                // Brick collision
                var hitBrick = false
                bricks = bricks.map {
                    if (it.isVisible &&
                        ballOffset.x in it.offset.x..(it.offset.x + brickWidth) &&
                        ballOffset.y in it.offset.y..(it.offset.y + brickHeight)
                    ) {
                        ballSpeed = ballSpeed.copy(y = -ballSpeed.y)
                        score += 10
                        hitBrick = true
                        if (soundOn) soundPool.play(brickSoundId.value, 1f, 1f, 1, 0, 1f)
                        it.copy(isVisible = false)
                    } else it
                }

                // Level up: all bricks cleared
                if (bricks.all { !it.isVisible }) {
                    showLevelUp = true
                    levelUpTimer = System.currentTimeMillis()
                    level++
                    delay(1200L)
                    showLevelUp = false
                    resetGame(level)
                    continue
                }

                // Missed the ball
                if (ballOffset.y > screenHeight) {
                    lives--
                    if (lives <= 0) {
                        isGameOver = true
                        updateHighScores(score)
                        if (soundOn) soundPool.play(gameOverSoundId.value, 1f, 1f, 1, 0, 1f)
                        if (vibrationOn && vibrator != null) {
                            if (android.os.Build.VERSION.SDK_INT >= 26) {
                                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(400)
                            }
                        }
                    } else {
                        ballOffset = Offset(screenWidth / 2, screenHeight / 2)
                        // Do NOT reset ballSpeed here
                        // ballSpeed = Offset(4f, -4f)
                        if (soundOn) soundPool.play(gameOverSoundId.value, 1f, 1f, 1, 0, 1f)
                        if (vibrationOn && vibrator != null) {
                            if (android.os.Build.VERSION.SDK_INT >= 26) {
                                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(150)
                            }
                        }
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

        TopBar(score = score, lives = lives, isPaused = isPaused, level = level) {
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

                // 3D Ball
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFB0C4DE), Color(0xFF2C3E50)),
                        center = ballOffset,
                        radius = ballRadius * 1.2f
                    ),
                    radius = ballRadius,
                    center = ballOffset
                )
                // Ball highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = ballRadius * 0.3f,
                    center = ballOffset + Offset(ballRadius * 0.3f, -ballRadius * 0.3f)
                )
                // Ball shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = ballRadius * 1.1f,
                    center = ballOffset + Offset(0f, ballRadius * 0.7f)
                )

                // 3D Paddle (single brick at bottom)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0E0E0), Color(0xFF757575)),
                        startY = paddleY,
                        endY = paddleY + paddleHeight
                    ),
                    topLeft = Offset(paddleX, paddleY),
                    size = Size(paddleWidth, paddleHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                // Paddle highlight
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset(paddleX, paddleY),
                    size = Size(paddleWidth, paddleHeight * 0.3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                // Paddle shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.18f),
                    topLeft = Offset(paddleX, paddleY + paddleHeight),
                    size = Size(paddleWidth, 8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                // 3D Bricks (top)
                bricks.forEach {
                    if (it.isVisible) {
                        val brickTop = it.offset.y
                        val brickBottom = it.offset.y + brickHeight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFF176), Color(0xFFFBC02D), Color(0xFFB8860B)),
                                startY = brickTop,
                                endY = brickBottom
                            ),
                            topLeft = it.offset,
                            size = Size(brickWidth, brickHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                        // Brick highlight
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.18f),
                            topLeft = it.offset,
                            size = Size(brickWidth, brickHeight * 0.25f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                        // Brick shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.10f),
                            topLeft = it.offset + Offset(0f, brickHeight * 0.85f),
                            size = Size(brickWidth, brickHeight * 0.15f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                    }
                }
            }

            if (isGameOver) {
                Dialog(onDismissRequest = { /* Prevent dismiss */ }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF232526), Color(0xFF414345))
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                            )
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.Yellow,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Game Over",
                                fontSize = 32.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Your Score: $score",
                                fontSize = 22.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "Top 5 High Scores:",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            highScores.forEachIndexed { index, hs ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (index == 0) Color(0xFFFFD700) else Color(0xFFB0BEC5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${index + 1}. $hs",
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            // 3D Try Again Button
                            Box(
                                modifier = Modifier
                                    .shadow(12.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFFFFE082), Color(0xFFFFA000)),
                                            startY = 0f,
                                            endY = 100f
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        level = 1
                                        resetGame(1)
                                    }
                                    .padding(horizontal = 36.dp, vertical = 14.dp)
                            ) {
                                Box {
                                    // Top highlight
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                                    startY = 0f,
                                                    endY = 30f
                                                ),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                                            )
                                    )
                                    Text(
                                        text = "Try Again",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (showLevelUp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Level $level!",
                        fontSize = 32.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(score: Int, lives: Int, isPaused: Boolean, level: Int, onPauseToggle: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
        tileMode = TileMode.Clamp
    )
    val levelGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
        tileMode = TileMode.Clamp
    )
    val heartGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
        tileMode = TileMode.Clamp
    )
    val topScoreIcon = Icons.Filled.Star

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradient)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .shadow(4.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Score (Fancy)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = topScoreIcon,
                contentDescription = "Top Score",
                tint = Color.Yellow,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Score: $score",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color.Yellow)
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        // Level Badge
        Box(
            modifier = Modifier
                .background(brush = levelGradient, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Level $level",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
        // Play/Pause Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(brush = gradient, shape = androidx.compose.foundation.shape.CircleShape)
                .clickable { onPauseToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        // Lives (Fancy Hearts)
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(lives) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Life",
                    tint = Color.Red,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(22.dp)
                        .shadow(4.dp, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BrickBreakerGameTheme {
        // Preview with level 1
        GameScreen()
    }
}