package com.simonecompany.timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simonecompany.timer.notification.TimerNotifier
import com.simonecompany.timer.ui.StopwatchSection
import com.simonecompany.timer.ui.TimerSection
import com.simonecompany.timer.ui.theme.TimerTheme
import com.simonecompany.timer.viewmodel.StopwatchViewModel
import com.simonecompany.timer.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {

    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStopTimerExtra(intent)
        enableEdgeToEdge()
        setContent {
            TimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimerApp(timerViewModel = timerViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStopTimerExtra(intent)
    }

    private fun handleStopTimerExtra(intent: Intent?) {
        val stopId = intent?.getIntExtra(TimerNotifier.EXTRA_STOP_TIMER_ID, -1) ?: -1
        if (stopId != -1) timerViewModel.stopTimer(stopId)
    }
}

@Composable
fun TimerApp(timerViewModel: TimerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val stopwatchViewModel: StopwatchViewModel = viewModel()

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Timer") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cronometro") }
                )
            }

            when (selectedTab) {
                0 -> TimerSection(viewModel = timerViewModel)
                1 -> StopwatchSection(viewModel = stopwatchViewModel)
            }
        }
    }
}