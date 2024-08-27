package com.example.numbasapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import com.example.numbasapp.ui.theme.NumbasAppTheme
import org.bouncycastle.crypto.generators.SCrypt
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NumbasAppTheme {
                val intent: Intent = intent
                val action: String? = intent.action
                val data: Uri? = intent.data
                val salt = "45ab2cf2e139c01f8447d17dc653d585"

                Log.d("MainActivity", "Intent Action: $action")
                Log.d("MainActivity", "Intent Data: $data")

                // State variables
                var showPasswordDialog by remember { mutableStateOf(false) }
                //var url by remember { mutableStateOf("") }
                var password by remember {mutableStateOf("")}
                //var authorization by remember { mutableStateOf("")}
                var launchData by remember { mutableStateOf(LaunchData("",""))}

                // Handle custom URL intent
                if (Intent.ACTION_VIEW == action && data != null && data.scheme == "numbas") {
                    //val host = data.host ?: ""
                    //val token = data.pathSegments.lastOrNull()
                    //url = "https://www.numbas.org.uk/lockdown-app/debug_headers.php" //need to use token
                    showPasswordDialog = true
                }

                // Main UI
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (showPasswordDialog) {
                        PasswordDialog(
                            onDismiss = { showPasswordDialog = false },
                            onConfirm = { enteredPassword ->
                                password = enteredPassword
                                launchData = decryptSettings(password,salt,uri = data)
                                //authorization = launchData.token
                                showPasswordDialog = false
                            }
                        )
                    } else if (launchData.url.isNotEmpty() && launchData.token.isNotEmpty()) {
                        //LoadWebPage("https://www.numbas.org.uk/lockdown-app/debug_headers.php", mapOf("Authorization" to launchData.token))
                        LoadWebPage(launchData.url, mapOf("Authorization" to "Basic " + launchData.token))
                    } else {
                        InfoPage()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun PasswordPagePreview() {
    NumbasAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PasswordPage()
        }
    }
}

@Composable
fun PasswordPage() {
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
            painter = painterResource(R.drawable.numbas_logo),
            contentDescription = "")
            Text(
                text = "Opening a Numbas Link",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
        }
        Column (horizontalAlignment = Alignment.CenterHorizontally)  {
            Text(
                text = "Enter the password to open this Numbas link.\n Your instructor should have given you the password.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = password,
                onValueChange = {password = it},
                label = {Text("Enter Password")},
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {/*TODO*/}){ //make deactivated when password empty
                Text(text = "Open")
            }

        }
        Spacer(modifier = Modifier.height(32.dp))
    }

}


@Composable
fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Enter Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


class NumbasWebViewClient(private val headers: Map<String, String>) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // Intercept URL loading and add headers
        if (view != null && request != null) {
            view.loadUrl(request.url.toString(), headers)
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        // Add headers to the request
        request?.requestHeaders?.putAll(headers)
        return super.shouldInterceptRequest(view, request)
    }
}

@Composable
fun LoadWebPage(url: String, extraHeaders: Map<String,String>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = NumbasWebViewClient(extraHeaders)
                settings.javaScriptEnabled = true
                loadUrl(url, extraHeaders)
            }
        }
    )
}

@Composable
fun InfoPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            //.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.numbas_logo),
            contentDescription = "")//,modifier = Modifier.fillMaxWidth()) //width fill doesn't work
        Column {
            Text(
                text = "This is the Numbas lockdown app",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "You don't normally need to open this app directly.\n Click on a Numbas link in a browser to use this app",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        TestButton()
    }
}

@Composable
fun TestButton() {
    val ctx = LocalContext.current
    Button(onClick = {
        val uri = Uri.parse("https://www.numbas.org.uk/lockdown-app/test/")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        ctx.startActivity(intent)
    }) {
        Text(text = "Open the test page")
    }

}


data class LaunchData(val url: String, val token: String)


//https://www.baeldung.com/kotlin/advanced-encryption-standard

fun aesDecrypt(encryptedData: ByteArray, secretKey: SecretKey, ivParameterSpec: IvParameterSpec): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
    return cipher.doFinal(encryptedData)
}

fun generateScryptKey(password: String, salt: String): SecretKeySpec {
    val key = SCrypt.generate(password.toByteArray(),
        salt.toByteArray(),
        16384,//default in nodejs crypto,
        8,//default in nodejs crypto,
        1,
        24) //selected value
    val secretKey = SecretKeySpec(key, "AES")
    return secretKey
}

// currently using an experimental function to retrieve the token from the encoded hex array, #
// consider re-writing into something more permanent
// https://www.baeldung.com/kotlin/string-hex-byte-array-conversion#:~:text=5.,Using%20BigInteger&text=We%20convert%20a%20hex%20string,return%20the%20corresponding%20byte%20array.
@OptIn(ExperimentalStdlibApi::class)
fun decryptSettings(password: String, salt: String, uri: Uri?) : LaunchData {
    if (uri == null) {
        return LaunchData("","") //probably should be a proper error
    }
    val hexEncrypted: ByteArray = uri.path!!.drop(1).hexToByteArray()
    val iv  = IvParameterSpec(hexEncrypted.sliceArray(0..15))
    val data : ByteArray = hexEncrypted.sliceArray(16..hexEncrypted.size-1)

    val secretKey = generateScryptKey(password, salt)

    val decryptActual = aesDecrypt(data, secretKey,iv)
    val clearText = String(decryptActual)

    val json = JSONObject(clearText)

    val launchURL: String = json.getString("url")
    val token: String = json.getString("token")

    return LaunchData(url = launchURL, token = token)
}