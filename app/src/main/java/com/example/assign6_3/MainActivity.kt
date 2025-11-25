package com.example.assign6_3

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.assign6_3.ui.theme.Assign6_3Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO), 0
        )

        enableEdgeToEdge()
        setContent {
            Assign6_3Theme {
                SoundMeterScreen()
            }
        }
    }
}

@Composable
fun SoundMeterScreen() {
    var dbValue by remember { mutableStateOf(0f) }
    var isLoud by remember { mutableStateOf(false) }

    val thresholdDb = 70f   // you can change this

    LaunchedEffect(true) {
        measureSoundLevels { db ->
            dbValue = db
            isLoud = db > thresholdDb
        }
    }

    SoundMeterUI(dbValue = dbValue, isLoud = isLoud, threshold = thresholdDb)
}

@SuppressLint("MissingPermission")
suspend fun measureSoundLevels(onDbMeasured: (Float) -> Unit) {
    withContext(Dispatchers.IO) {

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecord = AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val buffer = ShortArray(bufferSize)

        audioRecord.startRecording()

        while (true) {
            val read = audioRecord.read(buffer, 0, buffer.size)

            if (read > 0) {
                // Compute RMS amplitude
                var sum = 0.0
                for (i in 0 until read) sum += buffer[i] * buffer[i]
                val rms = sqrt(sum / read)

                // Convert to dB (relative)
                // 32767 = max value of 16-bit PCM
                val db = 20 * ln(rms / 32767.0).toFloat() / ln(10f) + 90

                onDbMeasured(db)
            }

            delay(100) // smooth updates
        }
    }
}

@Composable
fun SoundMeterUI(dbValue: Float, isLoud: Boolean, threshold: Float) {

    val normalized = ((dbValue + 90) / 90).coerceIn(0f, 1f)
    // db range = roughly -90 dB (silent) to 0 dB (max)

    val barColor = when {
        isLoud -> Color.Red
        normalized > 0.6f -> Color.Yellow
        else -> Color.Green
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Sound Level: ${dbValue.toInt()} dB")

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalized)
                    .background(barColor, RoundedCornerShape(10.dp))
            )
        }

        Spacer(Modifier.height(20.dp))

        if (isLoud) {
            Text(
                "⚠️ Noise too high!",
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Assign6_3Theme {
        Greeting("Android")
    }
}