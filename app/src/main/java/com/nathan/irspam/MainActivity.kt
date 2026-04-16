package com.nathan.irspam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nathan.irspam.data.IrDatabase
import com.nathan.irspam.logic.IrManager
import com.nathan.irspam.ui.theme.IRSpamTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRSpamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IRSpamApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRSpamApp() {
    val context = LocalContext.current
    val irManager = remember { IrManager(context) }
    val scope = rememberCoroutineScope()
    var isSpamming by remember { mutableStateOf(false) }
    var currentDeviceName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!irManager.hasIrEmitter()) {
                ErrorMessage(stringResource(id = R.string.no_ir_blaster))
            }

            // Section Spam
            ElevatedCard(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.spam_mode),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isSpamming) {
                                isSpamming = false
                            } else {
                                isSpamming = true
                                scope.launch {
                                    while (isSpamming) {
                                        for (device in IrDatabase.commonTVs) {
                                            if (!isSpamming) break
                                            currentDeviceName = "${device.brand} ${device.model}"
                                            irManager.transmitProntoHex(device.powerCode)
                                            delay(300) // Délai pour éviter de saturer le récepteur
                                        }
                                        if (!isSpamming) break
                                        delay(1000) // Pause entre les cycles
                                    }
                                    currentDeviceName = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpamming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSpamming) stringResource(id = R.string.stop_spam) else stringResource(id = R.string.start_spam))
                    }
                    if (isSpamming) {
                        Text(
                            text = "Envoi : $currentDeviceName",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }

            Text(
                text = "Liste des appareils",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )

            LazyColumn {
                items(IrDatabase.commonTVs) { device ->
                    ListItem(
                        headlineContent = { Text(device.brand) },
                        supportingContent = { Text(device.model) },
                        trailingContent = {
                            FilledIconButton(onClick = { irManager.transmitProntoHex(device.powerCode) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Send Power")
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}
