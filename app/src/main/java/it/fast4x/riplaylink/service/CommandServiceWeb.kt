package it.fast4x.riplaylink.service

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import io.ktor.http.ContentType
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.port
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import io.netty.handler.codec.DefaultHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import io.ktor.server.plugins.calllogging.CallLogging
import it.fast4x.riplaylink.appContext
import kotlinx.coroutines.runBlocking

fun Application.module() {
    install(CallLogging)

    monitor.subscribe(ApplicationStarted) {
        println("CommandServiceWeb ApplicationStarted")
    }

    monitor.subscribe(ApplicationStopped) {
        println("CommandServiceWeb ApplicationStopped")
    }

    routing {
        println("CommandServiceWeb routing /")
        get("/") {
            println("CommandServiceWeb get /")
            call.respondText("CommandServiceWeb Hello, world from Module in ${Build.MODEL}!")
        }
    }
}

fun ApplicationEngine.Configuration.envConfig() {

    connector {
        host = "0.0.0.0"
        port = 13456
    }
    connector {
        host = "0.0.0.0"
        port = 9090
    }
}

fun ApplicationEngine.Configuration.envConfigWithSSL() {

    println("CommandServiceWeb ApplicationEngine.Configuration envConfig")

    connector {
        port = 8000
    }

        val keyStoreFile = File("${appContext().externalCacheDir?.absolutePath}/build/keystore.jks")
        val keyStore = buildKeyStore {
            certificate("sampleAlias") {
                password = "foobar"
                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
            }
        }
        keyStore.saveToFile(keyStoreFile, "123456")

        sslConnector(
            keyStore = keyStore,
            keyAlias = "sampleAlias",
            keyStorePassword = { "123456".toCharArray() },
            privateKeyPassword = { "foobar".toCharArray() }
        ) {
            port = 8443
            keyStorePath = keyStoreFile
        }
}

class CommandServiceWeb(
    private val activity: Activity,
    onCommandLoad: (mediaId: String, position: Float) -> Unit,
    onCommandPlay: () -> Unit,
    onCommandPause: () -> Unit
) {

    private val logger = LoggerFactory.getLogger(CommandServiceWeb::class.java)


// Not work
//    private val server by lazy {
//        embeddedServer(Netty, applicationEnvironment { log = logger }, {
//            ApplicationEngine.Configuration().envConfig()
//        }, module = Application::module)
//    }

// Not work
//    val appProperties = serverConfig {
//        module { module() }
//        applicationEnvironment{ log = logger }
//    }
//    private val server by lazy {
//        embeddedServer(Netty, appProperties) {
//            ApplicationEngine.Configuration().envConfig()
//        }
//    }

    private val server by lazy {
        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                //host = "127.0.0.1"
                port = 8080
            })

            envConfigWithSSL()
            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        }) {
            module()
//            routing {
//                get("/") {
//                    call.respondText("Hello, world!")
//                }
//            }
        }
    }


//Work
//    private val server by lazy {
//        embeddedServer(Netty, port = 8000, watchPaths = emptyList()) {
//
//            install(CallLogging)
//
//            routing {
//                get("/") {
//                    call.respondText("CommandServiceWeb All good here in ${Build.MODEL}", ContentType.Text.Plain)
//                }
//            }
//        }
//    }

    val onCommandLoad: (mediaId: String, position: Float) -> Unit = { id, position ->
        onCommandLoad(id, position)
    }
    val onCommandPlay: () -> Unit = {
        onCommandPlay()
    }
    val onCommandPause: () -> Unit = {
        onCommandPause()
    }

    fun stop() {
        isServiceSunning = false
    }


    fun start() {

        ipAddress = findIPAddress(activity)
        isServiceSunning = true

        server.start(wait = false)

        realPort = runBlocking { server.engine.environment.config.port.toString() }
        resolvedConnectors = runBlocking { server.engine.resolvedConnectors() }

//        embeddedServer(Netty, applicationEnvironment { log = LoggerFactory.getLogger("ktor.application") }, {
//            ApplicationEngine.Configuration().envConfig()
//        }, module = Application::module).start(wait = true)

        println("CommandServiceWeb is listening at ${ipAddress} : ${realPort}")
        println("CommandServiceWeb resolvedConnectors ${resolvedConnectors}")

    }





    @Synchronized
    fun ipAddress(): String? = ipAddress
    @Synchronized
    fun isServiceSunning(): Boolean = isServiceSunning
    @Synchronized
    fun mediaId(): String = mediaId

    private fun findIPAddress(context: Context): String? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            return if (wifiManager.connectionInfo != null) {
                val wifiInfo = wifiManager.connectionInfo
                InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(wifiInfo.ipAddress)
                        .array()
                ).hostAddress
            } else
                null
        } catch (e: Exception) {
            Log.e(CommandServiceWeb::class.java.simpleName, "Error finding IpAddress: ${e.message}", e)
        }
        return null
    }

    companion object {
        private var serviceSocket: ServerSocket? = null
        private var isServiceSunning = false
        private var ipAddress: String? = null
        private var port: String? = null
        private var realPort: String? = null
        private var resolvedConnectors: List<EngineConnectorConfig?> = emptyList()
        var mediaId: String = ""
        const val commandLoad: String = "load"
        const val commandPlay: String = "play"
        const val commandPause: String = "pause"
    }
}