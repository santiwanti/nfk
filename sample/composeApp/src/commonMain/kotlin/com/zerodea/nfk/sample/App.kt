package com.zerodea.nfk.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import utils.displayAsHex

@Composable
@Preview
fun App() {
    MaterialTheme {
        SampleApp()
    }
}

@Composable
private fun SampleApp() {
    val mainViewModel = remember { MainViewModel() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier,
                snackbar = { snackbarData ->
                    Text(snackbarData.visuals.message)
                }
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    if (mainViewModel.isEnabled()) {
                        mainViewModel.startReading()
                    } else {
                        println("not enabled")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Enable NFC")
                        }
                    }
                },
            ) {
                Text("read tag")
            }

            mainViewModel.readTag.collectAsState().value?.let { nfcCard ->
                when (nfcCard) {
                    is NfcCard.Ndef -> Text(nfcCard.message.records.joinToString(separator = "\n") { "${it.id}\t ${it.tnf}\t ${it.type}\t ${it.payload}" })
                    is NfcCard.NfcA -> Text("id: ${nfcCard.id.displayAsHex()}\natqa: ${nfcCard.atqa.displayAsHex()}\nsak: ${nfcCard.sak.displayAsHex()}")
                    is NfcCard.NfcB -> Text("id: ${nfcCard.id.displayAsHex()}\nApp Data: ${nfcCard.appData.displayAsHex()}\nprotocol info: ${nfcCard.protocolInfo.displayAsHex()}")
                    is NfcCard.NfcF -> Text("id: ${nfcCard.id.displayAsHex()}\nsystemCode: ${nfcCard.systemCode.displayAsHex()}\nmanufacturer: ${nfcCard.manufacturer.displayAsHex()}")
                    is NfcCard.NfcV -> Text("id: ${nfcCard.id.displayAsHex()}\ndsfId: ${nfcCard.dsfId.displayAsHex()}\nResponseFlags: ${nfcCard.responseFlags.displayAsHex()}")
                    is NfcCard.IsoDep.A -> Text("id: ${nfcCard.id.displayAsHex()}\nhistoricalBtyes: ${nfcCard.historicalBytes.displayAsHex()}\natqa: ${nfcCard.atqa.displayAsHex()}\nsak: ${nfcCard.sak}")
                    is NfcCard.IsoDep.B -> Text("id: ${nfcCard.id.displayAsHex()}\nHiLayerResponse: ${nfcCard.hiLayerResponse.displayAsHex()}\nApp Data: ${nfcCard.appData.displayAsHex()}\nprotocol info: ${nfcCard.protocolInfo.displayAsHex()}")
                    else -> throw IllegalStateException("tag can't be a random type")
                }
            }
        }
    }
}