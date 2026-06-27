package com.deckmaster.data

/** All 52 card codes in deck order, plus joker. */
val CARD_CODES = listOf(
    "AH","2H","3H","4H","5H","6H","7H","8H","9H","TH","JH","QH","KH",
    "AC","2C","3C","4C","5C","6C","7C","8C","9C","TC","JC","QC","KC",
    "AD","2D","3D","4D","5D","6D","7D","8D","9D","TD","JD","QD","KD",
    "AS","2S","3S","4S","5S","6S","7S","8S","9S","TS","JS","QS","KS",
    "JOKR"
)

/** Full name for each card code. */
val CARD_NAMES: Map<String, String> = mapOf(
    "AH" to "Ace of Hearts",    "2H" to "Two of Hearts",    "3H" to "Three of Hearts",
    "4H" to "Four of Hearts",   "5H" to "Five of Hearts",   "6H" to "Six of Hearts",
    "7H" to "Seven of Hearts",  "8H" to "Eight of Hearts",  "9H" to "Nine of Hearts",
    "TH" to "Ten of Hearts",    "JH" to "Jack of Hearts",   "QH" to "Queen of Hearts",
    "KH" to "King of Hearts",
    "AC" to "Ace of Clubs",     "2C" to "Two of Clubs",     "3C" to "Three of Clubs",
    "4C" to "Four of Clubs",    "5C" to "Five of Clubs",    "6C" to "Six of Clubs",
    "7C" to "Seven of Clubs",   "8C" to "Eight of Clubs",   "9C" to "Nine of Clubs",
    "TC" to "Ten of Clubs",     "JC" to "Jack of Clubs",    "QC" to "Queen of Clubs",
    "KC" to "King of Clubs",
    "AD" to "Ace of Diamonds",  "2D" to "Two of Diamonds",  "3D" to "Three of Diamonds",
    "4D" to "Four of Diamonds", "5D" to "Five of Diamonds", "6D" to "Six of Diamonds",
    "7D" to "Seven of Diamonds","8D" to "Eight of Diamonds","9D" to "Nine of Diamonds",
    "TD" to "Ten of Diamonds",  "JD" to "Jack of Diamonds", "QD" to "Queen of Diamonds",
    "KD" to "King of Diamonds",
    "AS" to "Ace of Spades",    "2S" to "Two of Spades",    "3S" to "Three of Spades",
    "4S" to "Four of Spades",   "5S" to "Five of Spades",   "6S" to "Six of Spades",
    "7S" to "Seven of Spades",  "8S" to "Eight of Spades",  "9S" to "Nine of Spades",
    "TS" to "Ten of Spades",    "JS" to "Jack of Spades",   "QS" to "Queen of Spades",
    "KS" to "King of Spades",
    "JOKR" to "Joker",          "0A" to "Joker"
)

/** Suit for each card (H=Hearts, C=Clubs, D=Diamonds, S=Spades). */
fun cardSuit(code: String): Char = when {
    code.endsWith("H") -> 'H'
    code.endsWith("C") -> 'C'
    code.endsWith("D") -> 'D'
    code.endsWith("S") -> 'S'
    else -> '?'
}

/** Unicode suit symbol. */
fun suitSymbol(suit: Char): String = when (suit) {
    'H' -> "♥"
    'C' -> "♣"
    'D' -> "♦"
    'S' -> "♠"
    else -> "★"
}

/** Rank label (A, 2..10, J, Q, K). */
fun cardRank(code: String): String = when (code.take(2).dropLast(1)) {
    "A" -> "A"
    "T" -> "10"
    "J" -> "J"
    "Q" -> "Q"
    "K" -> "K"
    else -> code.take(1)
}

/** Display label: rank + suit symbol (e.g. "A♥", "10♠"). */
fun cardLabel(code: String): String {
    if (code == "0A" || code == "JOKR") return "🃏"
    val suit = cardSuit(code)
    return cardRank(code) + suitSymbol(suit)
}

/** True for red suits. */
fun isRed(code: String): Boolean = cardSuit(code) == 'H' || cardSuit(code) == 'D'
