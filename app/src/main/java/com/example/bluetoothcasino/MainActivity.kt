
package com.example.bluetoothcasino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Felt = Color(0xFF075B43)
private val Gold = Color(0xFFFFD36A)
private val Dark = Color(0xFF082B23)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation =
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.statusBarColor = android.graphics.Color.rgb(8, 43, 35)
        window.navigationBarColor = android.graphics.Color.rgb(8, 43, 35)

        setContent {
            MaterialTheme {
                CardGame()
            }
        }
    }
}

data class PlayingCard(
    val rank: String,
    val suit: String,
    val value: Int,
    val id: Int
)

data class GameState(
    val deck: List<PlayingCard>,
    val player: List<PlayingCard>,
    val dealer: List<PlayingCard>,
    val finished: Boolean,
    val message: String
)

private fun createDeck(): List<PlayingCard> {
    val suits = listOf("♠", "♥", "♦", "♣")
    val ranks = listOf(
        "2", "3", "4", "5", "6", "7",
        "8", "9", "10", "J", "Q", "K", "A"
    )

    val cards = mutableListOf<PlayingCard>()
    var id = 0

    for (suit in suits) {
        for (rank in ranks) {
            val value = when (rank) {
                "J", "Q", "K" -> 10
                "A" -> 11
                else -> rank.toInt()
            }

            cards.add(PlayingCard(rank, suit, value, id++))
        }
    }

    return cards.shuffled()
}

private fun score(cards: List<PlayingCard>): Int {
    var result = cards.sumOf { it.value }
    var aces = cards.count { it.rank == "A" }

    while (result > 21 && aces > 0) {
        result -= 10
        aces--
    }

    return result
}

private fun newGame(): GameState {
    val deck = createDeck()

    return GameState(
        deck = deck.drop(4),
        player = listOf(deck[0], deck[2]),
        dealer = listOf(deck[1], deck[3]),
        finished = false,
        message = "Ваш ход"
    )
}

private fun drawCard(game: GameState): Pair<PlayingCard, List<PlayingCard>> {
    val card = game.deck.first()
    return card to game.deck.drop(1)
}

@Composable
fun CardGame() {
    var game by remember { mutableStateOf(newGame()) }
    var showDealerCard by remember { mutableStateOf(false) }

    fun finishGame(current: GameState): GameState {
        var dealerCards = current.dealer
        var deck = current.deck

        while (score(dealerCards) < 17 && deck.isNotEmpty()) {
            val card = deck.first()
            dealerCards = dealerCards + card
            deck = deck.drop(1)
        }

        val playerScore = score(current.player)
        val dealerScore = score(dealerCards)

        val result = when {
            playerScore > 21 -> "Перебор! Раздатчица победила"
            dealerScore > 21 -> "Победа! У раздатчицы перебор"
            playerScore > dealerScore -> "Вы победили!"
            playerScore < dealerScore -> "Победила раздатчица"
            else -> "Ничья!"
        }

        return current.copy(
            dealer = dealerCards,
            deck = deck,
            finished = true,
            message = result
        )
    }

    fun hit() {
        if (game.finished || game.deck.isEmpty()) return

        val (card, remaining) = drawCard(game)
        val updated = game.copy(
            player = game.player + card,
            deck = remaining
        )

        game = if (score(updated.player) > 21) {
            finishGame(updated)
        } else {
            updated
        }

        if (game.finished) showDealerCard = true
    }

    fun stand() {
        if (game.finished) return
        showDealerCard = true
        game = finishGame(game)
    }

    fun restart() {
        showDealerCard = false
        game = newGame()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Felt, RoundedCornerShape(24.dp))
                .border(2.dp, Gold, RoundedCornerShape(24.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BLACKJACK",
                    color = Gold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "КАРТОЧНАЯ ИГРА",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                TextButton(onClick = { restart() }) {
                    Text("НОВАЯ ИГРА", color = Gold)
                }
            }

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    emoji = "👩🏻‍💼",
                    name = "РАЗДАТЧИЦА"
                )

                Spacer(Modifier.width(18.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (showDealerCard) "КАРТЫ РАЗДАТЧИЦЫ"
                        else "КАРТЫ РАЗДАТЧИЦЫ",
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(game.dealer, key = { it.id }) { card ->
                            PlayingCardView(
                                card = card,
                                hidden = !showDealerCard &&
                                    card == game.dealer.last()
                            )
                        }
                    }

                    Text(
                        if (showDealerCard) "Очки: ${score(game.dealer)}"
                        else "Очки: ?",
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                game.message,
                color = if (game.finished) Gold else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    emoji = "🧑🏻",
                    name = "ИГРОК"
                )

                Spacer(Modifier.width(18.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ВАШИ КАРТЫ",
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(game.player, key = { it.id }) { card ->
                            PlayingCardView(card = card)
                        }
                    }

                    Text(
                        "Очки: ${score(game.player)}",
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameAction(
                    text = "ВЗЯТЬ КАРТУ",
                    enabled = !game.finished,
                    onClick = { hit() }
                )

                GameAction(
                    text = "ХВАТИТ",
                    enabled = !game.finished,
                    onClick = { stand() }
                )

                GameAction(
                    text = "НОВАЯ РАЗДАЧА",
                    enabled = game.finished,
                    onClick = { restart() }
                )
            }
        }
    }
}

@Composable
fun Avatar(
    emoji: String,
    name: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .background(
                    Color(0xFF123D32),
                    RoundedCornerShape(50)
                )
                .border(2.dp, Gold, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 38.sp)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            name,
            color = Gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlayingCardView(
    card: PlayingCard,
    hidden: Boolean = false
) {
    var visible by remember(card.id) { mutableStateOf(false) }

    LaunchedEffect(card.id) {
        delay(40)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it / 2 }
        )
    ) {
        if (hidden) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .background(
                        Color(0xFF173B85),
                        RoundedCornerShape(7.dp)
                    )
                    .border(
                        2.dp,
                        Color.White,
                        RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("★", color = Gold, fontSize = 25.sp)
            }
        } else {
            val red = card.suit == "♥" || card.suit == "♦"

            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .background(Color.White, RoundedCornerShape(7.dp))
                    .border(
                        1.dp,
                        Color.LightGray,
                        RoundedCornerShape(7.dp)
                    )
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    card.rank,
                    color = if (red) Color.Red else Color.Black,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    card.suit,
                    color = if (red) Color.Red else Color.Black,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun GameAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF123D32),
            contentColor = Gold,
            disabledContainerColor = Color.DarkGray,
            disabledContentColor = Color.LightGray
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(46.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
