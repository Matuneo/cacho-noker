package com.matuneo.bloqueonacional

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matuneo.bloqueonacional.game.DefenderType
import com.matuneo.bloqueonacional.game.GameBoard
import com.matuneo.bloqueonacional.game.GameContent
import com.matuneo.bloqueonacional.game.GameState
import com.matuneo.bloqueonacional.game.GameViewModel
import com.matuneo.bloqueonacional.game.MenuBackdrop
import com.matuneo.bloqueonacional.game.ScreenMode
import com.matuneo.bloqueonacional.game.SoundKind
import kotlinx.coroutines.isActive
import kotlin.math.ceil

private val Ink = Color(0xFF071522)
private val Panel = Color(0xE6112740)
private val Gold = Color(0xFFFFD54F)
private val Cream = Color(0xFFFFF4D6)
private val Green = Color(0xFF49C76D)
private val Red = Color(0xFFE95D56)

@Composable
fun BloqueoNacionalApp(game: GameViewModel = viewModel()) {
    val state = game.state
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 42) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(state.soundSerial) {
        if (state.soundSerial == 0) return@LaunchedEffect
        when (state.soundKind) {
            SoundKind.PLACE -> tone.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            SoundKind.HIT -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 70)
            SoundKind.RESOURCE -> tone.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
            SoundKind.WIN -> tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)
            SoundKind.LOSE -> tone.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 260)
            SoundKind.NONE -> Unit
        }
    }

    LaunchedEffect(state.mode) {
        if (state.mode != ScreenMode.PLAYING) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (isActive && game.state.mode == ScreenMode.PLAYING) {
            withFrameNanos { now ->
                game.update((now - previous) / 1_000_000_000f)
                previous = now
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        when (state.mode) {
            ScreenMode.MENU -> MainMenu(state, game::openLevels, game::openHelp)
            ScreenMode.LEVELS -> LevelSelection(state, game::startLevel, game::goToMenu)
            ScreenMode.HELP -> HelpScreen(game::goToMenu)
            ScreenMode.PLAYING, ScreenMode.PAUSED, ScreenMode.WON, ScreenMode.LOST -> GamePlay(state, game)
        }
    }
}

@Composable
private fun MainMenu(state: GameState, onPlay: () -> Unit, onHelp: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "menu")
    val movement by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "background"
    )
    Box(Modifier.fillMaxSize()) {
        MenuBackdrop(movement)
        Box(Modifier.fillMaxSize().background(Color(0x55061520)))
        Column(
            modifier = Modifier.fillMaxHeight().width(620.dp).padding(start = 56.dp, top = 34.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text("JUEGO DE ESTRATEGIA · SÁTIRA FICTICIA", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "BLOQUEO\nNACIONAL",
                color = Cream,
                fontSize = 58.sp,
                lineHeight = 54.sp,
                fontWeight = FontWeight.Black
            )
            Text("LA MARCHA FINAL", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))
            Text(
                "Organiza bloqueadores, manifestantes y marchistas. Defiende los carriles de las capibaras policiales y eleva la presión social.",
                color = Color(0xFFE1E8ED),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.width(500.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier.width(280.dp).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("JUGAR", fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onHelp, modifier = Modifier.width(280.dp).height(48.dp), border = BorderStroke(1.dp, Cream)) {
                Icon(Icons.Default.Help, null)
                Spacer(Modifier.width(8.dp))
                Text("CÓMO JUGAR", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Text("Niveles desbloqueados: ${state.unlockedLevel}/9  ·  Mejor puntaje: ${state.bestScore}", color = Color(0xFFB9C8D2), fontSize = 13.sp)
            Text("Obra humorística. No representa autoridades, partidos ni personas reales.", color = Color(0xFF92A5B4), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LevelSelection(state: GameState, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BrushBackground).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Cream) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("MAPA DE LA MOVILIZACIÓN", color = Cream, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Completa una zona para desbloquear la siguiente", color = Color(0xFFB8C8D5), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(GameContent.levels) { level ->
                val unlocked = level.number <= state.unlockedLevel
                Card(
                    modifier = Modifier.fillMaxWidth().height(142.dp).alpha(if (unlocked) 1f else 0.55f).clickable(enabled = unlocked) { onSelect(level.number) },
                    colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFF15344D) else Color(0xFF172634)),
                    border = BorderStroke(1.dp, if (unlocked) Color(0xFF3E6F8B) else Color(0xFF2B3945))
                ) {
                    Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (unlocked) Gold else Color(0xFF455A64), modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                if (unlocked) Text("${level.number}", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                                else Icon(Icons.Default.Lock, null, tint = Color(0xFFB0BEC5))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(level.title.uppercase(), color = Cream, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(level.location, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(level.description, color = Color(0xFFB8C8D5), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${level.enemyCount} capibaras · 5 oleadas", color = Gold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    Row(Modifier.fillMaxSize().background(BrushBackground).padding(28.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(0.9f)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Cream) }
            Text("CÓMO JUGAR", color = Cream, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("Una defensa por carriles de presión social no violenta", color = Green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            HelpStep("1", "Genera apoyo", "La caserita del api produce apoyo. También recibes una pequeña cantidad automáticamente.")
            HelpStep("2", "Organiza los carriles", "Selecciona una unidad y toca una casilla libre. No dejes ningún carril sin protección.")
            HelpStep("3", "Eleva la presión", "Las consignas reducen la firmeza de las capibaras. Cuando se retiran, aumenta la presión social.")
            HelpStep("4", "Conserva la moral", "Si cinco capibaras llegan al campamento, la movilización pierde el nivel.")
        }
        Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text("UNIDADES DISPONIBLES", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            DefenderType.entries.forEach { type ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(9.dp), color = unitColor(type), modifier = Modifier.size(46.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(type.shortName.take(3), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(type.description, color = Color(0xFFB8C8D5), fontSize = 11.sp)
                    }
                    Text("${type.cost}", color = Gold, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = Gold, modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = Ink, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(detail, color = Color(0xFFB8C8D5), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun GamePlay(state: GameState, game: GameViewModel) {
    val view = LocalView.current
    Box(Modifier.fillMaxSize()) {
        GameBoard(
            state = state,
            onCellTapped = { row, column ->
                game.place(row, column).also { placed ->
                    if (placed) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        )

        GameHud(state, game::togglePause)
        UnitTray(state, game::select)

        AnimatedVisibility(state.mode == ScreenMode.PAUSED) {
            ResultOverlay(
                title = "PAUSA",
                message = "La movilización está esperando tus instrucciones.",
                primary = "CONTINUAR",
                onPrimary = game::togglePause,
                secondary = "SALIR AL MENÚ",
                onSecondary = game::goToMenu
            )
        }
        AnimatedVisibility(state.mode == ScreenMode.WON) {
            ResultOverlay(
                title = if (state.level.number == GameContent.levels.size) "VICTORIA FINAL" else "ZONA CONQUISTADA",
                message = state.message + "\nPresión social: ${state.pressure}% · Moral: ${state.morale}%",
                primary = if (state.level.number == GameContent.levels.size) "VER MAPA" else "SIGUIENTE NIVEL",
                onPrimary = if (state.level.number == GameContent.levels.size) game::openLevels else game::nextLevel,
                secondary = "REPETIR",
                onSecondary = game::retry
            )
        }
        AnimatedVisibility(state.mode == ScreenMode.LOST) {
            ResultOverlay(
                title = "LA MORAL CAYÓ",
                message = "Reorganiza la generación de apoyo y refuerza los carriles más débiles.",
                primary = "REINTENTAR",
                onPrimary = game::retry,
                secondary = "SALIR AL MENÚ",
                onSecondary = game::goToMenu
            )
        }
    }
}

@Composable
private fun GameHud(state: GameState, onPause: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(67.dp), color = Color(0xE8071522)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HudChip("APOYO", "${state.support}", Gold)
            Spacer(Modifier.width(8.dp))
            HudChip("MORAL", "${state.morale}%", if (state.morale > 40) Green else Red)
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PRESIÓN SOCIAL", color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${state.pressure}%", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { state.pressure / 100f },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Gold,
                    trackColor = Color(0xFF29445A)
                )
                Text(state.message, color = Color(0xFFB8C8D5), fontSize = 9.sp, maxLines = 1)
            }
            Spacer(Modifier.width(15.dp))
            HudChip("OLEADA", "${state.wave}/5", Color(0xFF64B5F6))
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = Color(0xFF24445E)) {
                IconButton(onClick = onPause) { Icon(Icons.Default.Pause, "Pausa", tint = Cream) }
            }
        }
    }
}

@Composable
private fun HudChip(label: String, value: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF15344D), border = BorderStroke(1.dp, Color(0xFF315873))) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF9DB2C1), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.UnitTray(
    state: GameState,
    onSelect: (DefenderType) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().height(94.dp).align(Alignment.BottomCenter), color = Color(0xED071522)) {
        LazyRow(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(DefenderType.entries) { type ->
                val cooldown = state.cooldowns[type] ?: 0f
                val enabled = cooldown <= 0f && state.support >= type.cost && state.mode == ScreenMode.PLAYING
                val selected = state.selected == type
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Color(0xFF315C73) else Color(0xFF142D42),
                    border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Gold else Color(0xFF345269)),
                    modifier = Modifier.width(133.dp).fillMaxHeight().alpha(if (enabled) 1f else 0.58f).clickable(enabled = state.mode == ScreenMode.PLAYING) { onSelect(type) }
                ) {
                    Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = unitColor(type), modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(type.shortName.take(2), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp) }
                        }
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(type.shortName, color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text("★ ${type.cost}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (cooldown > 0f) Text("${ceil(cooldown).toInt()} s", color = Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    title: String,
    message: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xB8000710)), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(520.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102B43)),
            border = BorderStroke(2.dp, Gold),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(message, color = Cream, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                Spacer(Modifier.height(23.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onPrimary, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(5.dp))
                        Text(primary, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = onSecondary, border = BorderStroke(1.dp, Cream)) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(5.dp))
                        Text(secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val BrushBackground = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color(0xFF0A1C2D), Color(0xFF102E45), Color(0xFF071522))
)

private fun unitColor(type: DefenderType): Color = when (type) {
    DefenderType.API -> Color(0xFFD84315)
    DefenderType.BLOCKER -> Color(0xFF455A64)
    DefenderType.STUDENT -> Color(0xFF00838F)
    DefenderType.DRUMMER -> Color(0xFF7B1FA2)
    DefenderType.CHOLITA -> Color(0xFFC2185B)
    DefenderType.MEGAPHONE -> Color(0xFFB71C1C)
}
