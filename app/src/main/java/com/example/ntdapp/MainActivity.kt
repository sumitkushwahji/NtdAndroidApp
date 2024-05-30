package com.example.ntdapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ntdapp.ui.theme.NtdAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NtdAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NtpSyncApp()
                }
            }
        }
    }
}

@Composable
fun NtpSyncApp() {
    var server by remember { mutableStateOf(TextFieldValue("time.nplindia.org")) }
    var syncTime by remember { mutableStateOf(TextFieldValue("60")) }
    var bias by remember { mutableStateOf(TextFieldValue("1")) }
    var output by remember { mutableStateOf("Output will be displayed here") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Enter NTP Server") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = syncTime,
            onValueChange = { syncTime = it },
            label = { Text("Enter Sync Time (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bias,
            onValueChange = { bias = it },
            label = { Text("Enter Bias (seconds)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    val serverText = server.text
                    val syncTimeInt = syncTime.text.toIntOrNull() ?: 60
                    val biasInt = bias.text.toIntOrNull() ?: 0
                    startSync(serverText, syncTimeInt, biasInt) {
                        output = it
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Start Sync")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(output)
    }
}

suspend fun startSync(server: String, syncTime: Int, bias: Int, onOutput: (String) -> Unit) {
    try {
        while (true) {
            val timestamp = getNtpTime(server) + bias
            val date = Date(timestamp * 1000)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = sdf.format(date)
            onOutput("NTP Time: $formattedDate")
            delay((syncTime * 60 * 1000).toLong())
        }
    } catch (e: Exception) {
        onOutput("Error: ${e.message}")
    }
}

suspend fun getNtpTime(server: String): Long {
    return withContext(Dispatchers.IO) {
        try {
            val port = 123
            val buffer = ByteArray(48)
            buffer[0] = 0x1B.toByte()

            val address = InetAddress.getByName(server)
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            val socket = DatagramSocket()
            socket.soTimeout = 5000 // Set a timeout for socket operations (e.g., 5 seconds)
            socket.send(packet)

            socket.receive(packet)
            socket.close()

            // Extract the 32-bit seconds part from the response (bytes 40-43)
            val seconds = ((buffer[40].toLong() and 0xFFL) shl 24) or
                    ((buffer[41].toLong() and 0xFFL) shl 16) or
                    ((buffer[42].toLong() and 0xFFL) shl 8) or
                    (buffer[43].toLong() and 0xFFL)

            // Convert NTP time to Unix time
            val epochDifference = 2208988800L
            seconds - epochDifference
        } catch (e: Exception) {
            throw IOException("Failed to retrieve NTP time from server: $server", e)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NtdAppTheme {
        NtpSyncApp()
    }
}
