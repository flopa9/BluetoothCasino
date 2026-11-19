
package com.example.bluetoothcasino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Dark = Color(0xFF101512)
private val Green = Color(0xFF17613D)
private val Gold = Color(0xFFE8BD63)
private val White = Color.White

data class PlayingCard(val rank: String, val suit: String) {
    val points: Int
        get() = when (rank) {
            "A" -> 11
            "K", "Q", "J" -> 10
            else -> rank.toInt()
        }
}

fun newDeck(): List<PlayingCard> {
    val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    val suits = listOf("♠", "♥", "♦", "♣")
    return (suits.flatMap { suit -> ranks.map { rank -> PlayingCard(rank, suit) } }).shuffled()
}

fun score(cards: List<PlayingCard>): Int {
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
        setContent { BlackjackApp() }
    }
}

@Composable
fun BlackjackApp() {
    var balance by remember { mutableIntStateOf(10000) }
    var bet by remember { mutableIntStateOf(100) }
    var deck by remember { mutableStateOf(newDeck()) }
    var player by remember { mutableStateOf(emptyList<PlayingCard>()) }
    var dealer by remember { mutableStateOf(emptyList<PlayingCard>()) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Сделай ставку и начни игру!") }

    fun finishGame() {
        var hand = dealer
        while (score(hand) < 17 && deck.isNotEmpty()) {
            hand = hand + deck.first()
            deck = deck.drop(1)
        }
        dealer = hand

        val p = score(player)
        val d = score(dealer)

        message = when {
            p > 21 -> "Перебор! Ты проиграл."
            d > 21 -> "У дилера перебор! Ты победил!"
            p > d -> "Победа! Отличная игра!"
            p < d -> "Дилер победил."
            else -> "Ничья! Ставка возвращена."
        }

        when {
            p > 21 -> Unit
            d > 21 || p > d -> balance += bet * 2
            p == d -> balance += bet
        }
        finished = true
    }

    fun startGame() {
        if (bet <= 0 || bet > balance) {
            message = "Недостаточно фишек для такой ставки."
            return
        }

        deck = newDeck()
        player = deck.take(2)
        dealer = deck.drop(2).take(2)
        deck = deck.drop(4)
        balance -= bet
        started = true
        finished = false
        message = "Твой ход. Возьми карту или остановись."

        if (score(player) == 21) {
            finishGame()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Dark
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        "♠ BLACKJACK 21 ♠",
                        color = Gold,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Text("ТВОЙ БАЛАНС", color = White)
                    Text(
                        "$balance 🪙",
                        color = Gold,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Green, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ДИЛЕР", color = White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (started) dealer.joinToString("   ") { "${it.rank}${it.suit}" }
                                else "Карты дилера",
                                color = White,
                                fontSize = 25.sp
                            )
                            if (started && (finished || dealer.isEmpty())) {
                                Text("Очки: ${score(dealer)}", color = Gold)
                            } else if (started) {
                                Text("Очки: ?", color = Gold)
                            }
                            Spacer(Modifier.height(24.dp))
                            Text("ТВОИ КАРТЫ", color = White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                player.joinToString("   ") { "${it.rank}${it.suit}" },
                                color = White,
                                fontSize = 25.sp
                            )
                            if (started) {
                                Text("Очки: ${score(player)}", color = Gold)
                            }
                        }
                    }
                }

                item {
                    Text(message, color = Gold, fontSize = 18.sp)
                }

                item {
                    Text("Ставка: $bet 🪙", color = White, fontSize = 18.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (!started && bet > 100) bet -= 100 },
                            enabled = !started && bet > 100
                        ) { Text("−100") }

                        Button(
                            onClick = {
                                if (!started && bet + 100 <= balance) bet += 100
                            },
                            enabled = !started && bet + 100 <= balance
                        ) { Text("+100") }
                    }
                }

                item {
                    Button(
                        onClick = { startGame() },
                        enabled = !started && balance >= bet,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text("РАЗДАТЬ КАРТЫ", color = Color.Black)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (started && !finished && deck.isNotEmpty()) {
                                    player = player + deck.first()
                                    deck = deck.drop(1)
                                    if (score(player) >= 21) finishGame()
                                }
                            },
                            enabled = started && !finished,
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            Text("ЕЩЁ КАРТУ")
                        }

                        Button(
                            onClick = {
                                if (started && !finished) finishGame()
                            },
                            enabled = started && !finished,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text("СТОП", color = Color.Black)
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            started = false
                            finished = false
                            player = emptyList()
                            dealer = emptyList()
                            message = "Сделай ставку и начни игру!"
                        },
                        enabled = finished,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("НОВАЯ ИГРА")
                    }
                }
            }
        }
    }
}
