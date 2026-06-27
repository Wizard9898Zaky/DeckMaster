package com.deckmaster

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deckmaster.data.*
import com.deckmaster.engine.SpreadEngine
import com.deckmaster.engine.SpreadEngine.Planet
import com.deckmaster.engine.SpreadEngine.SpreadType
import com.deckmaster.ui.theme.*
import com.deckmaster.viewmodel.DeckViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeckMasterTheme {
                DeckMasterApp()
            }
        }
    }
}

@Composable
fun DeckMasterApp(vm: DeckViewModel = viewModel()) {
    val tabletsReady by vm.tabletsReady.collectAsStateWithLifecycle()
    val birthData    by vm.birthData.collectAsStateWithLifecycle()
    val spreads      by vm.spreads.collectAsStateWithLifecycle()
    val birthCard    by vm.birthCard.collectAsStateWithLifecycle()
    val isCalculating by vm.isCalculating.collectAsStateWithLifecycle()
    val error        by vm.error.collectAsStateWithLifecycle()

    // Trigger recalc after tablets finish loading
    LaunchedEffect(tabletsReady) {
        if (tabletsReady) vm.onTabletsReady()
    }

    if (!tabletsReady) {
        LoadingScreen()
        return
    }

    if (birthData == null) {
        BirthDateScreen(onConfirm = { y, m, d, sunrise ->
            vm.setBirthData(y, m, d, sunrise)
        })
        return
    }

    MainSpreadScreen(
        vm = vm,
        spreads = spreads,
        birthCard = birthCard,
        isCalculating = isCalculating,
        error = error,
        birthData = birthData!!,
        onResetBirth = { /* allow re-entry */ }
    )

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text  = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.clearError() }) { Text("OK") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(MidnightBlue), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🃏", fontSize = 64.sp)
            Text("DeckMaster", color = CardGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(color = CardGold)
            Text("Preparing card tablets...", color = OnSurface.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Birth Date Entry Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BirthDateScreen(onConfirm: (Int, Int, Int, Boolean) -> Unit) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showSunriseDialog by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    fun showDatePicker() {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d -> pendingDate = LocalDate.of(y, m + 1, d); showSunriseDialog = true },
            today.year - 30, today.monthValue - 1, today.dayOfMonth
        ).show()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepPurple, MidnightBlue))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier.fillMaxWidth(0.88f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("🃏", fontSize = 56.sp)
                Text("DeckMaster", color = CardGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Enter your date of birth to reveal your life spread",
                    color = OnSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Divider(color = CardGold.copy(alpha = 0.3f))

                selectedDate?.let { d ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, "date", tint = CardGold)
                        Text(
                            d.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                            color = CardGoldLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                }

                Button(
                    onClick = { showDatePicker() },
                    colors = ButtonDefaults.buttonColors(containerColor = CardGold, contentColor = DeepPurple),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DateRange, "pick date")
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedDate == null) "Select Birth Date" else "Change Date",
                         fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSunriseDialog) {
        AlertDialog(
            onDismissRequest = { showSunriseDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("Sunrise Question", color = CardGold, fontWeight = FontWeight.Bold)
            },
            text = {
                val d = pendingDate
                Text(
                    "Were you born before or after sunrise on " +
                    (d?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) ?: "") + "?\n\n" +
                    "If born before sunrise, your card day begins the previous day.",
                    color = OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSunriseDialog = false
                    pendingDate?.let {
                        selectedDate = it
                        onConfirm(it.year, it.monthValue, it.dayOfMonth, true)
                    }
                }) { Text("Before Sunrise", color = HeartsRed) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSunriseDialog = false
                    pendingDate?.let {
                        selectedDate = it
                        onConfirm(it.year, it.monthValue, it.dayOfMonth, false)
                    }
                }) { Text("After Sunrise", color = CardGold) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Spread Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSpreadScreen(
    vm: DeckViewModel,
    spreads: List<SpreadEngine.SpreadResult>,
    birthCard: String,
    isCalculating: Boolean,
    error: String?,
    birthData: DeckViewModel.BirthData,
    onResetBirth: () -> Unit
) {
    val context = LocalContext.current
    val selectedSpreadIdx by vm.selectedSpreadIndex.collectAsStateWithLifecycle()
    val selectedCardIdx   by vm.selectedCardIndex.collectAsStateWithLifecycle()
    val targetDate        by vm.targetDate.collectAsStateWithLifecycle()

    val currentSpread = spreads.getOrNull(selectedSpreadIdx)
    val spreadTypes   = SpreadType.values()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DeckMaster", color = CardGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (birthCard.isNotBlank()) {
                            Text(
                                "Birth card: ${CARD_NAMES[birthCard] ?: birthCard}  •  " +
                                targetDate.format(DateTimeFormatter.ofPattern("MMM d yyyy")),
                                color = OnSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                actions = {
                    IconButton(onClick = {
                        val t = targetDate
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> vm.setTargetDate(LocalDate.of(y, m + 1, d)) },
                            t.year, t.monthValue - 1, t.dayOfMonth
                        ).show()
                    }) { Icon(Icons.Default.DateRange, "target date", tint = CardGold) }
                }
            )
        },
        containerColor = MidnightBlue
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Spread type tabs
            ScrollableTabRow(
                selectedTabIndex = selectedSpreadIdx,
                containerColor = SurfaceDark,
                contentColor = CardGold,
                edgePadding = 8.dp
            ) {
                spreadTypes.forEachIndexed { i, type ->
                    Tab(
                        selected = selectedSpreadIdx == i,
                        onClick  = { vm.selectSpread(i) },
                        text = {
                            Text(
                                type.label,
                                fontWeight = if (selectedSpreadIdx == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSpreadIdx == i) CardGold else OnSurface.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            if (isCalculating) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CardGold)
                }
                return@Column
            }

            currentSpread?.let { spread ->
                Column(Modifier.fillMaxSize()) {
                    // Ruler row
                    RulerRow(spread)

                    // Card grid  ← main body
                    Box(Modifier.weight(1f)) {
                        CardGrid(
                            spread = spread,
                            selectedCardIdx = selectedCardIdx,
                            onCardTap = { vm.selectCard(it) }
                        )
                    }

                    // Interpretation panel
                    selectedCardIdx?.let { idx ->
                        InterpretationPanel(
                            cardCode = spread.quad.getOrElse(idx) { "" },
                            planet   = SpreadEngine.planetForIndex(idx),
                            onDismiss = { vm.selectCard(idx) /* toggle off */ }
                        )
                    }
                }
            } ?: run {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a birth date to begin.", color = OnSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ruler Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RulerRow(spread: SpreadEngine.SpreadResult) {
    val planetLabels = listOf("☿","♀","♂","♃","♄","☉","☽")
    val planets = listOf("Mercury","Venus","Mars","Jupiter","Saturn","Sun","Moon")

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMid),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ruler
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Ruler", color = RulerGlow, fontSize = 8.sp)
                    PlayingCard(code = spread.rulerCard, isRuler = true, isHighlighted = true, isSmall = true)
                }

                Divider(Modifier.width(1.dp).height(44.dp), color = CardGold.copy(alpha = 0.3f))

                // Planet cards
                spread.planetCards.take(7).forEachIndexed { i, card ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(planetLabels.getOrElse(i){"?"}, color = PlanetGlow, fontSize = 10.sp)
                        PlayingCard(code = card, isRuler = false, isHighlighted = false, isSmall = true)
                    }
                }
            }

            // Info label
            Text(
                "${spread.spreadType.label}  ${spread.info}",
                color = CardGold.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp).align(Alignment.End)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card Grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CardGrid(
    spread: SpreadEngine.SpreadResult,
    selectedCardIdx: Int?,
    onCardTap: (Int) -> Unit
) {
    val planetCards = spread.planetCards.toSet()

    // Group headers: Crown(0-2), then 7 planets × 7 cards
    val sectionLabels = mapOf(
        0 to "Crown", 3 to "Mercury", 10 to "Venus", 17 to "Mars",
        24 to "Jupiter", 31 to "Saturn", 38 to "Sun", 45 to "Moon"
    )

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
        // Render by planet group
        val groups = listOf(
            "Crown"   to (0..2),
            "Mercury" to (3..9),
            "Venus"   to (10..16),
            "Mars"    to (17..23),
            "Jupiter" to (24..30),
            "Saturn"  to (31..37),
            "Sun"     to (38..44),
            "Moon"    to (45..51)
        )
        groups.forEach { (label, range) ->
            item {
                PlanetSectionHeader(label)
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    range.forEach { idx ->
                        val code = spread.quad.getOrElse(idx) { "0A" }
                        val isPlanetCard = code in planetCards
                        val isSelected  = selectedCardIdx == idx
                        Box(
                            Modifier.weight(1f).clickable { onCardTap(idx) }
                        ) {
                            PlayingCard(
                                code = code,
                                isRuler = false,
                                isHighlighted = isPlanetCard,
                                isSmall = false,
                                isSelected = isSelected
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun PlanetSectionHeader(label: String) {
    val symbol = when (label) {
        "Crown"   -> "✦"
        "Mercury" -> "☿"
        "Venus"   -> "♀"
        "Mars"    -> "♂"
        "Jupiter" -> "♃"
        "Saturn"  -> "♄"
        "Sun"     -> "☉"
        "Moon"    -> "☽"
        else      -> "·"
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(symbol, color = PlanetGlow, fontSize = 14.sp)
        Text(label, color = PlanetGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Divider(Modifier.weight(1f).height(1.dp), color = PlanetGlow.copy(alpha = 0.2f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playing Card composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayingCard(
    code: String,
    isRuler: Boolean,
    isHighlighted: Boolean,
    isSmall: Boolean,
    isSelected: Boolean = false
) {
    val isNull = (code == "0A")
    val suit   = cardSuit(code)
    val red    = isRed(code)

    val borderColor = when {
        isSelected    -> CardGold
        isRuler       -> RulerGlow
        isHighlighted -> PlanetGlow
        isNull        -> NullCard
        else          -> SurfaceMid
    }
    val borderWidth = when {
        isSelected || isRuler || isHighlighted -> 2.dp
        else -> 1.dp
    }
    val bgColor = when {
        isNull     -> SurfaceMid.copy(alpha = 0.5f)
        isSelected -> CardGold.copy(alpha = 0.15f)
        else       -> Color(0xFFEFEFEF)
    }
    val textColor = if (isNull) NullCard else if (red) HeartsRed else SpadesBlack

    val cardWidth  = if (isSmall) 34.dp else 42.dp
    val cardHeight = if (isSmall) 44.dp else 58.dp
    val fontSize   = if (isSmall) 9.sp else 11.sp

    Card(
        modifier = Modifier.width(cardWidth).height(cardHeight)
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isNull) {
                Text("✦", color = NullCard, fontSize = fontSize)
            } else {
                Text(
                    cardLabel(code),
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interpretation Panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InterpretationPanel(
    cardCode: String,
    planet: Planet,
    onDismiss: () -> Unit
) {
    val planetName = planet.name.lowercase().replaceFirstChar { it.uppercase() }
    val cardName   = CARD_NAMES[cardCode] ?: cardCode
    val isNull     = cardCode == "0A"

    // Get interpretation text
    val text = if (isNull) {
        "This card has been nullified by contra-indication rules. " +
        "When certain cards appear in proximity, they neutralize each other's influence."
    } else {
        com.deckmaster.data.CardInterpretations.get(planetName, cardCode).ifBlank {
            "Interpretation for $cardName in $planetName position."
        }
    }

    // Birth card extra
    val birthCardText = if (!isNull && planet == Planet.CROWN) {
        com.deckmaster.data.CardInterpretations.get("Birthcard", cardCode)
    } else ""

    Card(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 320.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        cardName,
                        color = if (isRed(cardCode)) HeartsRed else CardGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "$planetName Position",
                        color = PlanetGlow,
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(42.dp, 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingCard(code = cardCode, isRuler = false, isHighlighted = false, isSmall = false)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "close", tint = OnSurface.copy(alpha = 0.5f))
                    }
                }
            }

            Divider(Modifier.padding(vertical = 8.dp), color = CardGold.copy(alpha = 0.2f))

            // Scrollable text body
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text, color = OnSurface, fontSize = 13.sp, lineHeight = 19.sp)

                if (birthCardText.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = CardGold.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Birth Card Profile",
                        color = CardGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(birthCardText, color = OnSurface.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
