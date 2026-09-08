package com.example.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats a year-month string like "2026-10" to "MM-YYYY" format like "10-2026".
 */
fun formatMonthToMMYYYY(yearMonth: String): String {
    val parts = yearMonth.trim().split("-")
    return if (parts.size == 2 && parts[0].length == 4) {
        "${parts[1]}-${parts[0]}"
    } else {
        yearMonth
    }
}

@Composable
fun LanguageToggleButton(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkVariant: Boolean = false
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkVariant) Color.White.copy(alpha = 0.22f) else BluePrimary.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDarkVariant) Color.White.copy(alpha = 0.45f) else BluePrimary.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Language",
                tint = if (isDarkVariant) Color.White else BluePrimary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (currentLanguage == AppLanguage.HINDI) "हिन्दी" else "ENG",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isDarkVariant) Color.White else BluePrimary
            )
        }
    }
}

@Composable
fun PoshanTopAppBar(
    title: String,
    subtitle: String? = null,
    currentLanguage: AppLanguage? = null,
    onLanguageToggle: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    onCalendarClick: (() -> Unit)? = null,
    onSyncClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    titleFontSize: TextUnit? = null,
    subtitleFontSize: TextUnit? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        color = BluePrimary,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        val hasRightAction = actions != null || (currentLanguage != null && onLanguageToggle != null)
        // Logo size reduced by 25% (48dp for top-level, 36dp for back-navigation screens)
        val logoSize = if (onNavigateBack != null) 36.dp else 48.dp
        val leftEstimatedWidth = if (onNavigateBack != null) (36.dp + 4.dp + logoSize + 4.dp) else (logoSize + 6.dp)
        val rightEstimatedWidth = if (hasRightAction) 64.dp else 8.dp
        val sidePadding = maxOf(leftEstimatedWidth, rightEstimatedWidth)

        val effectiveTitleFontSize = titleFontSize ?: if (title.length > 26) 15.sp else 16.5.sp
        val effectiveTitleLineHeight = if (effectiveTitleFontSize <= 15.sp) 19.sp else 21.sp
        val effectiveSubtitleFontSize = subtitleFontSize ?: 11.5.sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp)
        ) {
            // 1. Left top corner: App Logo (and back button if onNavigateBack is specified)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Surface(
                    modifier = Modifier.size(logoSize),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.5.dp,
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.95f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cg_mdm_final_logo),
                        contentDescription = "CG MDM Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 2. Centre of the top: Page Header (Title & Subtitle)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = effectiveTitleFontSize,
                    lineHeight = effectiveTitleLineHeight
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = effectiveSubtitleFontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }

            // 3. Right top corner: Actions (Language toggle or custom actions)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actions != null) {
                    actions()
                } else if (currentLanguage != null && onLanguageToggle != null) {
                    LanguageToggleButton(
                        currentLanguage = currentLanguage,
                        onToggle = onLanguageToggle,
                        isDarkVariant = true
                    )
                }
            }
        }
    }
}

/**
 * Returns previous month in YYYY-MM format.
 */
fun getPreviousMonth(currentMonth: String): String {
    return try {
        val parts = currentMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        if (month == 1) {
            String.format(Locale.US, "%04d-12", year - 1)
        } else {
            String.format(Locale.US, "%04d-%02d", year, month - 1)
        }
    } catch (e: Exception) {
        currentMonth
    }
}

/**
 * Returns next month in YYYY-MM format.
 */
fun getNextMonth(currentMonth: String): String {
    return try {
        val parts = currentMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        if (month == 12) {
            String.format(Locale.US, "%04d-01", year + 1)
        } else {
            String.format(Locale.US, "%04d-%02d", year, month + 1)
        }
    } catch (e: Exception) {
        currentMonth
    }
}

/**
 * Horizontal swipe gesture modifier to navigate months forward (swipe left) and backward (swipe right).
 */
@Composable
fun Modifier.swipeToNavigateMonth(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
): Modifier {
    var accumulatedDrag by remember(selectedMonth) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minSwipeDistancePx = remember(density) { with(density) { 50.dp.toPx() } }

    return this.draggable(
        state = rememberDraggableState { delta ->
            accumulatedDrag += delta
        },
        orientation = Orientation.Horizontal,
        onDragStarted = {
            accumulatedDrag = 0f
        },
        onDragStopped = { velocity ->
            val threshold = minSwipeDistancePx
            if (accumulatedDrag < -threshold || velocity < -400f) {
                // Swipe Left -> Go forward to next month
                val next = getNextMonth(selectedMonth)
                if (next != selectedMonth) {
                    onMonthSelected(next)
                }
            } else if (accumulatedDrag > threshold || velocity > 400f) {
                // Swipe Right -> Go backward to previous month
                val prev = getPreviousMonth(selectedMonth)
                if (prev != selectedMonth) {
                    onMonthSelected(prev)
                }
            }
            accumulatedDrag = 0f
        }
    )
}

/**
 * Represents an Academic Year starting in June (startYear) and ending in May (endYear = startYear + 1).
 */
data class AcademicYearInfo(
    val startYear: Int,
    val endYear: Int,
    val code: String, // e.g. "2025-26"
    val isCurrent: Boolean,
    val months: List<Pair<String, String>> // e.g. [("2025-06", "जून 2025"), ..., ("2026-05", "मई 2026")]
)

/**
 * Returns the academic year code (e.g. "2025-26", "2026-27") for any given YYYY-MM string.
 * Academic year starts on June 1 and ends on May 31.
 */
fun getAcademicYearCodeForMonth(monthStr: String): String {
    return try {
        val parts = monthStr.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val startY = if (m >= 6) y else y - 1
        val endShort = (startY + 1).toString().takeLast(2)
        "$startY-$endShort"
    } catch (e: Exception) {
        "2026-27"
    }
}

/**
 * Generates the 12 months for an academic year starting in June of startYear and ending in May of (startYear + 1).
 */
fun getAcademicYearMonths(startYear: Int, isHi: Boolean): List<Pair<String, String>> {
    val endYear = startYear + 1
    val monthsData = listOf(
        Pair(6, if (isHi) "जून" else "Jun"),
        Pair(7, if (isHi) "जुलाई" else "Jul"),
        Pair(8, if (isHi) "अगस्त" else "Aug"),
        Pair(9, if (isHi) "सितंबर" else "Sep"),
        Pair(10, if (isHi) "अक्टूबर" else "Oct"),
        Pair(11, if (isHi) "नवंबर" else "Nov"),
        Pair(12, if (isHi) "दिसंबर" else "Dec"),
        Pair(1, if (isHi) "जनवरी" else "Jan"),
        Pair(2, if (isHi) "फरवरी" else "Feb"),
        Pair(3, if (isHi) "मार्च" else "Mar"),
        Pair(4, if (isHi) "अप्रैल" else "Apr"),
        Pair(5, if (isHi) "मई" else "May")
    )

    return monthsData.map { (m, name) ->
        val y = if (m >= 6) startYear else endYear
        val code = String.format(Locale.US, "%04d-%02d", y, m)
        val label = "$name $y"
        code to label
    }
}

/**
 * Generates list of academic years.
 * Dynamically computes current academic year based on current calendar date in IST:
 * - If current month is June..December (>=6), startYear is current calendar year (e.g. 2026 -> AY 2026-27).
 * - If current month is January..May (<6), startYear is current calendar year - 1 (e.g. May 2027 -> AY 2026-27).
 * When current year's May ends (June 1st), the next academic year (e.g. 2027-28) automatically becomes current,
 * and the year 2026-27 is automatically considered as a previous academic year.
 */
fun getAcademicYearsList(isHi: Boolean, earliestStartYear: Int = 2024): List<AcademicYearInfo> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val curYear = cal.get(Calendar.YEAR)
    val curMonth = cal.get(Calendar.MONTH) + 1 // 1..12

    val currentAcademicStartYear = if (curMonth >= 6) curYear else curYear - 1

    val list = mutableListOf<AcademicYearInfo>()
    val minStart = minOf(earliestStartYear, currentAcademicStartYear - 1)
    for (startY in minStart..currentAcademicStartYear) {
        val endY = startY + 1
        val code = "$startY-${endY.toString().takeLast(2)}"
        val isCurrent = startY == currentAcademicStartYear
        list.add(
            AcademicYearInfo(
                startYear = startY,
                endYear = endY,
                code = code,
                isCurrent = isCurrent,
                months = getAcademicYearMonths(startY, isHi)
            )
        )
    }
    return list
}

sealed class MonthRowItem {
    data class YearTab(
        val yearInfo: AcademicYearInfo,
        val isExpanded: Boolean,
        val isSelectedYear: Boolean
    ) : MonthRowItem()

    data class MonthButton(
        val code: String,
        val label: String,
        val yearCode: String,
        val isSelected: Boolean
    ) : MonthRowItem()
}

@Composable
fun MonthSelectorRow(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI

    val selectedYear = try {
        selectedMonth.split("-")[0].toInt()
    } catch (e: Exception) {
        2026
    }
    val earliestStart = minOf(2024, selectedYear - 1)

    val academicYears = remember(isHi, earliestStart) {
        getAcademicYearsList(isHi, earliestStart)
    }

    val selectedYearCode = remember(selectedMonth) {
        getAcademicYearCodeForMonth(selectedMonth)
    }

    val currentAcademicYear = remember(academicYears) {
        academicYears.find { it.isCurrent }
    }
    val currentYearCode = currentAcademicYear?.code ?: "2026-27"

    // Previous academic years are collapsed by default unless the selectedMonth belongs to that year
    var expandedYears by remember(selectedYearCode, currentYearCode) {
        val initialPastYear = if (selectedYearCode != currentYearCode) setOf(selectedYearCode) else emptySet()
        mutableStateOf(initialPastYear)
    }

    // List of items in order:
    // Past years:
    //   Tab "Academic Year 2025-26" shows before Jun of current year.
    //   When clicked, its 12 months appear as individual top buttons before Jun of current year!
    // Current year:
    //   All 12 months appear as individual top buttons from June to May.
    val rowItems = remember(academicYears, expandedYears, selectedMonth, selectedYearCode) {
        val items = mutableListOf<MonthRowItem>()
        academicYears.forEach { ay ->
            if (!ay.isCurrent) {
                val isExpanded = ay.code in expandedYears
                val isYearSelected = ay.code == selectedYearCode
                items.add(
                    MonthRowItem.YearTab(
                        yearInfo = ay,
                        isExpanded = isExpanded,
                        isSelectedYear = isYearSelected
                    )
                )

                if (isExpanded) {
                    ay.months.forEach { (code, label) ->
                        items.add(
                            MonthRowItem.MonthButton(
                                code = code,
                                label = label,
                                yearCode = ay.code,
                                isSelected = code == selectedMonth
                            )
                        )
                    }
                }
            } else {
                ay.months.forEach { (code, label) ->
                    items.add(
                        MonthRowItem.MonthButton(
                            code = code,
                            label = label,
                            yearCode = ay.code,
                            isSelected = code == selectedMonth
                        )
                    )
                }
            }
        }
        items
    }

    val listState = rememberLazyListState()
    val selectedIndex = remember(selectedMonth, rowItems, selectedYearCode) {
        val idx = rowItems.indexOfFirst { it is MonthRowItem.MonthButton && it.code == selectedMonth }
        if (idx >= 0) idx else rowItems.indexOfFirst { it is MonthRowItem.YearTab && it.yearInfo.code == selectedYearCode }
    }

    LaunchedEffect(selectedMonth, selectedIndex) {
        if (selectedIndex >= 0) {
            val targetScroll = (selectedIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = rowItems,
            key = { item ->
                when (item) {
                    is MonthRowItem.YearTab -> "tab_${item.yearInfo.code}"
                    is MonthRowItem.MonthButton -> "month_${item.code}"
                }
            }
        ) { item ->
            when (item) {
                is MonthRowItem.YearTab -> {
                    val tabLabel = if (isHi) "शैक्षणिक सत्र ${item.yearInfo.code}" else "Academic Year ${item.yearInfo.code}"
                    val isTabActive = item.isExpanded || item.isSelectedYear
                    FilterChip(
                        selected = isTabActive,
                        onClick = {
                            val willExpand = item.yearInfo.code !in expandedYears
                            expandedYears = if (willExpand) {
                                expandedYears + item.yearInfo.code
                            } else {
                                expandedYears - item.yearInfo.code
                            }
                            // When expanding, auto-select latest month of that year if none selected
                            if (willExpand && selectedYearCode != item.yearInfo.code) {
                                val targetMonth = item.yearInfo.months.lastOrNull()?.first ?: "${item.yearInfo.endYear}-05"
                                onMonthSelected(targetMonth)
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tabLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (item.isExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            selectedTrailingIconColor = Color.White,
                            containerColor = Color(0xFFEFF6FF),
                            labelColor = Color(0xFF1E40AF),
                            iconColor = Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isTabActive,
                            borderColor = if (isTabActive) Color(0xFF1E3A8A) else Color(0xFF93C5FD),
                            borderWidth = 1.5.dp
                        )
                    )
                }

                is MonthRowItem.MonthButton -> {
                    val isSelected = item.isSelected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMonthSelected(item.code) },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) BluePrimary else Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "ACTIVE", "SYNCED", "GOOD", "VERIFIED" -> Triple(
            Color(0xFFECFDF5),
            StatusGoodGreen,
            if (lang == AppLanguage.HINDI) "🟢 सक्रिय / पूर्ण" else "🟢 Active / Synced"
        )
        "LOW", "WARNING", "PENDING", "EXPIRING_SOON" -> Triple(
            Color(0xFFFFFBEB),
            StatusWarningOrange,
            if (lang == AppLanguage.HINDI) "🟠 ध्यान दें" else "🟠 Warning / Pending"
        )
        "CRITICAL", "FAILED", "EXPIRED", "LOCKED" -> Triple(
            Color(0xFFFEF2F2),
            StatusCriticalRed,
            if (lang == AppLanguage.HINDI) "🔴 क्रिटिकल / बंद" else "🔴 Critical / Closed"
        )
        "VACATION" -> Triple(
            Color(0xFFF5F3FF),
            StatusVacationPurple,
            if (lang == AppLanguage.HINDI) "🟣 अवकाश" else "🟣 Vacation"
        )
        "SPECIAL_WORKING_DAY" -> Triple(
            Color(0xFFF0F9FF),
            BlueSecondary,
            if (lang == AppLanguage.HINDI) "🔵 विशेष कार्यदिवस" else "🔵 Special Working Day"
        )
        else -> Triple(Color(0xFFF8FAFC), Color(0xFF334155), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color = Color.White,
    iconTint: Color = BluePrimary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun MaskedAccountDisplay(
    maskedAccount: String,
    ifsc: String,
    bankName: String,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = bankName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == AppLanguage.HINDI) "खाता: $maskedAccount" else "A/C: $maskedAccount",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "IFSC: $ifsc",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun poshanTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    cursorColor = BluePrimary,
    focusedBorderColor = BluePrimary,
    unfocusedBorderColor = Color(0xFF94A3B8),
    focusedLabelColor = BluePrimary,
    unfocusedLabelColor = Color(0xFF334155),
    focusedPlaceholderColor = Color(0xFF64748B),
    unfocusedPlaceholderColor = Color(0xFF64748B),
    focusedSupportingTextColor = Color(0xFF475569),
    unfocusedSupportingTextColor = Color(0xFF64748B),
    focusedLeadingIconColor = BluePrimary,
    unfocusedLeadingIconColor = Color(0xFF475569),
    focusedTrailingIconColor = BluePrimary,
    unfocusedTrailingIconColor = Color(0xFF475569)
)

@Composable
fun poshanOutlinedTextFieldColors() = poshanTextFieldColors()

@Composable
fun poshanFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color(0xFFF1F5F9),
    labelColor = Color(0xFF1E293B),
    iconColor = Color(0xFF334155),
    selectedContainerColor = BluePrimary,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White
)

@Composable
fun poshanButtonColors(containerColor: Color = BluePrimary) = ButtonDefaults.buttonColors(
    containerColor = containerColor,
    contentColor = Color.White,
    disabledContainerColor = Color(0xFFCBD5E1),
    disabledContentColor = Color(0xFF64748B)
)

@Composable
fun poshanSuccessButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF16A34A),
    contentColor = Color.White,
    disabledContainerColor = Color(0xFFCBD5E1),
    disabledContentColor = Color(0xFF64748B)
)

/**
 * Formats standard ISO dates (yyyy-MM-dd) to Indian standard display format (dd-MM-yyyy).
 * Example: "2026-08-25" -> "25-08-2026"
 * Also handles timestamps: "2026-08-25 10:30:00" -> "25-08-2026 10:30:00"
 */
fun formatDisplayDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val trimmed = dateStr.trim()
    if (trimmed.contains(" ")) {
        val parts = trimmed.split(" ", limit = 2)
        val datePart = parts[0]
        val timePart = parts.getOrNull(1) ?: ""
        val dParts = datePart.split("-")
        if (dParts.size == 3 && dParts[0].length == 4) {
            val formatted = "${dParts[2]}-${dParts[1]}-${dParts[0]}"
            return if (timePart.isNotBlank()) "$formatted $timePart" else formatted
        }
        return trimmed
    }
    val parts = trimmed.split("-")
    if (parts.size == 3 && parts[0].length == 4) {
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }
    return trimmed
}

fun String?.toDisplayDate(): String = formatDisplayDate(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoshanDatePickerModal(
    currentDateStr: String,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialMillis = remember(currentDateStr) {
        try {
            val trimmed = currentDateStr.trim()
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            if (trimmed.matches(Regex("""\d{2}-\d{2}-\d{4}"""))) {
                val parts = trimmed.split("-")
                sdfUtc.parse("${parts[2]}-${parts[1]}-${parts[0]}")?.time ?: System.currentTimeMillis()
            } else if (trimmed.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
                val parts = trimmed.split("/")
                sdfUtc.parse("${parts[2]}-${parts[1]}-${parts[0]}")?.time ?: System.currentTimeMillis()
            } else if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                sdfUtc.parse(trimmed)?.time ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayedMonthMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedMillis
                        }
                        val yyyy = utcCal.get(Calendar.YEAR)
                        val mm = utcCal.get(Calendar.MONTH) + 1
                        val dd = utcCal.get(Calendar.DAY_OF_MONTH)
                        val formatted = String.format(Locale.US, "%02d-%02d-%04d", dd, mm, yyyy)
                        onDateSelected(formatted)
                    } else {
                        onDismiss()
                    }
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(
                    text = if (isHi) "दिनांक चुनें (OK)" else "Select Date (OK)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isHi) "रद्द करें" else "Cancel",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        )
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = if (isHi) "कैलेंडर से दिनांक चुनें" else "Select Date from Calendar",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            },
            headline = {
                val selectedMillis = datePickerState.selectedDateMillis
                val headlineText = if (selectedMillis != null) {
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = selectedMillis
                    }
                    val sdf = SimpleDateFormat("dd MMM yyyy (EEEE)", if (isHi) Locale("hi", "IN") else Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.format(utcCal.time)
                } else {
                    if (isHi) "दिनांक का चयन करें" else "Choose a date"
                }
                Text(
                    text = headlineText,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = BluePrimary,
                headlineContentColor = Color(0xFF0F172A),
                weekdayContentColor = Color(0xFF334155),
                subheadContentColor = Color(0xFF0F172A),
                navigationContentColor = Color(0xFF0F172A),
                yearContentColor = Color(0xFF0F172A),
                disabledYearContentColor = Color(0xFF94A3B8),
                currentYearContentColor = BluePrimary,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = BluePrimary,
                dayContentColor = Color(0xFF0F172A),
                disabledDayContentColor = Color(0xFFCBD5E1),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = BluePrimary,
                todayDateBorderColor = BluePrimary,
                todayContentColor = BluePrimary,
                dividerColor = Color(0xFFE2E8F0)
            )
        )
    }
}

/**
 * An interactive form field that triggers the Material 3 Calendar Date Picker on click.
 */
@Composable
fun PoshanDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isHi: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "DD-MM-YYYY"
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = BluePrimary
                    )
                }
            },
            colors = poshanTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        // Click overlay so clicking anywhere on the text field brings up the calendar picker
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        PoshanDatePickerModal(
            currentDateStr = value,
            isHi = isHi,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                onValueChange(selectedDate)
                showDatePicker = false
            }
        )
    }
}

