package it.fast4x.riplaylink

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.fast4x.riplaylink.service.CommandService
import it.fast4x.riplaylink.ui.Player
import it.fast4x.riplaylink.ui.theme.RiPlayLinkTheme
import it.fast4x.riplaylink.service.registerNsdService

class MainActivity : AppCompatActivity() {

    //private lateinit var commandService: CommandService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerNsdService()
//        commandService = CommandService(this)
//        commandService.start()
        setContent {
            RiPlayLinkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Player(innerPadding)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        commandService.stop()
        registerNsdService(unRegister = true)
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
    RiPlayLinkTheme {
        Greeting("Android")
    }
}