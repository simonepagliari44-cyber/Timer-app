package com.simonecompany.timer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simonecompany.timer.viewmodel.TimerState
import com.simonecompany.timer.viewmodel.TimerViewModel

@Composable
fun TimerSection(viewModel: TimerViewModel) {
    val timers by viewModel.timers.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (timers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun timer attivo.\nPremi + per aggiungerne uno.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(timers, key = { _, timer -> timer.id }) { index, timer ->
                    TimerCard(
                        number = index + 1,
                        timer = timer,
                        onDurationChange = { h, m, s -> viewModel.updateDuration(timer.id, h, m, s) },
                        onStart = { viewModel.startTimer(timer.id) },
                        onStop = { viewModel.stopTimer(timer.id) },
                        onReset = { viewModel.resetTimer(timer.id) },
                        onRemove = { viewModel.removeTimer(timer.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                requestNotificationPermission()
                viewModel.addTimer()
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi timer")
        }
    }
}

@Composable
fun TimerCard(
    number: Int,
    timer: TimerState,
    onDurationChange: (String, String, String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer $number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Rimuovi",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTime(timer.remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                color = if (timer.isFinished)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!timer.isRunning && timer.remainingSeconds == timer.totalSeconds && !timer.isFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationField(
                        value = timer.hours,
                        label = "Ore",
                        onValueChange = { onDurationChange(it, timer.minutes, timer.seconds) },
                        modifier = Modifier.weight(1f)
                    )
                    DurationField(
                        value = timer.minutes,
                        label = "Minuti",
                        onValueChange = { onDurationChange(timer.hours, it, timer.seconds) },
                        modifier = Modifier.weight(1f)
                    )
                    DurationField(
                        value = timer.seconds,
                        label = "Secondi",
                        onValueChange = { onDurationChange(timer.hours, timer.minutes, it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onStart,
                    enabled = !timer.isRunning && timer.remainingSeconds > 0
                ) {
                    Text("Start")
                }
                TextButton(
                    onClick = onStop,
                    enabled = timer.isRunning
                ) {
                    Text("Stop")
                }
                TextButton(
                    onClick = onReset,
                    enabled = timer.remainingSeconds != timer.totalSeconds || timer.isRunning
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun DurationField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%02d:%02d:%02d".format(hours, mins, secs)
}