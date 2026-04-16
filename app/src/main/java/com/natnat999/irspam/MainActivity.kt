package com.natnat999.irspam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.natnat999.irspam.data.IrDatabase
import com.natnat999.irspam.data.IrDevice
import com.natnat999.irspam.data.IrType
import com.natnat999.irspam.logic.IrManager
import com.natnat999.irspam.ui.theme.IRSpamTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRSpamTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val tabs = listOf("Tous", "TV", "Clim", "Projo", "Audio", "Box")
    val deviceTypes = listOf(null, IrType.TV, IrType.AC, IrType.PROJECTOR, IrType.AUDIO, IrType.BOX)

    val filteredDevices = remember(selectedTabIndex, searchQuery) {
        val type = deviceTypes[selectedTabIndex]
        IrDatabase.allDevices.filter { device ->
            (type == null || device.type == type) &&
            (searchQuery.isEmpty() || device.brand.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { 
                        Column {
                            Text("IR SPAM", fontWeight = FontWeight.ExtraBold)
                            Text("${IrDatabase.allDevices.size} appareils chargés", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        if (isSpamming) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Barre de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Rechercher une marque...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) })
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            if (!irManager.hasIrEmitter()) {
                ErrorMessage("Matériel Infrarouge non détecté.")
            }

            // Bouton Spam Universel
            ElevatedCard(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSpamming) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isSpamming) "SPAM EN COURS..." else "SPAMMER LA SÉLECTION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                                        for (device in filteredDevices) {
                                            if (!isSpamming) break
                                            currentDeviceName = "${device.brand} (${device.type})"
                                            irManager.transmitProntoHex(device.powerCode)
                                            delay(200) // Très rapide !
                                        }
                                        if (!isSpamming) break
                                        delay(300)
                                    }
                                    currentDeviceName = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpamming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (isSpamming) Icons.Default.Stop else Icons.Default.FlashOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSpamming) "ARRÊTER" else "LANCER L'ATTAQUE")
                    }

                    AnimatedVisibility(visible = isSpamming) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cible actuelle : $currentDeviceName", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }
                }
            }

            // Liste
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredDevices) { device ->
                    ListItem(
                        headlineContent = { Text(device.brand, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${device.type} • ${device.info}") },
                        leadingContent = {
                            val icon = when (device.type) {
                                IrType.TV -> Icons.Default.Tv
                                IrType.AC -> Icons.Default.Air
                                IrType.PROJECTOR -> Icons.Default.Videocam
                                IrType.AUDIO -> Icons.Default.Speaker
                                IrType.DVD -> Icons.Default.Album
                                IrType.BOX -> Icons.Default.SettingsInputComponent
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            FilledIconButton(onClick = { irManager.transmitProntoHex(device.powerCode) }) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            }
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
