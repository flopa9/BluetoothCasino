
package com.example.bluetoothcasino

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Felt = Color(0xFF075B43)
private val Gold = Color(0xFFFFD36A)
private val Dark = Color(0xFF082B23)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.statusBarColor = android.graphics.Color.rgb(8, 43, 35)
        window.navigationBarColor = android.graphics.Color.rgb(8, 43, 35)

        setContent {
            MaterialTheme {
                CardGame()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

@Composable
fun CardGame() {
    var game by remember { mutableStateOf(newGame()) }
    var showDealerCard by remember { mutableStateOf(false) }
    val bluetooth = remember { BluetoothConnection() }

    fun finishGame(current: GameState): GameState {
        var dealerCards = current.dealer
        var deck = current.deck

        while (score(dealerCards) < 17 && deck.isNotEmpty()) {
            dealerCards = dealerCards + deck.first()
            deck = deck.drop(1)
        }

        val playerScore = score(current.player)
        val dealerScore = score(dealerCards)

        val result = when {
            playerScore > 21 -> "Перебор! Победила раздатчица"
            dealerScore > 21 -> "Вы победили!"
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

        val card = game.deck.first()
        val updated = game.copy(
            player = game.player + card,
            deck = game.deck.drop(1)
        )

        game = if (score(updated.player) > 21) {
            finishGame(updated)
        } else updated

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark)
            .padding(8.dp)
            .background(Felt, RoundedCornerShape(22.dp))
            .border(2.dp, Gold, RoundedCornerShape(22.dp))
            .padding(10.dp),
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
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "КАРТОЧНАЯ ИГРА",
                color = Color.White,
                fontSize = 12.sp
            )
            TextButton(onClick = { restart() }) {
                Text("НОВАЯ ИГРА", color = Gold)
            }
        }

        BluetoothPanel(bluetooth)

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar("👩🏻‍💼", "РАЗДАТЧИЦА")
            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("КАРТЫ РАЗДАТЧИЦЫ", color = Color.White, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(game.dealer, key = { it.id }) { card ->
                        PlayingCardView(
                            card,
                            hidden = !showDealerCard && card == game.dealer.last()
                        )
                    }
                }
                Text(
                    if (showDealerCard) "Очки: ${score(game.dealer)}" else "Очки: ?",
                    color = Gold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            game.message,
            color = Gold,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar("🧑🏻", "ИГРОК")
            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ВАШИ КАРТЫ", color = Color.White, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(game.player, key = { it.id }) { card ->
                        PlayingCardView(card)
                    }
                }
                Text("Очки: ${score(game.player)}", color = Gold)
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameAction("ВЗЯТЬ КАРТУ", !game.finished) { hit() }
            GameAction("ХВАТИТ", !game.finished) { stand() }
            GameAction("НОВАЯ РАЗДАЧА", game.finished) { restart() }
        }
    }
}

@Composable
fun BluetoothPanel(connection: BluetoothConnection) {
    val context = LocalContext.current
    val status by connection.status.collectAsState()
    val message by connection.messages.collectAsState()

    var devices by remember {
        mutableStateOf(emptyList<android.bluetooth.BluetoothDevice>())
    }

    val permissions = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            try {
                devices = connection.getPairedDevices()
            } catch (_: SecurityException) {
            }
        }
    }

    fun withPermissions(action: () -> Unit) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            action()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark, RoundedCornerShape(12.dp))
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "BLUETOOTH: $status",
            color = Gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Button(
                onClick = {
                    withPermissions { connection.startServer() }
                },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("СОЗДАТЬ КОМНАТУ", fontSize = 10.sp)
            }

            Button(
                onClick = {
                    withPermissions {
                        devices = connection.getPairedDevices()
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("СОПРЯЖЁННЫЕ", fontSize = 10.sp)
            }

            Button(
                onClick = { connection.disconnect() },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("ОТКЛЮЧИТЬ", fontSize = 10.sp)
            }
        }

        if (devices.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(devices) { device ->
                    Button(
                        onClick = {
                            withPermissions {
                                connection.connect(device)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 7.dp)
                    ) {
                        Text(device.name ?: "Телефон", fontSize = 10.sp)
                    }
                }
            }
        }

        if (message.isNotEmpty()) {
            Text("Сообщение: $message", color = Color.White, fontSize = 10.sp)
        }

        Button(
            onClick = { connection.sendMessage("HELLO") },
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("ПРОВЕРИТЬ СВЯЗЬ", fontSize = 10.sp)
        }
    }
}

@Composable
fun Avatar(emoji: String, name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF123D32), RoundedCornerShape(50))
                .border(2.dp, Gold, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 34.sp)
        }
        Text(name, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlayingCardView(card: PlayingCard, hidden: Boolean = false) {
    var visible by remember(card.id) { mutableStateOf(false) }

    LaunchedEffect(card.id) {
        delay(40)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
    ) {
        if (hidden) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(62.dp)
                    .background(Color(0xFF173B85), RoundedCornerShape(7.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("★", color = Gold, fontSize = 23.sp)
            }
        } else {
            val red = card.suit == "♥" || card.suit == "♦"
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .height(62.dp)
                    .background(Color.White, RoundedCornerShape(7.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(7.dp))
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    card.rank,
                    color = if (red) Color.Red else Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    card.suit,
                    color = if (red) Color.Red else Color.Black,
                    fontSize = 21.sp
                )
            }
        }
    }
}

@Composable
fun GameAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF123D32),
            contentColor = Gold
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(42.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
