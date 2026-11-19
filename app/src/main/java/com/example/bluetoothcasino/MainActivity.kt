
package com.example.bluetoothcasino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF07110E)
private val Green = Color(0xFF0D563A)
private val DarkGreen = Color(0xFF083B2B)
private val Gold = Color(0xFFE9BD68)
private val White = Color(0xFFFFF8E9)
private val Red = Color(0xFFC83E4D)

data class CardData(val rank: String, val suit: String) {
    val points: Int get() = when (rank) {
        "A" -> 11
        "K", "Q", "J" -> 10
        else -> rank.toInt()
    }
    val red: Boolean get() = suit == "♥" || suit == "♦"
}

private fun makeDeck() =
    listOf("♠", "♥", "♦", "♣").flatMap { suit ->
        listOf("A", "2", "3", "4", "5", "6", "7",
            "8", "9", "10", "J", "Q", "K").map {
            CardData(it, suit)
        }
    }.shuffled()

private fun score(cards: List<CardData>): Int {
    var total = cards.sumOf { it.points }
    var aces = cards.count { it.rank == "A" }
    while (total > 21 && aces > 0) {
        total -= 10
        aces--
    }
    return total
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CasinoScreen() } }
    }
}

@Composable
fun PlayerAvatar(emoji: String, name: String, chips: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(Color(0xFF263C34), CircleShape)
                .border(2.dp, Gold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 34.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(chips, color = Gold, fontSize = 11.sp)
    }
}

@Composable
fun PlayingCardView(card: CardData, hidden: Boolean = false) {
    var revealed by remember { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (hidden && !revealed) 180f else 0f,
        animationSpec = tween(450),
        label = "cardFlip"
    )

    Box(
        modifier = Modifier
            .width(53.dp)
            .height(76.dp)
            .graphicsLayer { rotationY = flip }
            .background(
                if (hidden && !revealed) DarkGreen else White,
                RoundedCornerShape(7.dp)
            )
            .border(1.dp, Gold, RoundedCornerShape(7.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hidden && !revealed) {
            Text("♠", color = Gold, fontSize = 27.sp)
        } else {
            val color = if (card.red) Red else Color(0xFF17251F)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${card.rank}${card.suit}",
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    card.suit,
                    color = color,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun AnimatedHand(cards: List<CardData>, hideSecond: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cards.forEachIndexed { index, card ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(450)
                    )
            ) {
                PlayingCardView(card, hideSecond && index == 1)
            }
        }
    }
}

@Composable
fun CasinoScreen() {
    var balance by remember { mutableIntStateOf(10000) }
    var bet by remember { mutableIntStateOf(0) }
    var deck by remember { mutableStateOf(makeDeck()) }
    var playerCards by remember { mutableStateOf(emptyList<CardData>()) }
    var dealerCards by remember { mutableStateOf(emptyList<CardData>()) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Выбери ставку") }

    val playerScore = score(playerCards)
    val dealerScore = score(dealerCards)

    fun draw(): CardData {
        if (deck.isEmpty()) deck = makeDeck()
        val card = deck.first()
        deck = deck.drop(1)
        return card
    }

    fun finish() {
        while (score(dealerCards) < 17) {
            dealerCards = dealerCards + draw()
        }
        val p = score(playerCards)
        val d = score(dealerCards)

        message = when {
            p > 21 -> "Перебор!"
            d > 21 -> "Ты выиграл!"
            p > d -> "Победа!"
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

    fun start() {
        if (bet <= 0) {
            message = "Сначала выбери ставку"
            return
        }
        if (bet > balance) {
            message = "Не хватает фишек"
            return
        }
        balance -= bet
        deck = makeDeck()
        playerCards = listOf(draw(), draw())
        dealerCards = listOf(draw(), draw())
        started = true
        finished = false
        message = "Твой ход"
    }

    fun hit() {
        if (!started || finished) return
        playerCards = playerCards + draw()
        if (score(playerCards) >= 21) finish()
    }

    fun stand() {
        if (started && !finished) finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("BLACKJACK", color = Gold, fontSize = 23.sp,
                    fontWeight = FontWeight.Black)
                Text("VIP TABLE", color = White.copy(alpha = 0.6f),
                    fontSize = 10.sp, letterSpacing = 3.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("БАЛАНС", color = White.copy(alpha = 0.6f), fontSize = 10.sp)
                Text("$balance", color = Gold, fontSize = 21.sp,
                    fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Color(0xFF111D18),
                    RoundedCornerShape(30.dp)
                )
                .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Green, RoundedCornerShape(100.dp))
                    .border(3.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(100.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ДИЛЕР", color = Gold, fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold)

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFF253A31), CircleShape)
                        .border(2.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👩🏻‍💼", fontSize = 43.sp)
                }

                Text("Анастасия", color = White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (dealerCards.isEmpty()) {
                    Text("Карты дилера", color = White.copy(alpha = 0.6f))
                } else {
                    AnimatedHand(
                        dealerCards,
                        hideSecond = started && !finished
                    )
                    Text(
                        if (started && !finished)
                            "Очки: ${dealerCards.first().points}"
                        else "Очки: $dealerScore",
                        color = White,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar("👨🏻", "Игрок 1", "10 000")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BLACKJACK", color = Gold, fontSize = 12.sp,
                            fontWeight = FontWeight.Black)
                        Text("21", color = White, fontSize = 30.sp,
                            fontWeight = FontWeight.Black)
                    }
                    PlayerAvatar("👨🏼", "Игрок 2", "10 000")
                }

                Spacer(Modifier.weight(1f))

                if (playerCards.isEmpty()) {
                    Text("ТВОИ КАРТЫ", color = Gold, fontSize = 12.sp)
                    Text("Ожидаем раздачу", color = White.copy(alpha = 0.7f))
                } else {
                    AnimatedHand(playerCards)
                    Text("Твои очки: $playerScore", color = White,
                        fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text(message, color = Gold, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("ВЫБЕРИ СТАВКУ", color = White, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(100, 500, 1000).forEach { amount ->
                Button(
                    onClick = { if (!started && balance >= amount) bet = amount },
                    enabled = !started && balance >= amount,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bet == amount) Gold else DarkGreen,
                        contentColor = White
                    )
                ) {
                    Text("$amount", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Ставка: $bet фишек", color = Gold, fontSize = 13.sp)

        Spacer(Modifier.height(8.dp))

        if (!started || finished) {
            Button(
                onClick = {
                    if (finished) {
                        started = false
                        finished = false
                        playerCards = emptyList()
                        dealerCards = emptyList()
                        bet = 0
                        message = "Выбери ставку"
                    } else start()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Bg
                )
            ) {
                Text(
                    if (finished) "СЛЕДУЮЩИЙ РАУНД" else "НАЧАТЬ ИГРУ",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { hit() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold,
                        contentColor = Bg)
                ) {
                    Text("ЕЩЁ КАРТУ", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { stand() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen,
                        contentColor = White)
                ) {
                    Text("СТОП", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        Text("Только виртуальные фишки", color = White.copy(alpha = 0.5f),
            fontSize = 10.sp)
    }
}
