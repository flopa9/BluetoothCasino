package com.example.bluetoothcasino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101512)
private val CardColor = Color(0xFF1C2820)
private val Gold = Color(0xFFE8BD63)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CasinoApp()
        }
    }
}

@Composable
fun CasinoApp() {
    var selectedGame by remember { mutableStateOf("Главная") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            if (selectedGame == "Главная") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "BLUETOOTH CASINO",
                            color = Gold,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Добро пожаловать!", color = Color.White)
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CardColor
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("ТВОЙ БАЛАНС", color = Color.LightGray)
                                Text(
                                    "10 000 🪙",
                                    color = Gold,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Только виртуальные фишки",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "ИГРЫ",
                            color = Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        GameCard("♠", "Блэкджек 21", "Сыграй против бота") {
                            selectedGame = "Блэкджек 21"
                        }
                    }

                    item {
                        GameCard("🎡", "Рулетка", "Испытай удачу") {
                            selectedGame = "Рулетка"
                        }
                    }

                    item {
                        GameCard("✈", "Самолётик", "Следи за полётом") {
                            selectedGame = "Самолётик"
                        }
                    }

                    item {
                        HorizontalDivider(color = CardColor)
                        Text(
                            "BLUETOOTH",
                            color = Gold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Локальная игра для двух телефонов",
                            color = Color.LightGray
                        )
                        Button(
                            onClick = { selectedGame = "Bluetooth" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Подключить игрока", color = Color.Black)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        selectedGame,
                        color = Gold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Этот режим пока разрабатывается.",
                        color = Color.White
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { selectedGame = "Главная" }
                    ) {
                        Text("Назад")
                    }
                }
            }
        }
    }
}

@Composable
fun GameCard(
    symbol: String,
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, fontSize = 32.sp)
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    name,
                    color = Gold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(description, color = Color.LightGray)
            }
        }
    }
}
