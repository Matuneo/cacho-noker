package com.matuneo.bloqueonacional.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.max
import kotlin.math.min

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("bloqueo_nacional_progress", 0)
    private var nextId = 1

    var state by mutableStateOf(
        GameState(
            unlockedLevel = preferences.getInt("unlocked_level", 1).coerceIn(1, GameContent.levels.size),
            bestScore = preferences.getInt("best_score", 0)
        )
    )
        private set

    fun openLevels() {
        state = state.copy(mode = ScreenMode.LEVELS)
    }

    fun openHelp() {
        state = state.copy(mode = ScreenMode.HELP)
    }

    fun goToMenu() {
        state = state.copy(mode = ScreenMode.MENU, enemies = emptyList(), projectiles = emptyList())
    }

    fun startLevel(levelNumber: Int) {
        if (levelNumber !in 1..state.unlockedLevel) return
        nextId = 1
        state = GameState(
            mode = ScreenMode.PLAYING,
            levelIndex = levelNumber - 1,
            unlockedLevel = state.unlockedLevel,
            bestScore = state.bestScore,
            message = if (levelNumber == 1) "Elige una unidad y toca una casilla" else "La marcha continúa"
        )
    }

    fun retry() = startLevel(state.level.number)

    fun nextLevel() {
        val next = (state.level.number + 1).coerceAtMost(GameContent.levels.size)
        startLevel(next)
    }

    fun togglePause() {
        state = when (state.mode) {
            ScreenMode.PLAYING -> state.copy(mode = ScreenMode.PAUSED)
            ScreenMode.PAUSED -> state.copy(mode = ScreenMode.PLAYING)
            else -> state
        }
    }

    fun select(type: DefenderType) {
        state = state.copy(selected = type)
    }

    fun place(row: Int, column: Int): Boolean {
        val current = state
        val type = current.selected
        if (current.mode != ScreenMode.PLAYING) return false
        if (row !in 0 until Board.LANES || column !in 0 until Board.COLUMNS) return false
        if (current.defenders.any { it.row == row && it.column == column }) return false
        if ((current.cooldowns[type] ?: 0f) > 0f || current.support < type.cost) return false

        state = current.copy(
            support = current.support - type.cost,
            defenders = current.defenders + Defender(nextId++, type, row, column),
            cooldowns = current.cooldowns + (type to type.cardCooldown),
            message = "${type.title} lista en el carril ${row + 1}",
            soundSerial = current.soundSerial + 1,
            soundKind = SoundKind.PLACE
        )
        return true
    }

    fun update(deltaSeconds: Float) {
        if (state.mode != ScreenMode.PLAYING) return
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        val current = state
        val spec = current.level

        var support = current.support
        var morale = current.morale
        var defeated = current.defeated
        var spawned = current.spawned
        var spawnTimer = current.spawnTimer - dt
        var passiveTimer = current.passiveTimer - dt
        var soundSerial = current.soundSerial
        var soundKind = current.soundKind

        if (passiveTimer <= 0f) {
            support += 10
            passiveTimer += 3.5f
        }

        val cooldowns = current.cooldowns.mapValues { (_, value) -> max(0f, value - dt) }
        val defenders = current.defenders.associateBy { it.id }.toMutableMap()
        val enemies = current.enemies.associateBy { it.id }.toMutableMap()
        val projectiles = current.projectiles.toMutableList()

        if (spawned < spec.enemyCount && spawnTimer <= 0f) {
            val enemyType = GameContent.enemyFor(spec.number, spawned, spec.enemyCount)
            val row = ((spawned * 3) + spec.number * 2) % Board.LANES
            enemies[nextId] = Enemy(id = nextId++, type = enemyType, row = row)
            spawned += 1
            spawnTimer += spec.spawnEvery
        }

        defenders.values.toList().forEach { defender ->
            var actionTimer = defender.actionTimer - dt
            if (defender.type == DefenderType.API) {
                if (actionTimer <= 0f) {
                    support += 30
                    actionTimer += defender.type.actionEvery
                    soundSerial += 1
                    soundKind = SoundKind.RESOURCE
                }
            } else if (defender.type.damage > 0f && actionTimer <= 0f) {
                val originX = Board.defenderX(defender.column)
                val hasTarget = enemies.values.any { it.row == defender.row && it.x > originX - 30f }
                if (hasTarget) {
                    val kind = when (defender.type) {
                        DefenderType.DRUMMER -> ProjectileKind.SOUND
                        DefenderType.MEGAPHONE -> ProjectileKind.SLOGAN
                        DefenderType.CHOLITA -> ProjectileKind.CONFETTI
                        else -> ProjectileKind.PAPER
                    }
                    val speed = when (kind) {
                        ProjectileKind.SOUND -> 390f
                        ProjectileKind.SLOGAN -> 610f
                        ProjectileKind.CONFETTI -> 570f
                        ProjectileKind.PAPER -> 520f
                    }
                    projectiles += Projectile(nextId++, defender.row, originX + 50f, defender.type.damage, speed, kind)
                    actionTimer += defender.type.actionEvery
                } else {
                    actionTimer = 0f
                }
            }
            defenders[defender.id] = defender.copy(actionTimer = actionTimer)
        }

        enemies.values.toList().forEach { enemy ->
            if (!enemies.containsKey(enemy.id)) return@forEach
            val slowedFor = max(0f, enemy.slowTimer - dt)
            val blockingDefender = defenders.values
                .filter { it.row == enemy.row }
                .filter {
                    val defenderX = Board.defenderX(it.column)
                    enemy.x <= defenderX + 115f && enemy.x >= defenderX - 75f
                }
                .maxByOrNull { Board.defenderX(it.column) }

            if (blockingDefender != null) {
                var attackTimer = enemy.attackTimer - dt
                if (attackTimer <= 0f) {
                    val remaining = blockingDefender.health - enemy.type.damage
                    if (remaining <= 0f) defenders.remove(blockingDefender.id)
                    else defenders[blockingDefender.id] = blockingDefender.copy(health = remaining)
                    attackTimer += enemy.type.attackEvery
                }
                enemies[enemy.id] = enemy.copy(attackTimer = attackTimer, slowTimer = slowedFor)
            } else {
                val slowFactor = if (slowedFor > 0f) 0.48f else 1f
                val movedX = enemy.x - enemy.type.speed * spec.enemyBoost * slowFactor * dt
                if (movedX < 125f) {
                    enemies.remove(enemy.id)
                    morale -= 20
                    soundSerial += 1
                    soundKind = SoundKind.LOSE
                } else {
                    enemies[enemy.id] = enemy.copy(x = movedX, attackTimer = enemy.type.attackEvery, slowTimer = slowedFor)
                }
            }
        }

        val remainingProjectiles = mutableListOf<Projectile>()
        projectiles.forEach { projectile ->
            val moved = projectile.copy(x = projectile.x + projectile.speed * dt)
            val hit = enemies.values
                .filter { it.row == projectile.row }
                .filter { it.x >= projectile.x - 18f && it.x <= moved.x + 62f }
                .minByOrNull { it.x }

            if (hit != null) {
                val health = hit.health - projectile.damage
                if (health <= 0f) {
                    enemies.remove(hit.id)
                    defeated += 1
                    support += hit.type.reward
                    soundSerial += 1
                    soundKind = SoundKind.HIT
                } else {
                    val extraSlow = if (projectile.kind == ProjectileKind.SOUND) 2.2f else hit.slowTimer
                    enemies[hit.id] = hit.copy(health = health, slowTimer = max(hit.slowTimer, extraSlow))
                }
            } else if (moved.x < Board.WIDTH + 70f) {
                remainingProjectiles += moved
            }
        }

        val pressure = if (spec.enemyCount == 0) 0 else (defeated * 100 / spec.enemyCount).coerceIn(0, 100)
        var mode = ScreenMode.PLAYING
        var message = "Oleada ${if (spec.enemyCount == 0) 1 else (spawned * 5 / spec.enemyCount).coerceIn(0, 4) + 1} de 5"
        var unlocked = current.unlockedLevel
        var bestScore = current.bestScore

        if (morale <= 0) {
            morale = 0
            mode = ScreenMode.LOST
            message = "Las capibaras despejaron los accesos"
            soundSerial += 1
            soundKind = SoundKind.LOSE
        } else if (spawned >= spec.enemyCount && enemies.isEmpty()) {
            mode = ScreenMode.WON
            message = if (spec.number == GameContent.levels.size) {
                "La presión social logra la renuncia del presidente ficticio"
            } else {
                "La movilización avanzó hacia la siguiente zona"
            }
            unlocked = max(unlocked, min(spec.number + 1, GameContent.levels.size))
            bestScore = max(bestScore, support + morale * 12 + pressure * 20)
            preferences.edit()
                .putInt("unlocked_level", unlocked)
                .putInt("best_score", bestScore)
                .apply()
            soundSerial += 1
            soundKind = SoundKind.WIN
        }

        state = current.copy(
            mode = mode,
            support = support.coerceAtMost(9999),
            morale = morale,
            pressure = pressure,
            spawned = spawned,
            defeated = defeated,
            spawnTimer = spawnTimer,
            elapsed = current.elapsed + dt,
            passiveTimer = passiveTimer,
            defenders = defenders.values.toList(),
            enemies = enemies.values.toList(),
            projectiles = remainingProjectiles,
            cooldowns = cooldowns,
            message = message,
            unlockedLevel = unlocked,
            bestScore = bestScore,
            soundSerial = soundSerial,
            soundKind = soundKind
        )
    }
}

