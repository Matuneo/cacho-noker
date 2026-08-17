package com.matuneo.bloqueonacional.game

object Board {
    const val WIDTH = 1920f
    const val HEIGHT = 1080f
    const val START_X = 300f
    const val TOP_Y = 235f
    const val CELL_WIDTH = 190f
    const val LANE_HEIGHT = 137f
    const val COLUMNS = 7
    const val LANES = 5

    fun defenderX(column: Int): Float = START_X + column * CELL_WIDTH + CELL_WIDTH / 2f
    fun laneY(row: Int): Float = TOP_Y + row * LANE_HEIGHT + LANE_HEIGHT / 2f
}

enum class ScreenMode { MENU, LEVELS, HELP, PLAYING, PAUSED, WON, LOST }

enum class DefenderType(
    val title: String,
    val shortName: String,
    val description: String,
    val cost: Int,
    val health: Float,
    val damage: Float,
    val actionEvery: Float,
    val cardCooldown: Float
) {
    API(
        "Caserita del api", "API", "Genera 30 puntos de apoyo cada seis segundos.",
        50, 95f, 0f, 6f, 4f
    ),
    BLOCKER(
        "Bloqueador de llantas", "LLANTA", "Barricada resistente que detiene el avance.",
        70, 430f, 0f, 0f, 8f
    ),
    STUDENT(
        "Universitario", "U", "Lanza volantes que reducen la firmeza policial.",
        90, 130f, 24f, 1.55f, 5f
    ),
    DRUMMER(
        "Marchista del bombo", "BOMBO", "Sus ondas frenan a las capibaras cercanas.",
        110, 170f, 14f, 2.25f, 7f
    ),
    CHOLITA(
        "Cholita resistente", "CHOLITA", "Defensora ágil con lluvia de volantes.",
        135, 260f, 19f, 1.15f, 8f
    ),
    MEGAPHONE(
        "Dirigente megáfono", "MEGA", "Proyecta consignas de gran alcance y potencia.",
        160, 150f, 46f, 2.65f, 10f
    )
}

enum class EnemyType(
    val title: String,
    val health: Float,
    val speed: Float,
    val damage: Float,
    val attackEvery: Float,
    val reward: Int
) {
    PATROL("Capibara patrullera", 105f, 38f, 24f, 1.25f, 10),
    SHIELD("Capibara escudera", 260f, 24f, 28f, 1.35f, 18),
    MOTO("Capibara motorizada", 135f, 68f, 20f, 0.95f, 16),
    WATER("Capibara aguatera", 190f, 31f, 38f, 1.7f, 20),
    COMMANDER("Comandante capibara", 520f, 21f, 48f, 1.25f, 45)
}

enum class ProjectileKind { PAPER, SOUND, SLOGAN, CONFETTI }
enum class SoundKind { NONE, PLACE, HIT, RESOURCE, WIN, LOSE }

data class Defender(
    val id: Int,
    val type: DefenderType,
    val row: Int,
    val column: Int,
    val health: Float = type.health,
    val actionTimer: Float = type.actionEvery
)

data class Enemy(
    val id: Int,
    val type: EnemyType,
    val row: Int,
    val x: Float = 1885f,
    val health: Float = type.health,
    val attackTimer: Float = type.attackEvery,
    val slowTimer: Float = 0f
)

data class Projectile(
    val id: Int,
    val row: Int,
    val x: Float,
    val damage: Float,
    val speed: Float,
    val kind: ProjectileKind
)

data class LevelSpec(
    val number: Int,
    val title: String,
    val location: String,
    val description: String,
    val enemyCount: Int,
    val spawnEvery: Float,
    val enemyBoost: Float,
    val skyTop: Long,
    val skyBottom: Long
)

object GameContent {
    val levels = listOf(
        LevelSpec(1, "El primer bloqueo", "Mercado del Valle", "Aprende a organizar una defensa pacífica.", 9, 3.3f, 0.92f, 0xFF4FC3F7, 0xFFFFF3C4),
        LevelSpec(2, "Cruce decisivo", "Ruta del Altiplano", "El viento acelera la llegada de patrullas.", 12, 3.0f, 0.98f, 0xFF42A5F5, 0xFFFDE6A5),
        LevelSpec(3, "La gran avenida", "Distrito del Oriente", "Aparecen capibaras motorizadas.", 15, 2.8f, 1.04f, 0xFF26C6DA, 0xFFFFECB3),
        LevelSpec(4, "Puente de unidad", "Valles Centrales", "Protege cada carril y administra el apoyo.", 18, 2.6f, 1.08f, 0xFF64B5F6, 0xFFFFCC80),
        LevelSpec(5, "Terminal popular", "Ciudad de los Encuentros", "Las escuderas resisten más consignas.", 21, 2.45f, 1.12f, 0xFF5C6BC0, 0xFFFFB74D),
        LevelSpec(6, "Noche de vigilia", "Plaza de la Unidad", "Una marcha nocturna de gran intensidad.", 24, 2.3f, 1.17f, 0xFF172554, 0xFF7C3F58),
        LevelSpec(7, "Ruta de la cumbre", "Cordillera del Cóndor", "La presión crece mientras llegan refuerzos.", 27, 2.15f, 1.21f, 0xFF3949AB, 0xFFFFA726),
        LevelSpec(8, "Cabildo abierto", "Anillo Metropolitano", "Combina todas las unidades disponibles.", 30, 2.0f, 1.26f, 0xFF1565C0, 0xFFFFD180),
        LevelSpec(9, "La marcha final", "Ciudad del Gran Cóndor", "Llega hasta el palacio ficticio sin perder la moral.", 34, 1.85f, 1.31f, 0xFF283593, 0xFFE57373)
    )

    fun enemyFor(level: Int, spawned: Int, total: Int): EnemyType {
        if (spawned == total - 1 && level >= 3) return EnemyType.COMMANDER
        return when {
            level >= 6 && spawned % 9 == 7 -> EnemyType.WATER
            level >= 3 && spawned % 7 == 4 -> EnemyType.MOTO
            level >= 2 && spawned % 4 == 3 -> EnemyType.SHIELD
            else -> EnemyType.PATROL
        }
    }
}

data class GameState(
    val mode: ScreenMode = ScreenMode.MENU,
    val levelIndex: Int = 0,
    val unlockedLevel: Int = 1,
    val selected: DefenderType = DefenderType.STUDENT,
    val support: Int = 180,
    val morale: Int = 100,
    val pressure: Int = 0,
    val spawned: Int = 0,
    val defeated: Int = 0,
    val spawnTimer: Float = 1.8f,
    val elapsed: Float = 0f,
    val passiveTimer: Float = 3.5f,
    val defenders: List<Defender> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val cooldowns: Map<DefenderType, Float> = emptyMap(),
    val message: String = "Organiza los cinco carriles",
    val soundSerial: Int = 0,
    val soundKind: SoundKind = SoundKind.NONE,
    val bestScore: Int = 0
) {
    val level: LevelSpec get() = GameContent.levels[levelIndex]
    val wave: Int get() = if (level.enemyCount == 0) 1 else (spawned * 5 / level.enemyCount).coerceIn(0, 4) + 1
}

