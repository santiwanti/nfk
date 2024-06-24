package com.zerodea.nfk.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                mainViewModel.startReading()
            },
        ) {
            Text("read tag")
        }

        mainViewModel.readTag.collectAsState().value?.let { nfcCard ->
            when (nfcCard) {
                is NfcCard.Ndef -> Text(nfcCard.message.records.joinToString(separator = "\n") { "${it.id}\t ${it.tnf}\t ${it.type}\t ${it.payload}" })
                else -> throw IllegalStateException("tag can't be a random type")
            }
        }
    }
}