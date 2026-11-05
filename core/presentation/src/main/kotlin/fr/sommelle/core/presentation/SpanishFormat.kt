package fr.sommelle.core.presentation

import fr.sommelle.core.model.AgeBand
import fr.sommelle.core.model.Notice
import fr.sommelle.core.model.Pathway
import fr.sommelle.core.model.Referral
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import kotlin.math.roundToInt

/**
 * Formato en español para horas, duraciones y fechas.
 */
object SpanishFormat : TextFormat {

    private const val NBSP = ' '

    override fun time(time: LocalTime): String {
        val minutes = time.minute
        val formattedMin = if (minutes < 10) "0$minutes" else "$minutes"
        return "${time.hour}:${formattedMin}"
    }

    override fun duration(duration: Duration): String {
        val total = duration.toMinutes()
        val hours = total / 60
        val minutes = total % 60
        return when {
            hours == 0L -> "$minutes${NBSP}min"
            minutes == 0L -> "${hours}h"
            else -> {
                val minStr = if (minutes < 10) "0$minutes" else "$minutes"
                "${hours}h${NBSP}${minStr}"
            }
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total < 60 -> "$total${NBSP}minuto${if (total > 1) "s" else ""}"
            total % 60 == 0L -> "${total / 60}${NBSP}hora${if (total >= 120) "s" else ""}"
            else -> duration(duration.abs())
        }
    }

    override fun percent(ratio: Double): String = "${(ratio * 100).roundToInt()}%"

    override fun dayName(day: DayOfWeek): String = DAYS[day.value - 1]

    override fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek).replaceFirstChar { it.uppercase() }}, ${date.dayOfMonth} de ${monthName(date.month)}"

    override fun nightLabel(nightOf: LocalDate): String =
        "Noche del ${dayName(nightOf.minusDays(1).dayOfWeek)} al ${dayName(nightOf.dayOfWeek)}"

    override fun shortNight(nightOf: LocalDate): String =
        "${dayName(nightOf.dayOfWeek).take(3)}. ${nightOf.dayOfMonth}/${nightOf.monthValue}"

    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} / 5"
    }

    override fun observationWeek() = "Semana de observación"
    override fun week(number: Int) = "Semana $number"
    override fun observation() = "Observación"
    override fun inProgress() = " (en curso)"
    override fun baseline() = "Al inicio"
    override fun noWindow() = "Ninguna"
    override fun noAwakening() = "Ninguno"
    override fun windowRange(bed: String, wake: String) = "$bed a $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "acostarse $bed, levantarse $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "Menor de 18 años"
        AgeBand.FROM_18_TO_24 -> "18 a 24 años"
        AgeBand.FROM_25_TO_44 -> "25 a 44 años"
        AgeBand.FROM_45_TO_64 -> "45 a 64 años"
        AgeBand.FROM_65 -> "65 años o más"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Programa completo, con restricción de tiempo en cama"
        Pathway.WITHOUT_RESTRICTION -> "Programa sin restricción de tiempo en cama"
        Pathway.OUT_OF_SCOPE -> "Fuera del ámbito del programa"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "Menor de 18 años: derivación médica requerida"
        Referral.SLEEP_APNEA_SUSPECTED -> "Ronquidos con pausas respiratorias: explorar posible apnea del sueño"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Somnolencia diurna excesiva o sueño involuntario declarados"
        Referral.BIPOLAR_DISORDER -> "Trastorno bipolar declarado: sin restricción de tiempo en cama"
        Referral.EPILEPSY -> "Epilepsia declarada: sin restricción de tiempo en cama"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Horarios muy tardíos con sueño normal: posible retraso de fase"
        Referral.SHIFT_WORK -> "Trabajo nocturno o a turnos declarado"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Actividad con riesgo de somnolencia: ventana mínima elevada a 6 h"
        Notice.EXTERNAL_DISRUPTION -> "Noches interrumpidas por terceros declaradas"
        Notice.PREGNANCY -> "Embarazo declarado: sin restricción de tiempo en cama"
        Notice.CRISIS_RESOURCES -> "Malestar anímico expresado: recursos de apoyo disponibles"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Tiempo en conciliar el sueño"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Tiempo despierto por la noche"
        OutcomeMetric.TOTAL_SLEEP -> "Tiempo total de sueño"
        OutcomeMetric.EFFICIENCY -> "Eficiencia del sueño"
        OutcomeMetric.QUALITY -> "Calidad percibida"
        OutcomeMetric.DAYTIME_IMPACT -> "Impacto en el día (de 0 a 4)"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "En mejoría"
        Trend.STABLE -> "Estable"
        Trend.WORSE -> "En descenso"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Evolución por semanas. " + bars.joinToString(" ") { "${it.label}: ${it.timeInBed} en la cama, ${it.totalSleep} dormido." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private val DAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val MONTHS = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )
}
