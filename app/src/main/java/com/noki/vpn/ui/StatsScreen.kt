package com.noki.vpn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noki.vpn.AppUiState
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.DailyStats
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val StatsBgBase = Color(0xFF07111A)
private val StatsBgLighter = Color(0xFF0D1B2A)
private val StatsBgSoft = Color(0xFF132635)
private val StatsTextPrimary = Color(0xFFF4FBFF)
private val StatsTextSecondary = Color(0xFF9FB6C5)
private val StatsAccent = Color(0xFF7AE7C7)
private val StatsStroke = Color(0xFF29404E)
private val StatsNoFontPaddingTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private enum class StatsPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
}

@Composable
fun StatsScreen(
    state: AppUiState,
    liveGlassEnabled: Boolean = true,
    showBackground: Boolean = true,
    onRefreshStats: () -> Unit = {},
) {
    val language = state.personalizationSettings.language
    var selectedPeriod by rememberSaveable { androidx.compose.runtime.mutableStateOf(StatsPeriod.WEEK) }

    LaunchedEffect(Unit) {
        onRefreshStats()
        while (true) {
            delay(60_000L)
            onRefreshStats()
        }
    }

    CompositionLocalProvider(LocalTextStyle provides StatsNoFontPaddingTextStyle) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBackground) Modifier.background(StatsBgBase) else Modifier)
                .statusBarsPadding(),
        ) {
            val metrics = nokiAdaptiveMetrics(maxWidth)
            val density = LocalDensity.current
            val panelX = metrics.contentStart
            val panelWidth = metrics.contentWidth
            val headerTop = 48.dp
            val titleLineHeight = 28.8.dp
            val subtitleTop = headerTop + 32.dp
            val contentTop = headerTop + 68.dp
            val navBarBottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val bottomNavTop = maxHeight - navBarBottomInset - metrics.dp(60f) - metrics.dp(20f)
            val periodSelectorHeight = 60.dp
            val cardTop = contentTop + periodSelectorHeight + 18.dp
            val cardHeight = (bottomNavTop - cardTop - 24.dp).coerceAtLeast(280.dp)

            if (showBackground) {
                HomeBackground(liveGlassEnabled = liveGlassEnabled)
            }

            StatsText(
                text = tr(language, "Статистика", "Statistics"),
                fontSize = 24f,
                lineHeight = 28.8f,
                color = StatsTextPrimary,
                modifier = Modifier
                    .offset(x = panelX, y = headerTop)
                    .width(panelWidth),
            )
            StatsText(
                text = tr(language, "Сводка использования", "Usage summary"),
                fontSize = 12f,
                lineHeight = 14.4f,
                color = StatsTextSecondary,
                modifier = Modifier
                    .offset(x = panelX, y = subtitleTop)
                    .width(panelWidth),
            )

            Column(
                modifier = Modifier
                    .offset(x = panelX, y = contentTop)
                    .width(panelWidth),
            ) {
                StatsPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    language = language,
                    liveGlassEnabled = liveGlassEnabled,
                    onPeriodChanged = { selectedPeriod = it },
                )
                Spacer(modifier = Modifier.height(18.dp))
                StatsDateCard(
                    state = state,
                    period = selectedPeriod,
                    modifier = Modifier
                        .width(panelWidth)
                        .height(cardHeight),
                )
            }
        }
    }
}

@Composable
private fun StatsPeriodSelector(
    selectedPeriod: StatsPeriod,
    language: AppLanguage,
    liveGlassEnabled: Boolean,
    onPeriodChanged: (StatsPeriod) -> Unit,
) {
    val periods = listOf(
        StatsPeriod.TODAY,
        StatsPeriod.WEEK,
        StatsPeriod.MONTH,
        StatsPeriod.YEAR,
    )
    GlassSegmentedControl(
        labels = listOf(
            tr(language, "Сегодня", "Today"),
            tr(language, "Неделя", "Week"),
            tr(language, "Месяц", "Month"),
            tr(language, "Год", "Year"),
        ),
        selectedIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0),
        onSelectedIndexChanged = { index -> onPeriodChanged(periods[index.coerceIn(periods.indices)]) },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        activeBlur = 0.dp,
        lensRefractionHeight = 24.dp,
        lensRefractionAmount = 24.dp,
        depthEffectEnabled = true,
        maxPressedScaleX = 1.1f,
        liveGlassEnabled = liveGlassEnabled,
    )
}

@Composable
private fun StatsDateCard(
    state: AppUiState,
    period: StatsPeriod,
    modifier: Modifier = Modifier,
) {
    val language = state.personalizationSettings.language
    val today = LocalDate.now()
    val aggregate = remember(state.dailyStats, period, today) {
        aggregateDailyStats(state.dailyStats, period, today)
    }
    val weeklyBars = remember(state.dailyStats, today, language) {
        buildWeeklyTrafficBars(state.dailyStats, today, language)
    }
    val trafficLabel = formatStatsBytes(aggregate.totalBytes, language)
    val avgPing = aggregate.averagePingLabel()
    val sessionsLabel = aggregate.sessions.toString()
    val onlineLabel = formatStatsDuration(aggregate.onlineSeconds, language)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(StatsBgLighter, RoundedCornerShape(20.dp))
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier
                .width(275.dp)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            StatsText(
                text = statsPeriodLabel(period, language),
                fontSize = 12f,
                lineHeight = 14.4f,
                color = StatsTextSecondary,
                modifier = Modifier.width(180.dp),
            )
            StatsText(
                text = trafficLabel,
                fontSize = 28f,
                lineHeight = 33.6f,
                color = StatsTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(180.dp),
            )
        }

        Column(
            modifier = Modifier
                .width(275.dp)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                StatsMetricCard(
                    title = tr(language, "Трафик", "Traffic"),
                    value = trafficLabel,
                    modifier = Modifier.weight(1f),
                )
                StatsMetricCard(
                    title = tr(language, "Сессии", "Sessions"),
                    value = sessionsLabel,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                StatsMetricCard(
                    title = tr(language, "Средний пинг", "Average ping"),
                    value = avgPing,
                    modifier = Modifier.weight(1f),
                )
                StatsMetricCard(
                    title = tr(language, "Онлайн", "Online"),
                    value = onlineLabel,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        StatsUsageChart(
            bars = weeklyBars,
        )
    }
}

@Composable
private fun StatsMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StatsBgSoft, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, StatsStroke.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        StatsText(
            text = title,
            fontSize = 11f,
            lineHeight = 13.2f,
            color = StatsTextSecondary,
            maxLines = 1,
        )
        StatsText(
            text = value,
            fontSize = 22f,
            lineHeight = 26.4f,
            color = StatsTextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatsUsageChart(
    bars: List<StatsChartBar>,
) {
    val maxValue = bars.maxOfOrNull { it.bytes }?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .width(288.dp)
                .height(67.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                val normalized = (bar.bytes.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                val barHeight = 18.dp + 49.dp * normalized
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (bar.isToday) {
                                Brush.horizontalGradient(
                                    listOf(StatsAccent, StatsTextPrimary.copy(alpha = 0.25f)),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xE6264250), Color(0x595B7180)),
                                )
                            },
                        ),
                )
            }
        }
        Row(
            modifier = Modifier
                .width(288.dp)
                .height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bars.forEach { bar ->
                StatsText(
                    text = bar.label,
                    fontSize = 10f,
                    lineHeight = 12f,
                    color = StatsTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(24.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatsText(
    text: String,
    fontSize: Float,
    lineHeight: Float,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        color = color,
        fontFamily = ManropeFontFamily,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private data class StatsAggregate(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val onlineSeconds: Long = 0L,
    val sessions: Int = 0,
    val pingSumMs: Long = 0L,
    val pingSamples: Int = 0,
) {
    val totalBytes: Long
        get() = (rxBytes + txBytes).coerceAtLeast(0L)

    fun averagePingLabel(): String {
        if (pingSamples <= 0) return "--"
        return "${(pingSumMs / pingSamples).coerceAtLeast(0L)} ms"
    }
}

private data class StatsChartBar(
    val label: String,
    val bytes: Long,
    val isToday: Boolean,
)

private fun statsPeriodLabel(period: StatsPeriod, language: AppLanguage): String {
    return when (period) {
        StatsPeriod.TODAY -> tr(language, "За сегодня", "Today")
        StatsPeriod.WEEK -> tr(language, "За неделю", "This week")
        StatsPeriod.MONTH -> tr(language, "За месяц", "This month")
        StatsPeriod.YEAR -> tr(language, "За год", "This year")
    }
}

private fun aggregateDailyStats(
    stats: List<DailyStats>,
    period: StatsPeriod,
    today: LocalDate,
): StatsAggregate {
    val start = when (period) {
        StatsPeriod.TODAY -> today
        StatsPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        StatsPeriod.MONTH -> today.withDayOfMonth(1)
        StatsPeriod.YEAR -> today.withDayOfYear(1)
    }
    return stats
        .asSequence()
        .mapNotNull { day -> parseDailyStatsDate(day)?.let { it to day } }
        .filter { (date, _) -> !date.isBefore(start) && !date.isAfter(today) }
        .fold(StatsAggregate()) { acc, (_, day) ->
            acc.copy(
                rxBytes = acc.rxBytes + day.rxBytes,
                txBytes = acc.txBytes + day.txBytes,
                onlineSeconds = acc.onlineSeconds + day.onlineSeconds,
                sessions = acc.sessions + day.sessions,
                pingSumMs = acc.pingSumMs + day.pingSumMs,
                pingSamples = acc.pingSamples + day.pingSamples,
            )
        }
}

private fun buildWeeklyTrafficBars(
    stats: List<DailyStats>,
    today: LocalDate,
    language: AppLanguage,
): List<StatsChartBar> {
    val statsByDate = stats.associateBy { it.date }
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L..6L).map { offset ->
        val date = monday.plusDays(offset)
        val dayStats = statsByDate[date.toString()]
        StatsChartBar(
            label = weekdayLabel(date.dayOfWeek, language),
            bytes = ((dayStats?.rxBytes ?: 0L) + (dayStats?.txBytes ?: 0L)).coerceAtLeast(0L),
            isToday = date == today,
        )
    }
}

private fun weekdayLabel(dayOfWeek: DayOfWeek, language: AppLanguage): String {
    if (language == AppLanguage.EN) {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Пн"
        DayOfWeek.TUESDAY -> "Вт"
        DayOfWeek.WEDNESDAY -> "Ср"
        DayOfWeek.THURSDAY -> "Чт"
        DayOfWeek.FRIDAY -> "Пт"
        DayOfWeek.SATURDAY -> "Сб"
        DayOfWeek.SUNDAY -> "Вс"
    }
}

private fun parseDailyStatsDate(stats: DailyStats): LocalDate? {
    return runCatching { LocalDate.parse(stats.date) }.getOrNull()
}

private fun formatStatsBytes(bytes: Long, language: AppLanguage): String =
    com.noki.vpn.data.TrafficFormat.bytes(bytes, language).label

fun formatStatsDuration(seconds: Long, language: AppLanguage): String {
    val totalSeconds = seconds.coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val totalHours = totalMinutes / 60L
    val secondsPart = totalSeconds % 60L
    val minutesPart = totalMinutes % 60L
    val totalDays = totalHours / 24L
    if (totalDays >= 30L) {
        val months = (totalDays / 30L).coerceAtLeast(1L)
        return if (language == AppLanguage.RU) "$months мес." else "$months mo"
    }
    if (totalDays >= 1L) {
        return if (language == AppLanguage.RU) "$totalDays д." else "$totalDays d"
    }
    if (totalHours >= 1L) {
        return "%02d:%02d".format(Locale.US, totalHours, minutesPart)
    }
    return "%02d:%02d".format(Locale.US, minutesPart, secondsPart)
}
