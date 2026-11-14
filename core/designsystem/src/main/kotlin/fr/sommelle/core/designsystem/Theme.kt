package fr.sommelle.core.designsystem

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/** Palette de documentation/design.md, section 9. Contrastes de texte vérifiés à 4,5:1 minimum. */
object SommelleColors {
    val Night = Color(0xFF2F3A6B)
    val Cream = Color(0xFFF6F3EE)
    val Paper = Color(0xFFFFFDF9)
    val Ink = Color(0xFF1E2233)
    val InkMuted = Color(0xFF4A4F63)
    val Lavender = Color(0xFFE6E4F2)
    val LavenderOnNight = Color(0xFFD9DCEB)
    val Line = Color(0xFFE2DDD3)
    val ChipBorder = Color(0xFFCFC8BA)
    val Amber = Color(0xFFE8A33D)
    val Moon = Color(0xFFF2C27B)

    // Thème de nuit : sombre et chaud, sans bleu.
    val Ember = Color(0xFF120D0A)
    val EmberSurface = Color(0xFF1E1611)
    val EmberContainer = Color(0xFF2A2019)
    val Sand = Color(0xFFC9A98A)
    val SandMuted = Color(0xFF9C8169)
    val EmberOutline = Color(0xFF6B5747)
    val AmberDim = Color(0xFFB9853A)
}

private val DayColors: ColorScheme = lightColorScheme(
    primary = SommelleColors.Night,
    onPrimary = SommelleColors.Cream,
    primaryContainer = SommelleColors.Lavender,
    onPrimaryContainer = SommelleColors.Night,
    secondary = SommelleColors.Amber,
    onSecondary = SommelleColors.Ink,
    background = SommelleColors.Cream,
    onBackground = SommelleColors.Ink,
    surface = SommelleColors.Paper,
    onSurface = SommelleColors.Ink,
    surfaceVariant = SommelleColors.Lavender,
    onSurfaceVariant = SommelleColors.InkMuted,
    outline = SommelleColors.ChipBorder,
    outlineVariant = SommelleColors.Line,
)

private val NightColors: ColorScheme = darkColorScheme(
    primary = SommelleColors.Sand,
    onPrimary = SommelleColors.Ember,
    primaryContainer = SommelleColors.EmberContainer,
    onPrimaryContainer = SommelleColors.Sand,
    secondary = SommelleColors.AmberDim,
    onSecondary = SommelleColors.Ember,
    background = SommelleColors.Ember,
    onBackground = SommelleColors.Sand,
    surface = SommelleColors.EmberSurface,
    onSurface = SommelleColors.Sand,
    surfaceVariant = SommelleColors.EmberContainer,
    onSurfaceVariant = SommelleColors.SandMuted,
    outline = SommelleColors.EmberOutline,
    outlineVariant = SommelleColors.EmberContainer,
)

/** Atkinson Hyperlegible Next (Braille Institute, licence SIL OFL 1.1, voir FONT-LICENSE.txt). */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_bold, FontWeight.Bold),
    Font(R.font.atkinson_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int = (size * 1.3).toInt()) = TextStyle(
    fontFamily = Atkinson,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

/** Corps de texte à 18 sp par défaut, jamais sous 15 sp (documentation/design.md, section 9). */
private val SommelleTypography = Typography(
    displayLarge = style(64, FontWeight.ExtraBold, 70),
    displayMedium = style(44, FontWeight.ExtraBold),
    displaySmall = style(36, FontWeight.ExtraBold),
    headlineLarge = style(34, FontWeight.ExtraBold),
    headlineMedium = style(30, FontWeight.ExtraBold),
    headlineSmall = style(24, FontWeight.Bold),
    titleLarge = style(21, FontWeight.Bold),
    titleMedium = style(19, FontWeight.Bold),
    titleSmall = style(17, FontWeight.Bold),
    bodyLarge = style(19, FontWeight.Normal, 28),
    bodyMedium = style(18, FontWeight.Normal, 26),
    bodySmall = style(16, FontWeight.Normal, 23),
    labelLarge = style(20, FontWeight.ExtraBold),
    labelMedium = style(16, FontWeight.SemiBold),
    labelSmall = style(15, FontWeight.SemiBold),
)

private val SommelleShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

enum class NightThemeMode {
    AUTO_HOURS,
    ALWAYS_DAY,
    ALWAYS_NIGHT,
    FOLLOW_SYSTEM,
}

data class NightThemeConfig(
    val mode: NightThemeMode = NightThemeMode.AUTO_HOURS,
    val startHour: Int = 21,
    val endHour: Int = 7,
) {
    fun isNightActive(now: LocalTime, systemDark: Boolean): Boolean = when (mode) {
        NightThemeMode.AUTO_HOURS -> isNightHours(now, startHour, endHour)
        NightThemeMode.ALWAYS_DAY -> false
        NightThemeMode.ALWAYS_NIGHT -> true
        NightThemeMode.FOLLOW_SYSTEM -> systemDark
    }
}

object NightThemeStore {
    private const val PREFS_NAME = "sommelle_theme_prefs"
    private const val KEY_MODE = "night_mode"
    private const val KEY_START_HOUR = "night_start_hour"
    private const val KEY_END_HOUR = "night_end_hour"

    private val _config = MutableStateFlow(NightThemeConfig())
    val config: StateFlow<NightThemeConfig> = _config

    @Volatile
    private var loaded = false

    fun load(context: Context): NightThemeConfig {
        if (!loaded) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val modeName = prefs.getString(KEY_MODE, NightThemeMode.AUTO_HOURS.name)
            val mode = NightThemeMode.entries.firstOrNull { it.name == modeName } ?: NightThemeMode.AUTO_HOURS
            val start = prefs.getInt(KEY_START_HOUR, 21).coerceIn(0, 23)
            val end = prefs.getInt(KEY_END_HOUR, 7).coerceIn(0, 23)
            _config.value = NightThemeConfig(mode, start, end)
            loaded = true
        }
        return _config.value
    }

    fun update(context: Context, transform: (NightThemeConfig) -> NightThemeConfig) {
        val current = load(context)
        val next = transform(current).let {
            it.copy(
                startHour = ((it.startHour % 24) + 24) % 24,
                endHour = ((it.endHour % 24) + 24) % 24,
            )
        }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, next.mode.name)
            .putInt(KEY_START_HOUR, next.startHour)
            .putInt(KEY_END_HOUR, next.endHour)
            .apply()
        loaded = true
        _config.value = next
    }
}

/** Vrai si [time] est dans la plage nocturne [startHour]..[endHour]. */
fun isNightHours(time: LocalTime, startHour: Int = 21, endHour: Int = 7): Boolean {
    val start = LocalTime.of(startHour.coerceIn(0, 23), 0)
    val end = LocalTime.of(endHour.coerceIn(0, 23), 0)
    return if (start <= end) {
        time >= start && time < end
    } else {
        time >= start || time < end
    }
}

@Composable
fun SommelleTheme(
    night: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    NightThemeStore.load(context)
    val config by NightThemeStore.config.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val effectiveNight = night ?: config.isNightActive(LocalTime.now(), systemDark)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                val value = if (effectiveNight) 0 else mask
                window.insetsController?.setSystemBarsAppearance(value, mask)
            } else {
                @Suppress("DEPRECATION")
                val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (effectiveNight) {
                    window.decorView.systemUiVisibility and flags.inv()
                } else {
                    window.decorView.systemUiVisibility or flags
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (effectiveNight) NightColors else DayColors,
        typography = SommelleTypography,
        shapes = SommelleShapes,
        content = content,
    )
}
