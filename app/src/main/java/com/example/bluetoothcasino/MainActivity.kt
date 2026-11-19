
package com.example.bluetoothcasino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF07130F)
private val Felt = Color(0xFF0D593B)
private val Gold = Color(0xFFE8BD63)
private val Cream = Color(0xFFFFF8E9)
private val Muted = Color(0xFFB9C9C0)
private val RedCard = Color(0xFFC83E4D)

data class PlayingCard(val rank: String, val suit: String) {
    val points: Int
        get() = when (rank) {
            "A" -> 11
            "K", "Q", "J" -> 10
            else -> rank.toInt()
        }

    val isRed: Boolean
        get() = suit == "♥" || suit == "♦"
}

private fun newDeck(): List<PlayingCard> =
    listOf("♠", "♥", "♦", "♣").flatMap { suit ->
        listOf("A", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "J", "Q", "K").map {
            PlayingCard(it, suit)
        }
    }.shuffled()

private fun handScore(cards: List<PlayingCard>): Int {
    var score = cards.sumOf { it.points }
    var aces = cards.count { it.rank == "A" }

    while (score > 21 && aces > 0) {
        score -= 10
        aces--
    }
    return score
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BlackjackApp()
            }
        }
    }
}

@Composable
fun PlayingCardView(card: PlayingCard?, hidden: Boolean = false) {
    Box(
        modifier = Modifier
            .width(59.dp)
            .height(84.dp)
            .background(
                if (hidden) Color(0xFF174B39) else Cream,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (hidden) Gold else Color.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hidden || card == null) {
            Text("★", color = Gold, fontSize = 28.sp)
        } else {
            val cardColor = if (card.isRed) RedCard else Color(0xFF17251F)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${card.rank}${card.suit}",
                    color = cardColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    card.suit,
                    color = cardColor,
                    fontSize = 27.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun HandRow(cards: List<PlayingCard>, hideSecond: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cards.forEachIndexed { index, card ->
            PlayingCardView(
                card = card,
                hidden = hideSecond && index == 1
            )
        }
    }
}

@Composable
fun BlackjackApp() {
    var balance by remember { mutableIntStateOf(10000) }
    var bet by remember { mutableIntStateOf(0) }
    var deck by remember { mutableStateOf(newDeck()) }
    var playerCards by remember { mutableStateOf(emptyList<PlayingCard>()) }
    var dealerCards by remember { mutableStateOf(emptyList<PlayingCard>()) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Выбери ставку и начни игру") }

    val playerScore = handScore(playerCards)
    val dealerScore = handScore(dealerCards)

    fun drawCard(): PlayingCard {
        if (deck.isEmpty()) deck = newDeck()
        val card = deck.first()
        deck = deck.drop(1)
        return card
    }

    fun finishRound() {
        while (handScore(dealerCards) < 17) {
            dealerCards = dealerCards + drawCard()
        }

        val p = handScore(playerCards)
        val d = handScore(dealerCards)

        message = when {
            p > 21 -> "Перебор! Дилер победил"
            d > 21 -> "Дилер перебрал! Ты выиграл"
            p > d -> "Ты выиграл!"
            p < d -> "Дилер победил"
            else -> "Ничья!"
        }

        when {
            p > 21 -> Unit
            d > 21 || p > d -> balance += bet * 2
            p == d -> balance += bet
        }

        finished = true
    }

    fun startRound() {
        if (bet <= 0) {
            message = "Сначала выбери ставку"
            return
        }
        if (bet > balance) {
            message = "Недостаточно фишек"
            return
        }

        balance -= bet
        deck = newDeck()
        playerCards = listOf(drawCard(), drawCard())
        dealerCards = listOf(drawCard(), drawCard())
        started = true
        finished = false
        message = "Твой ход!"
    }

    fun hit() {
        if (!started || finished) return
        playerCards = playerCards + drawCard()
        if (handScore(playerCards) >= 21) {
            finishRound()
        }
    }

    fun stand() {
        if (!started || finished) return
        finishRound()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "BLACKJACK",
                    color = Gold,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text("21 • Классическая игра", color = Muted, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("ТВОИ ФИШКИ", color = Muted, fontSize = 11.sp)
                Text(
                    balance.toString(),
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Felt, RoundedCornerShape(24.dp))
                .border(2.dp, Gold, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ДИЛЕР",
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                if (dealerCards.isEmpty()) {
                    Text("Ожидание игры", color = Muted)
                } else {
                    HandRow(
                        dealerCards,
                        hideSecond = started && !finished
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (started && !finished)
                            "Очки: ${dealerCards.first().points}"
                        else "Очки: $dealerScore",
                        color = Cream
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Gold.copy(alpha = 0.5f))
                Spacer(Modifier.height(24.dp))

                Text(
                    "ТВОИ КАРТЫ",
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                if (playerCards.isEmpty()) {
                    Text("Здесь появятся твои карты", color = Muted)
                } else {
                    HandRow(playerCards)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Очки: $playerScore",
                        color = Cream,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    message,
                    color = Gold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "ВЫБЕРИ СТАВКУ",
            color = Cream,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(100, 500, 1000).forEach { amount ->
                Button(
                    onClick = {
                        if (!started && balance >= amount) {
                            bet = amount
                        }
                    },
                    enabled = !started && balance >= amount,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Background
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(amount.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Текущая ставка: $bet фишек",
            color = Gold,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(18.dp))

        if (!started || finished) {
            Button(
                onClick = {
                    if (finished) {
                        started = false
                        finished = false
                        playerCards = emptyList()
                        dealerCards = emptyList()
                        bet = 0
                        message = "Выбери ставку и начни игру"
                    } else {
                        startRound()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Background
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (finished) "СЛЕДУЮЩИЙ РАУНД" else "НАЧАТЬ ИГРУ",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { hit() },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Background
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("ЕЩЁ КАРТУ", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { stand() },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF244C3D),
                        contentColor = Cream
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("СТОП", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Только виртуальные фишки • Без реальных денег",
            color = Muted,
            fontSize = 11.sp
        )
    }
}
