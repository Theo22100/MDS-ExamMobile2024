package com.example.myapplication

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimerScreen()
                }
            }
        }
    }
}

@Composable
fun TimerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current // Obtenir le contexte actuel de l'application

    var timers by remember { mutableStateOf(listOf(TimerState(3, TimerStatus.PAUSED), TimerState(6, TimerStatus.PAUSED), TimerState(9, TimerStatus.PAUSED))) } // Liste mutable de minuteurs avec des valeurs initiales pour oeufs
    var customTimer by remember { mutableStateOf(0) } // Minuteur personnalisé
    var message by remember { mutableStateOf("") } // Message d'erreur ou de confirmation
    var editingTimerIndex by remember { mutableStateOf(-1) } // Index du minuteur en cours d'édition

    Column(
        modifier = Modifier
            .fillMaxSize() // Prendre toute taille disponible
            .padding(16.dp), // Ajouter marges
        horizontalAlignment = Alignment.CenterHorizontally, // Centrer horizontalement
        verticalArrangement = Arrangement.Top // Aligner haut verticalement
    ) {
        Text(
            text = "Minuteurs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp)) // Espacement

        timers.forEachIndexed { index, timer ->
            if (index == editingTimerIndex) { // Si le minuteur est en cours d'édition
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = customTimer.toString(), // Valeur du minuteur personnalisé
                        onValueChange = {
                            customTimer = it.toIntOrNull() ?: 0 // MAJ le minuteur personnalisé
                        },
                        label = { Text("Modifier (minutes)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Espacement
                    Button(onClick = {
                        if (customTimer > 0) {
                            timers = timers.toMutableList().also {
                                it[index] = TimerState(customTimer, TimerStatus.PAUSED) // MAJ le minuteur en cours d'édition
                            }
                            editingTimerIndex = -1 // Terminer l'édition
                        }
                    }) {
                        Text("Valider")
                    }
                }
            } else {
                CountdownTimer(
                    timerState = timer,
                    onTimerFinished = { msg ->
                        message = msg // Afficher msg fin minuteur
                    },
                    onTimerStateChanged = { newTimerState ->
                        timers = timers.toMutableList().also { it[index] = newTimerState } // MAJ ETAT MINUTEUR
                    },
                    onTimerRemoved = {
                        if (index !in listOf(0, 1, 2)) { // Ne pas Enlever Sup minuteur oeuf
                            timers = timers.toMutableList().also { it.removeAt(index) } // Supprimer le minuteur
                        }
                    },
                    onTimerEdit = {
                        editingTimerIndex = index // Commencer édition du minuteur
                        customTimer = timer.value // Définir la valeur du minuteur en cours d'édition
                    },
                    onTimerRestart = {
                        timers = timers.toMutableList().also { it[index] = TimerState(timer.value, TimerStatus.RUNNING) } // Redémarrer minuteur
                    },
                    context = context,
                    index = index // Passer index minuteur
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = customTimer.toString(), // Valeur minuteur personnalisé
                onValueChange = {
                    customTimer = it.toIntOrNull() ?: 0 // MAJ minuteur personnalisé
                },
                label = { Text("Minuteur Custom (minutes)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (customTimer > 0) {
                    timers = timers + TimerState(customTimer, TimerStatus.PAUSED) // Ajouter le nouveau minuteur à la liste
                    customTimer = 0 // Réinitialiser la valeur du minuteur personnalisé après l'ajout
                }
            }) {
                Text("Ajouter Minuteur")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error) // Afficher le message d'erreur
        }
    }
}


enum class TimerStatus { RUNNING, PAUSED }

data class TimerState(val value: Int, val status: TimerStatus)

@Composable
fun CountdownTimer(
    timerState: TimerState,
    onTimerFinished: (String) -> Unit,
    onTimerStateChanged: (TimerState) -> Unit,
    onTimerRemoved: () -> Unit,
    onTimerEdit: () -> Unit,
    onTimerRestart: () -> Unit,
    context: Context,
    index: Int // Index du minuteur pour oeuf
) {
    var remainingTime by remember { mutableStateOf(timerState.value * 60L) } // Temps restant en secondes
    var isRunning by remember { mutableStateOf(timerState.status == TimerStatus.RUNNING) } // Booléen pour indiquer si le minuteur en exécution

    LaunchedEffect(isRunning, remainingTime) {
        // Effet lancé lorsque le minuteur est en cours d'exécution et qu'il reste du temps
        if (isRunning && remainingTime > 0) {
            delay(1000L)
            remainingTime--
            if (remainingTime == 0L) {
                // Si le temps restant est écoulé
                val timerText = if (timerState.value == 1) {
                    "Le minuteur de ${timerState.value} minute est fini !"
                } else {
                    "Le minuteur de ${timerState.value} minutes est fini !"
                }
                onTimerFinished(timerText) // Appeler fonction de retour avec le message de fin du minuteur
                isRunning = false // Mettre minuteur à l'état non en cours d'exécution
                playTimerFinishedSound(context) // Jouer le son de fin de minuteur
            }
        }
    }

    val minutes = TimeUnit.SECONDS.toMinutes(remainingTime).toString().padStart(2, '0') // Conversion temps restant en minutes
    val seconds = (remainingTime % 60).toString().padStart(2, '0') // Calcul des secondes restantes

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Affichage du temps restant
        Text(text = "$minutes:$seconds", style = MaterialTheme.typography.bodyLarge)
        Row {
            if (index in listOf(0, 1, 2) || timerState.value in listOf(3, 6, 9)) { // Si minuteurs oeufs
                // Bouton pour mettre en pause ou reprendre
                Button(onClick = {
                    onTimerStateChanged(
                        TimerState(
                            timerState.value,
                            if (isRunning) TimerStatus.PAUSED else TimerStatus.RUNNING
                        )
                    )
                    isRunning = !isRunning
                }) {
                    Text(if (isRunning) "▐▐" else "▶")  // Affichage du symbole Pause ou Play
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Bouton pour redémarrer le minuteur
                Button(onClick = {
                    onTimerRestart() // Appeler fonction de redémarrage du minuteur
                    remainingTime = timerState.value * 60L // Réinitialiser temps restant
                }) {
                    Text("\uD83D\uDD03")
                }
            } else { // Si le minuteur n'est pas spécial
                // Bouton pour mettre en pause ou reprendre
                Button(onClick = {
                    onTimerStateChanged(
                        TimerState(
                            timerState.value,
                            if (isRunning) TimerStatus.PAUSED else TimerStatus.RUNNING
                        )
                    )
                    isRunning = !isRunning
                }) {
                    Text(if (isRunning) "▐▐" else "▶") // Affichage du symbole Pause ou Play
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Bouton pour éditer le minuteur
                Button(onClick = {
                    onTimerEdit()
                }) {
                    Text("✏\uFE0F")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Bouton pour redémarrer le minuteur
                Button(onClick = {
                    onTimerRestart()
                    remainingTime = timerState.value * 60L
                }) {
                    Text("\uD83D\uDD03")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Bouton pour supprimer le minuteur
                Button(onClick = {
                    onTimerRemoved()
                }) {
                    Text("❌")
                }
            }
        }
    }
}

//Bruit fin
fun playTimerFinishedSound(context: Context) {
    val mediaPlayer = MediaPlayer.create(context, R.raw.timer_finished_sound)
    mediaPlayer.setOnCompletionListener { mp -> mp.release() }
    mediaPlayer.start()
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    MyApplicationTheme {
        TimerScreen()
    }
}

