package com.monjur.docscanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.monjur.docscanner.ui.theme.DocScannerTheme
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options =
            GmsDocumentScannerOptions.Builder().setScannerMode(SCANNER_MODE_FULL).setPageLimit(10)
                .setResultFormats(
                    RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF
                ).build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocScannerTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    var uriList by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            if (it.resultCode == RESULT_OK) {
                                val result =
                                    GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                                uriList = result?.pages?.map { page ->
                                    page.imageUri
                                } ?: emptyList()

                                result?.pdf?.let { pdf ->
                                    val fileOutput =
                                        FileOutputStream(File(filesDir, "${Date().time}.pdf"))

                                    contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
                                        inputStream.copyTo(fileOutput)
                                    }

                                }
                            }
                        })

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        uriList.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = uri.toString(),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener {
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(it).build()
                                    )

                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        it.localizedMessage,
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                        }) {
                            Text(text = "Scan Document")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DocScannerTheme {
        Greeting("Android")
    }
}