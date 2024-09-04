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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.numbasapp.ui.theme.NumbasAppTheme
import org.bouncycastle.crypto.generators.SCrypt
import org.json.JSONObject
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource

private const val TAG = "NumbasMainActivity"

class Common{
    //this class provides the password which allows it to persist between activity launches,
    // so we can try the 'old' password as long as the app has not been closed.
    companion object{
        var password = ""
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        const val PASSWORD = "PASSWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NumbasAppTheme {
                val intent: Intent = intent
                val action: String? = intent.action
                val data: Uri? = intent.data
                val salt = "45ab2cf2e139c01f8447d17dc653d585"

                Log.d(TAG, "Intent Action: $action")
                Log.d(TAG, "Intent Data: $data")

                // State variables
                var showPasswordPage by remember { mutableStateOf(false)}
                var currentPasswordBad by remember { mutableStateOf(false)}
                //var password by remember {mutableStateOf("")}
                var launchData by remember { mutableStateOf(LaunchData("",""))}

                // Handle custom URL intent
                if (Intent.ACTION_VIEW == action && data != null && data.scheme == "numbas") {
                    if (Common.password != "") {
                        try {
                            launchData = decryptSettings(Common.password, salt, uri = data)
                            showPasswordPage = false
                        } catch (e: IncorrectPasswordException) {
                            currentPasswordBad = true
                            showPasswordPage = true
                        }
                    } else {
                        showPasswordPage = true
                    }
                }

                // Main UI
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (showPasswordPage) {
                        PasswordPage(
                            Common.password,
                            sendPassword = { enteredPassword ->
                                Common.password = enteredPassword
                                try {
                                    launchData = decryptSettings(Common.password, salt, uri = data)
                                    currentPasswordBad = false
                                    showPasswordPage = false
                                } catch (e: IncorrectPasswordException) {
                                    currentPasswordBad = true //this is not currently cascaded down....
                                }
                            }, currentPasswordBad
                            )
                    }
                     else if (launchData.url.isNotEmpty() && launchData.token.isNotEmpty()) {
                        LoadWebPage(launchData.url, mapOf("Authorization" to "Basic " + launchData.token))
                    } else {
                        InfoPage()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putString(PASSWORD, Common.password)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Common.password = savedInstanceState.getString(PASSWORD).toString()

        super.onRestoreInstanceState(savedInstanceState)
    }

}

@Composable
fun PasswordPage(savedPassword: String, sendPassword: (String) -> Unit, passwordBad: Boolean = false) {
    var password by remember { mutableStateOf(savedPassword) }
    var unsubmittedPassword by remember { mutableStateOf(false)}
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
                text = stringResource(R.string.opening_a_numbas_link),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
        }
        Column (horizontalAlignment = Alignment.CenterHorizontally)  {
            Text(
                text = stringResource(R.string.enter_password_instruction),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                singleLine = true,
                value = password,
                onValueChange = {
                    password = it
                    unsubmittedPassword = true
                },
                label = {Text(stringResource(R.string.enter_password))},
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    unsubmittedPassword = false
                    sendPassword(password)
                }),
                modifier = Modifier.onKeyEvent { event ->
                    return@onKeyEvent if (event.key.keyCode == Key.Enter.keyCode){
                        unsubmittedPassword = false
                        sendPassword(password)
                        true
                    } else {
                        false
                    }
                }
            )
            if (passwordBad and !unsubmittedPassword) {
                Text(text = stringResource(R.string.password_failed_text),color = Color.Red)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                unsubmittedPassword = false
                sendPassword(password)
            }){ //TODO: make deactivated when password empty
                Text(text = stringResource(R.string.open_link))
            }

        }
        Spacer(modifier = Modifier.height(32.dp))
    }

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
                settings.domStorageEnabled = true
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
                text = stringResource(R.string.lockdown_app_declaration),
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.use_a_link),
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
        Text(text = stringResource(R.string.open_test_page))
    }

}


data class LaunchData(val url: String, val token: String)
class IncorrectPasswordException(message: String) : Exception(message)

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

// currently using an experimental function to retrieve the token from the encoded hex array,
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

    try {
        val decryptedToken = aesDecrypt(data, secretKey,iv)
        val clearText = String(decryptedToken)

        val json = JSONObject(clearText)

        val launchURL: String = json.getString("url")
        val token: String = json.getString("token")

        return LaunchData(url = launchURL, token = token)
    }  catch (e: BadPaddingException) {
        if (e.toString() == "javax.crypto.BadPaddingException: error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT") {
            throw IncorrectPasswordException("Password $password does not allow decryption")
        }
    }
    return LaunchData("","") //should never reach this part but if it does there's an empty return because kotlin refuses to compile without it
}