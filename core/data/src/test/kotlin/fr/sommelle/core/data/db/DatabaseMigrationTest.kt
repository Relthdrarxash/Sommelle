package fr.sommelle.core.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rejoue la migration v1 -> v2 sur un vrai moteur SQLite, à partir d'une base v1 remplie.
 *
 * Deux garanties :
 * - le programme, l'agenda et les fenêtres de sommeil existants sont conservés à l'identique ;
 * - la base migrée est identique (colonnes, types, NOT NULL, clé primaire) à une base neuve créée
 *   par Room à partir du schéma exporté `schemas/.../2.json`.
 */
class DatabaseMigrationTest {

    private val latestSchema = File("schemas/fr.sommelle.core.data.db.SommelleDatabase/2.json")

    @Test
    fun `migration de v1 a v2 sans perte de donnees`() = withDatabase { conn, db ->
        createV1(db)
        db.execSQL(
            "INSERT INTO program VALUES (0, '2026-10-01', '07:00', 'FROM_25_TO_44', 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO diary_entries VALUES ('2026-10-02', '2026-10-01T23:00', '2026-10-01T23:15', 30, 2, 40, " +
                "'2026-10-02T06:30', '2026-10-02T06:45', 'FAIR', 'Nuit agitée', NULL)",
        )
        db.execSQL("INSERT INTO sleep_windows VALUES ('2026-10-08', '00:15', '07:00', 405, 1, NULL)")

        SommelleDatabase.MIGRATION_1_2.migrate(db)

        conn.query("SELECT * FROM program WHERE id = 0") { rs ->
            assertTrue(rs.next())
            assertEquals("2026-10-01", rs.getString("startDate"))
            assertEquals("FROM_25_TO_44", rs.getString("ageBand"))
            assertEquals(1, rs.getInt("safetyCriticalActivity"))
        }
        conn.query("SELECT * FROM diary_entries") { rs ->
            assertTrue(rs.next())
            assertEquals("2026-10-02", rs.getString("nightOf"))
            assertEquals(30, rs.getLong("sleepOnsetMinutes"))
            assertEquals("Nuit agitée", rs.getString("notes"))
        }
        conn.query("SELECT durationMinutes FROM sleep_windows") { rs ->
            assertTrue(rs.next())
            assertEquals(405, rs.getLong(1))
        }
        conn.query("SELECT COUNT(*) FROM settings") { rs -> assertEquals(0, rs.getInt(1)) }
    }

    @Test
    fun `la base migree correspond exactement au schema Room v2`() {
        val expected = withDatabase { conn, db ->
            createTablesSql(latestSchema).values.forEach(db::execSQL)
            tables(conn)
        }
        val migrated = withDatabase { conn, db ->
            createV1(db)
            SommelleDatabase.MIGRATION_1_2.migrate(db)
            tables(conn)
        }
        assertEquals(expected, migrated)
    }

    /** Schéma de la toute première version publiée : programme, agenda et fenêtres. */
    private fun createV1(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `program` (`id` INTEGER NOT NULL, `startDate` TEXT NOT NULL, " +
                "`wakeTime` TEXT, `ageBand` TEXT NOT NULL, `loudSnoringWithBreathingPauses` INTEGER NOT NULL, " +
                "`markedDaytimeSleepiness` INTEGER NOT NULL, `bipolarDisorder` INTEGER NOT NULL, " +
                "`epilepsy` INTEGER NOT NULL, `pregnancy` INTEGER NOT NULL, `safetyCriticalActivity` INTEGER NOT NULL, " +
                "`prefersNoRestriction` INTEGER NOT NULL, `shiftOrNightWork` INTEGER NOT NULL, " +
                "`externalNightDisruption` INTEGER NOT NULL, `lateScheduleSleepsWellOnceAsleep` INTEGER NOT NULL, " +
                "`expressedDistress` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (`nightOf` TEXT NOT NULL, `bedTime` TEXT NOT NULL, " +
                "`lightsOutTime` TEXT NOT NULL, `sleepOnsetMinutes` INTEGER NOT NULL, `awakeningCount` INTEGER NOT NULL, " +
                "`awakeMinutes` INTEGER NOT NULL, `finalWakeTime` TEXT NOT NULL, `outOfBedTime` TEXT NOT NULL, " +
                "`quality` TEXT NOT NULL, `notes` TEXT NOT NULL, `prefillSource` TEXT, PRIMARY KEY(`nightOf`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_windows` (`effectiveFrom` TEXT NOT NULL, `bedTime` TEXT NOT NULL, " +
                "`wakeTime` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `protocolVersion` INTEGER NOT NULL, " +
                "`change` TEXT, PRIMARY KEY(`effectiveFrom`))",
        )
    }
}

// Outils, sans Robolectric : SQLite JDBC derrière l'interface SupportSQLiteDatabase de Room.

private fun <T> withDatabase(block: (Connection, SupportSQLiteDatabase) -> T): T =
    DriverManager.getConnection("jdbc:sqlite::memory:").use { conn -> block(conn, conn.asSupportDb()) }

private fun Connection.asSupportDb(): SupportSQLiteDatabase =
    Proxy.newProxyInstance(
        SupportSQLiteDatabase::class.java.classLoader,
        arrayOf(SupportSQLiteDatabase::class.java),
    ) { _, method, args ->
        when (method.name) {
            "execSQL" -> createStatement().use { it.execute(args[0] as String) }.let { null }
            else -> throw UnsupportedOperationException("Non simulé dans le test : ${method.name}")
        }
    } as SupportSQLiteDatabase

private fun <T> Connection.query(sql: String, block: (ResultSet) -> T): T =
    createStatement().use { stmt -> stmt.executeQuery(sql).use(block) }

private fun tables(conn: Connection): Map<String, List<String>> {
    val names = conn.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name") { rs ->
        buildList { while (rs.next()) add(rs.getString(1)) }
    }
    return names.associateWith { table ->
        conn.query("PRAGMA table_info(`$table`)") { rs ->
            buildList { while (rs.next()) add("${rs.getString("name")} ${rs.getString("type")} notnull=${rs.getInt("notnull")} pk=${rs.getInt("pk")}") }
        }.sorted()
    }
}

private fun createTablesSql(schema: File): Map<String, String> {
    assertTrue(schema.exists(), "Schéma Room absent : ${schema.path}. Lancer une compilation pour l'exporter.")
    val regex = Regex("\"tableName\":\\s*\"([^\"]+)\",\\s*\"createSql\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
    return regex.findAll(schema.readText()).associate { match ->
        val table = match.groupValues[1]
        table to match.groupValues[2].replace("\\\"", "\"").replace("\${TABLE_NAME}", table)
    }
}
