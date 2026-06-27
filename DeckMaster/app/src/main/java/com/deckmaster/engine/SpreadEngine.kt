package com.deckmaster.engine

/**
 * SpreadEngine — exact Kotlin port of the DeckMaster bash script algorithms.
 *
 * Algorithms ported verbatim:
 *   quadrate(), cut2card(), get_card_pattern(), get_cut_pattern(),
 *   stack_cards(), cut_pattern_stack(), contra_indications(), cards_invert(),
 *   get_tablets(), get_periods_and_weeks(), get_spreads()
 */
object SpreadEngine {

    // ──────────────────────────────────────────────────────────────────────────
    // Constants & Initial Deck
    // ──────────────────────────────────────────────────────────────────────────

    /** The standard ordered deck used to seed quadration. */
    val INITIAL_DECK: List<String> = listOf(
        "AH","2H","3H","4H","5H","6H","7H","8H","9H","TH","JH","QH","KH",
        "AC","2C","3C","4C","5C","6C","7C","8C","9C","TC","JC","QC","KC",
        "AD","2D","3D","4D","5D","6D","7D","8D","9D","TD","JD","QD","KD",
        "AS","2S","3S","4S","5S","6S","7S","8S","9S","TS","JS","QS","KS"
    )

    /**
     * days_before_month — indexed 1..12 (index 0 unused).
     * Non-leap-year fixed values, exactly as in the bash script.
     */
    val DAYS_BEFORE_MONTH = intArrayOf(0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)

    /** Days in each month (non-leap). Used for sunrise-adjustment rollback. */
    val DAYS_IN_MONTH = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /** Sub-period day index boundaries within a 52-day period (7 pairs of start/end). */
    val PDAY = intArrayOf(0, 7, 7, 14, 15, 21, 22, 29, 30, 36, 37, 44, 44, 51)

    // quad_order for 52-card deck
    private val QUAD_ORDER_52 = intArrayOf(
        2,13,24,48,17,28,39,6,32,43,10,21,47,1,12,38,
        5,16,27,49,20,31,42,9,35,46,0,26,37,4,15,41,
        8,19,30,50,23,34,45,14,25,36,3,29,40,7,18,44,11,22,33,51
    )

    // quad_order for 54-card deck (with jokers)
    private val QUAD_ORDER_54 = intArrayOf(
        0,3,14,25,49,18,29,40,7,33,44,11,22,48,2,13,
        39,6,17,28,50,21,32,43,10,36,47,1,27,38,5,16,
        42,9,20,31,51,24,35,46,15,26,37,4,30,41,8,19,45,12,23,34,52,53
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Core algorithms
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * quadrate() — deterministic shuffle using quad_order permutation.
     * new[i] = old[quad_order[i]]
     */
    fun quadrate(deck: List<String>): List<String> {
        val order = when (deck.size) {
            52 -> QUAD_ORDER_52
            54 -> QUAD_ORDER_54
            else -> throw IllegalArgumentException("quadrate: unsupported deck size ${deck.size}")
        }
        return order.map { deck[it] }
    }

    /**
     * get_tablets() — apply quadrate 90 times from INITIAL_DECK, store each result.
     * tablet[0] = first quadration, tablet[89] = ninetieth.
     */
    fun getTablets(): Array<List<String>> {
        val tablets = Array(90) { emptyList<String>() }
        var cards = INITIAL_DECK
        for (i in 0..89) {
            cards = quadrate(cards)
            tablets[i] = cards
        }
        return tablets
    }

    /**
     * date2jd() — convert (month, day) to julian day number used in this system.
     * Exactly: days_before_month[month] + day
     */
    fun date2jd(month: Int, day: Int): Int = DAYS_BEFORE_MONTH[month] + day

    /**
     * jd2dateStr() — convert JD to "M/D" display string.
     */
    fun jd2dateStr(jd: Int): String {
        if (jd <= 0) return ""
        val (m, d) = jd2date(jd)
        return "$m/$d"
    }

    /**
     * jd2date() — convert julian day back to (month, day).
     * Returns Pair(month, day).
     */
    fun jd2date(jd: Int): Pair<Int, Int> {
        if (jd < 1 || jd > 366) return Pair(1, 1)
        var month = 12
        for (m in 1..12) {
            if (DAYS_BEFORE_MONTH[m] >= jd) {
                month = m - 1
                break
            }
        }
        if (month < 1) month = 1
        val day = jd - DAYS_BEFORE_MONTH[month]
        return Pair(month, day)
    }

    /**
     * isLeapYear() — standard Gregorian leap year test.
     */
    fun isLeapYear(year: Int): Boolean {
        val y = if (year < 0) year + 1 else year
        return (y % 4 == 0) && (y % 100 != 0 || y % 400 == 0)
    }

    /**
     * dayOfWeek() — Tomohiko Sakamoto / standard formula.
     * Returns 0=Sun, 1=Mon, ... 6=Sat (matching weekday_names array in bash script).
     */
    fun dayOfWeek(year: Int, month: Int, day: Int): Int {
        // Gregorian formula from the bash script's Zeller implementation
        val a = (14 - month) / 12
        val y = year - a
        val m = month + 12 * a - 2
        return ((day + y + y / 4 - y / 100 + y / 400 + (31 * m) / 12) % 7 + 7) % 7
    }

    /**
     * getBirthNumber() — 55 - ((month*2) + day)
     */
    fun getBirthNumber(month: Int, day: Int): Int = 55 - (month * 2 + day)

    /**
     * getBirthCard() — look up birth card in tablet[89] (prefixed with "0A").
     * cards = ["0A"] + tablet[89]  →  index = getBirthNumber(month, day)
     */
    fun getBirthCard(month: Int, day: Int, tablets: Array<List<String>>): String {
        val birthNumber = getBirthNumber(month, day)
        val cards = listOf("0A") + tablets[89]
        return if (birthNumber in cards.indices) cards[birthNumber] else "0A"
    }

    /**
     * cut2card() — rotate deck so `target` card is first.
     * Exact port of bash cut2card() for 52-card deck.
     */
    fun cut2card(target: String, deck: List<String>): List<String> {
        val idx = deck.indexOf(target)
        if (idx < 0) return deck
        return deck.subList(idx, deck.size) + deck.subList(0, idx)
    }

    /**
     * getCardPattern() — return first 8 cards, skipping leading "0A".
     */
    fun getCardPattern(deck: List<String>): List<String> {
        val d = if (deck.isNotEmpty() && deck[0] == "0A") deck.drop(1) else deck
        return d.take(8)
    }

    /**
     * getCutPattern() — cut deck to `card`, then return first 8 (the planet pattern).
     */
    fun getCutPattern(card: String, tablet: List<String>): List<String> {
        return getCardPattern(cut2card(card, tablet))
    }

    /**
     * stackCards() — group 52 cards by which of 8 pattern cards they follow.
     *
     * Exact port of bash stack_cards():
     *   - num starts at 8 (pre-pattern bucket)
     *   - For each card: if it matches pattern[a], set num=a
     *   - Card is appended to groups[num]
     *   - At the end: last group absorbs the pre-pattern bucket (groups[8])
     *   - Output: groups[0]..groups[7] concatenated
     */
    fun stackCards(pattern: List<String>, deck: List<String>): List<String> {
        val groups = Array(9) { mutableListOf<String>() }
        var currentGroup = 8

        for (card in deck) {
            val matchIdx = pattern.indexOfFirst { it == card }
            if (matchIdx >= 0) {
                currentGroup = matchIdx
            }
            groups[currentGroup].add(card)
        }

        // Append pre-pattern bucket to the last touched group
        val overflow = groups[8].toList()
        if (currentGroup != 8 && overflow.isNotEmpty()) {
            groups[currentGroup].addAll(overflow)
            groups[8].clear()
        }

        return groups.take(8).flatten()
    }

    /**
     * cutPatternStack() — get_cut_pattern then stack_cards.
     * Equivalent to bash cut_pattern_stack().
     */
    fun cutPatternStack(card: String, tablet: List<String>, deck: List<String>): List<String> {
        val pattern = getCutPattern(card, tablet)
        return stackCards(pattern, deck)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spread position constants
    // ──────────────────────────────────────────────────────────────────────────

    enum class Planet { CROWN, MERCURY, VENUS, MARS, JUPITER, SATURN, SUN, MOON }

    fun planetForIndex(i: Int): Planet = when (i) {
        0, 1, 2       -> Planet.CROWN
        in 3..9       -> Planet.MERCURY
        in 10..16     -> Planet.VENUS
        in 17..23     -> Planet.MARS
        in 24..30     -> Planet.JUPITER
        in 31..37     -> Planet.SATURN
        in 38..44     -> Planet.SUN
        else          -> Planet.MOON
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Contra-indications
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * contraIndications() — exact port of bash contra_indications().
     *
     * Input: 52-card list (in the order: crown[3] + mercury[7] + venus[7] + mars[7]
     *         + jupiter[7] + saturn[7] + sun[7] + moon[7])
     *
     * Applies nullification rules, returns modified groups:
     *   Pair(crown, mapOf("mercury" to List, "venus" to List, ...))
     */
    fun contraIndications(spread52: List<String>): List<String> {
        if (spread52.size != 52) return spread52

        // Parse into groups
        val crown    = spread52.subList(0, 3).toMutableList()
        val mercury  = spread52.subList(3, 10).toMutableList()
        val venus    = spread52.subList(10, 17).toMutableList()
        val mars     = spread52.subList(17, 24).toMutableList()
        val jupiter  = spread52.subList(24, 31).toMutableList()
        val saturn   = spread52.subList(31, 38).toMutableList()
        val sun      = spread52.subList(38, 45).toMutableList()
        val moon     = spread52.subList(45, 52).toMutableList()

        var contra = ""  // accumulated contra cards

        for (count in 0..6) {
            // Mercury rules
            when (mercury.getOrNull(count)) {
                "6H" -> for (a in 0..6) if (mercury.getOrNull(a) == "5H") mercury[a] = "0A"
                "TC" -> {
                    if (count > 0 && (mercury[count - 1] == "7C" || mercury[count - 1] == "9C"))
                        mercury[count - 1] = "0A"
                    if (count < 6 && (mercury[count + 1] == "7C" || mercury[count + 1] == "9C"))
                        mercury[count + 1] = "0A"
                }
                "6D" -> contra += "5D"
                "7S" -> contra += "8S"
            }
            // Venus rules
            when (venus.getOrNull(count)) {
                "6D" -> for (a in 0..6) {
                    val v = venus.getOrNull(a)
                    if (v == "5H" || v == "5D") venus[a] = "0A"
                }
                "8D", "TD" -> for (a in 0..6) if (venus.getOrNull(a) == "9D") venus[a] = "0A"
                "7S" -> contra += "7H"
            }
            // Mars rules
            when (mars.getOrNull(count)) {
                "4D" -> contra += "5D7D"
                "6D" -> for (a in 0..6) {
                    val m = mars.getOrNull(a)
                    if (m == "5D" || m == "7D") mars[a] = "0A"
                }
                "4S" -> contra += "5S7S"
            }
            // Jupiter rules
            when (jupiter.getOrNull(count)) {
                "6S" -> for (a in 0..6) {
                    val j = jupiter.getOrNull(a)
                    if (j != null && (j.first() == '5' || j.first() == '7')) jupiter[a] = "0A"
                }
            }
            // Saturn rules
            when (saturn.getOrNull(count)) {
                "6D" -> contra += "5D7D"
                "4S" -> for (a in 0..6) if (saturn.getOrNull(a) == "7S") saturn[a] = "0A"
                "TS" -> contra += "9S"
            }
            // Sun rules
            when (sun.getOrNull(count)) {
                "6C" -> contra += "5C"
                "6D" -> contra += "5D5S"
            }
            // Moon rules
            when (moon.getOrNull(count)) {
                "6C" -> {
                    if (mercury.getOrNull(count)?.first() == '5') mercury[count] = "0A"
                }
                "9C" -> contra += "TC"
                "6D" -> {
                    val fives = (0..6).count { moon.getOrNull(it)?.first() == '5' }
                    if (fives == 1) for (a in 0..6)
                        if (moon.getOrNull(a)?.first() == '5') moon[a] = "0A"
                }
                "7D" -> contra += "8D"
                "6S" -> contra += "5S7S"
            }
        }

        // Apply accumulated contra cards
        if (contra.isNotEmpty()) {
            val contraCards = contra.chunked(2)
            for (cc in contraCards) {
                for (a in 0..6) {
                    if (mercury.getOrNull(a) == cc) mercury[a] = "0A"
                    if (venus.getOrNull(a) == cc) venus[a] = "0A"
                    if (mars.getOrNull(a) == cc) mars[a] = "0A"
                    if (jupiter.getOrNull(a) == cc) jupiter[a] = "0A"
                    if (saturn.getOrNull(a) == cc) saturn[a] = "0A"
                    if (sun.getOrNull(a) == cc) sun[a] = "0A"
                    if (moon.getOrNull(a) == cc) moon[a] = "0A"
                }
            }
        }

        // cards_invert: reverse each planetary group (NOT crown)
        return crown + mercury.reversed() + venus.reversed() + mars.reversed() +
               jupiter.reversed() + saturn.reversed() + sun.reversed() + moon.reversed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Periods and Weeks
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * getPeriodJDs() — calculate the 7 period blocks of 52 days.
     * Returns List of 7 IntArrays, each containing the JDs for that period.
     * If leap=true, day 60 is skipped (leap year adjustment).
     */
    fun getPeriodJDs(birthMonth: Int, birthDay: Int, leap: Boolean): List<IntArray> {
        val bjd = date2jd(birthMonth, birthDay)
        val periods = mutableListOf<IntArray>()
        var a = bjd + 1
        for (p in 1..7) {
            val b = a + 51
            val jds = mutableListOf<Int>()
            for (aa in a..b) {
                var jj = aa
                if (jj > 366) jj -= 366
                if (jj == 60 && !leap) {
                    // skip day 60 for non-leap, add 61 instead (leap year version)
                    // Actually: for non-leap (pjd), skip day 60. For leap (pjdleap), include it.
                    // bash: [[ "$aa" -eq 60 && -z "${var#pjd}" ]] && leap=0 || eval ...
                    // When var=pjd (non-leap): if aa==60, set leap=0 (skip it)
                    // At end of period: if leap was set, add aa+1
                    continue
                }
                jds.add(jj)
            }
            // For non-leap: if day 60 was encountered, add the next day (61 mapped to its jd)
            // The bash does: [ ! -z "$leap" ] && ((aa++)) && eval ... && unset leap
            // This adds one extra day after the skip to keep count at 52
            if (!leap) {
                // Check if any jd in range was 60
                if (a <= 60 && 60 <= b) {
                    var extra = (b + 1)
                    if (extra > 366) extra -= 366
                    jds.add(extra)
                }
            }
            periods.add(jds.toIntArray())
            a = (jds.lastOrNull() ?: (a + 51)) + 1
        }
        return periods
    }

    /**
     * getWeekJDs() — calculate 52 weekly periods of 7 days.
     */
    fun getWeekJDs(birthMonth: Int, birthDay: Int, leap: Boolean): List<IntArray> {
        val bjd = date2jd(birthMonth, birthDay)
        val weeks = mutableListOf<IntArray>()
        var a = bjd + 1
        for (p in 1..52) {
            val b = a + 6
            val jds = mutableListOf<Int>()
            for (aa in a..b) {
                var jj = aa
                if (jj > 366) jj -= 366
                if (jj == 60 && !leap) continue
                jds.add(jj)
            }
            if (!leap && a <= 60 && 60 <= b) {
                var extra = b + 1
                if (extra > 366) extra -= 366
                jds.add(extra)
            }
            weeks.add(jds.toIntArray())
            a = (jds.lastOrNull() ?: (a + 6)) + 1
        }
        return weeks
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Main calculation: makeQuad helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * makeQuad() — apply the quad transformation:
     *   cut to ruler → quadrate → cut to birthcard → rotate last→first
     */
    fun makeQuad(birthCard: String, ruler: String, spread: List<String>): List<String> {
        val step1 = cut2card(ruler, spread)
        val step2 = quadrate(step1)
        val step3 = cut2card(birthCard, step2)
        // Rotate: put element at index 51 first, drop last
        return listOf(step3[51]) + step3.subList(0, 51)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Full spread calculation
    // ──────────────────────────────────────────────────────────────────────────

    data class SpreadResult(
        val spreadType: SpreadType,
        val quad: List<String>,           // 52 cards (after contra_indications + invert)
        val rulerCard: String,
        val planetCards: List<String>,    // 7 cards: Mercury .. Moon
        val info: String,                 // label (age / period# / day# / week# / weekday name)
        val rowDateLabels: List<Pair<String, String>> = emptyList() // 7 (left, right) for Mercury..Moon rows
    )

    enum class SpreadType(val label: String) {
        AGE("Age"), PERIOD("Period"), DAY("Day"), WEEK("Week"), WEEKDAY("Weekday")
    }

    /**
     * getSpreads() — calculate all 5 spreads for the given birth date and target date.
     * Returns list of 5 SpreadResults in order: Age, Period, Day, Week, Weekday.
     */
    fun getSpreads(
        birthYear: Int, birthMonth: Int, birthDay: Int,
        targetYear: Int, targetMonth: Int, targetDay: Int,
        tablets: Array<List<String>>
    ): List<SpreadResult> {

        val birthCard = getBirthCard(birthMonth, birthDay, tablets)
        val isJoker = birthCard == "0A"

        // Age
        var age = targetYear - birthYear
        val bjd = date2jd(birthMonth, birthDay)
        val tjd = date2jd(targetMonth, targetDay)
        if (tjd < bjd) age--

        // Age spread
        val ageSpread: List<String> = if (isJoker) {
            tablets[89]
        } else {
            cutPatternStack(birthCard, tablets[0], tablets[89])
        }

        // Age planet: 8-card pattern from age spread
        val agePlanetRaw = getCutPattern(ageSpread[age % 52], tablets[age % 90])
        val ageRuler = agePlanetRaw[0]
        val agePlanets = agePlanetRaw.drop(1) // Mercury..Moon (7 cards)
        val agePlanetFull = listOf(ageRuler) + agePlanets // full 8

        // Age quad (raw, before contra)
        val ageQuadRaw = makeQuad(birthCard, ageRuler, ageSpread)

        // Determine if we need to adjust for leap year
        var ty = 0
        if (bjd >= 61) {
            ty = 1
        }
        val effectiveTargetYear = if (ty == 1) targetYear + 1 else targetYear
        val leap = isLeapYear(effectiveTargetYear)

        // Get period JDs
        val periodJDs = getPeriodJDs(birthMonth, birthDay, leap)
        val weekJDs = getWeekJDs(birthMonth, birthDay, leap)

        // Find which period the target falls in
        var pnum = 1
        var cardNum = 0
        for (p in 1..7) {
            val period = periodJDs.getOrElse(p - 1) { intArrayOf() }
            val found = period.indexOfFirst { it == tjd }
            if (found >= 0) {
                pnum = p
                cardNum = (found + 1) % 52
                break
            }
        }

        // Find which week the target falls in
        var wnum = 1
        var wkdayNum = 0
        for (w in 1..52) {
            val week = weekJDs.getOrElse(w - 1) { intArrayOf() }
            val found = week.indexOfFirst { it == tjd }
            if (found >= 0) {
                wnum = w
                wkdayNum = (found + 1) % 8
                break
            }
        }

        // Day spread = stack_cards(age_planet[0..7], age_spread)
        val daySpread = stackCards(agePlanetFull, ageSpread)

        // Period planet
        val periodPlanetRaw = getCutPattern(agePlanetFull[pnum], tablets[(age + pnum) % 90])
        val periodRuler = periodPlanetRaw[0]
        val periodPlanets = periodPlanetRaw.drop(1)
        val periodPlanetFull = listOf(periodRuler) + periodPlanets

        // Period quad (raw)
        val periodQuadRaw = makeQuadFromDaySpread(birthCard, periodRuler, daySpread)

        // Day planet
        val dayPlanetRaw = getCutPattern(
            daySpread[cardNum],
            tablets[((pnum - 1) * 52 + cardNum + age) % 90]
        )
        val dayRuler = dayPlanetRaw[0]
        val dayPlanets = dayPlanetRaw.drop(1)
        val dayPlanetFull = listOf(dayRuler) + dayPlanets

        // Day quad (raw)
        val dayQuadRaw = makeQuadFromCutPatternStack(
            birthCard, dayRuler,
            tablets[((pnum - 1) * 52 + cardNum + age) % 90],
            daySpread
        )

        // Week spread: indexed 1..52 (1-based)
        val weekSpread = cutPatternStack(birthCard, tablets[age % 90], tablets[89])
        // weekSpread is 0-indexed but wnum is 1-based → use wnum-1
        val weekSpreadCard = weekSpread.getOrElse(wnum - 1) { weekSpread[0] }

        // Week planet
        val weekPlanetRaw = getCutPattern(weekSpreadCard, tablets[(wnum + age) % 90])
        val weekRuler = weekPlanetRaw[0]
        val weekPlanets = weekPlanetRaw.drop(1)
        val weekPlanetFull = listOf(weekRuler) + weekPlanets

        // Week quad (raw)
        val weekQuadRaw = makeQuad(birthCard, weekRuler, weekSpread)

        // Weekday ruler = week_planet[wkday_num]
        val wkdayRuler = weekPlanetFull.getOrElse(wkdayNum) { weekPlanetFull[0] }
        val wkdayPlanetRaw = getCutPattern(
            wkdayRuler, tablets[(age + wnum + wkdayNum) % 90]
        )
        val wkdayPlanets = wkdayPlanetRaw.drop(1)

        // Weekday quad (raw) — uses stackCards of weekPlanetFull + weekSpread then quadrate+cut
        val wkdayBase = stackCards(weekPlanetFull, weekSpread)
        val wkdayQuadRaw = makeQuad(birthCard, wkdayRuler, wkdayBase)

        // Apply contra_indications and cards_invert to each raw quad
        val weekdayName = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")[
            dayOfWeek(targetYear, targetMonth, targetDay)
        ]

        // ── Row date labels for each spread ───────────────────────────────────
        // Age: 7 rows = 7 periods (Mercury=Period1 … Moon=Period7)
        val ageRowLabels: List<Pair<String, String>> = periodJDs.map { jds ->
            Pair(jd2dateStr(jds.firstOrNull() ?: 0), jd2dateStr(jds.lastOrNull() ?: 0))
        }

        // Period: 7 rows = 7 sub-periods within current period (using PDAY boundaries)
        val currentPeriodJDs = periodJDs.getOrElse(pnum - 1) { intArrayOf() }
        val periodRowLabels: List<Pair<String, String>> = (0 until 7).map { k ->
            val startIdx = PDAY[k * 2]
            val endIdx   = PDAY[k * 2 + 1]
            Pair(
                jd2dateStr(currentPeriodJDs.getOrElse(startIdx) { 0 }),
                jd2dateStr(currentPeriodJDs.getOrElse(endIdx)   { 0 })
            )
        }

        // Week/Weekday: 7 rows = 7 days of current week (date + weekday name)
        val weekdayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val currentWeekJDs = weekJDs.getOrElse(wnum - 1) { intArrayOf() }
        val targetDayOfWeek = dayOfWeek(targetYear, targetMonth, targetDay)
        // Day 1 of the week is (wkdayNum - 1) days before the target
        val day1DOW = (targetDayOfWeek - (wkdayNum - 1) + 70) % 7
        val weekRowLabels: List<Pair<String, String>> = (0 until 7).map { i ->
            val jd  = currentWeekJDs.getOrElse(i) { 0 }
            val dow = (day1DOW + i) % 7
            Pair(jd2dateStr(jd), weekdayNames[dow])
        }

        return listOf(
            SpreadResult(
                SpreadType.AGE,
                contraIndications(ageQuadRaw),
                ageRuler, agePlanets,
                "$age",
                ageRowLabels
            ),
            SpreadResult(
                SpreadType.PERIOD,
                contraIndications(periodQuadRaw),
                periodRuler, periodPlanets,
                "#$pnum",
                periodRowLabels
            ),
            SpreadResult(
                SpreadType.DAY,
                contraIndications(dayQuadRaw),
                dayRuler, dayPlanets,
                "#${(pnum - 1) * 52 + cardNum}"
            ),
            SpreadResult(
                SpreadType.WEEK,
                contraIndications(weekQuadRaw),
                weekRuler, weekPlanets,
                "#$wnum",
                weekRowLabels
            ),
            SpreadResult(
                SpreadType.WEEKDAY,
                contraIndications(wkdayQuadRaw),
                wkdayRuler, wkdayPlanets,
                weekdayName,
                weekRowLabels
            )
        )
    }

    /** makeQuad variant: cut ruler from daySpread (for period quad) */
    private fun makeQuadFromDaySpread(birthCard: String, ruler: String, spread: List<String>): List<String> {
        return makeQuad(birthCard, ruler, spread)
    }

    /** makeQuad variant: cut_pattern_stack then quadrate+cut (for day quad) */
    private fun makeQuadFromCutPatternStack(
        birthCard: String, ruler: String,
        tablet: List<String>, spread: List<String>
    ): List<String> {
        val stacked = cutPatternStack(ruler, tablet, spread)
        val step2 = quadrate(cut2card(ruler, stacked))
        val step3 = cut2card(birthCard, step2)
        return listOf(step3[51]) + step3.subList(0, 51)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sunrise date adjustment
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * adjustForSunrise() — if born before sunrise, subtract one day from birth date.
     * Returns Pair(month, day).
     */
    fun adjustForSunrise(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        var m = month
        var d = day
        var y = year
        d--
        if (d == 0) {
            m--
            if (m == 0) {
                y--
                m = 12
                d = 31
            } else {
                d = DAYS_IN_MONTH[m]
            }
        }
        return Triple(y, m, d)
    }
}
