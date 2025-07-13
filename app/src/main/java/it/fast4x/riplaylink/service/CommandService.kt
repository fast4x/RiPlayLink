package it.fast4x.riplaylink.service

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.core.net.toUri

class CommandService(
    private val activity: Activity,
    onCommandLoad: (mediaId: String, position: Float) -> Unit,
    onCommandPlay: () -> Unit,
    onCommandPause: () -> Unit
) {

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
        try {
            if (serviceSocket != null) {
                serviceSocket!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun start() {
        isServiceSunning = true
        val serviceThread = Thread(Runnable {
            var socket: Socket
            var responseThread: ResponseThread?
            try {

                serviceSocket = ServerSocket(8000)
                //println("CommandService WebServerThread started listening ip ${httpServerSocket!!.localSocketAddress} on port ${httpServerSocket!!.localPort}")
                ipAddress = findIPAddress(activity)
                port = serviceSocket!!.localPort.toString()
                println("CommandService WebServerThread started listening ip $ipAddress on port ${serviceSocket!!.localPort}")
                while (isServiceSunning) {
                    socket = serviceSocket!!.accept()
                    responseThread = ResponseThread(socket)
                    responseThread.start()
                }
            } catch (e: Exception) {
                println("CommandService Exception ${e.message}")
                e.printStackTrace()
            }
        })
        serviceThread.start()
    }

    private inner class ResponseThread(var clientSocket: Socket) : Thread() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            try {
                BufferedReader(InputStreamReader(clientSocket.getInputStream())).use { bufferedReader ->
                    clientSocket.getOutputStream().use { outputStream ->
                        val input = bufferedReader.readLine()
                        println("CommandService ResponseThread received input $input")
                        val uri = input.toUri()
                        val parameters = uri.queryParameterNames.associateWith { uri.getQueryParameters(it) }
                        val command = parameters["command"]?.firstOrNull()?.replace("HTTP/1.1", "")?.replace(" ", "")
                        val requestMediaId = parameters["mediaId"]?.firstOrNull()?.replace("HTTP/1.1", "")?.replace(" ", "")
                        val position = parameters["position"]?.firstOrNull()?.replace("HTTP/1.1", "")?.replace(" ", "")

                        println("CommandService ResponseThread received uri parameters $command")
                        if (command != null) {
                            println("CommandService ResponseThread received input process $input")
                            if (command == commandLoad) {
                                if (requestMediaId?.isNotEmpty() == true) {
                                    println("CommandService ResponseThread received play mediaId $requestMediaId position")

                                    onCommandLoad(requestMediaId.toString(), position?.toFloat() ?: 0f)

                                    outputStream.write("HTTP/1.1 200 OK\r\n".toByteArray())
                                    outputStream.write("Server: Apache/0.8.4\r\n".toByteArray())
                                    outputStream.write("\r\n".toByteArray())

                                }
                            }
                            if (command == commandPlay) {
                                onCommandPlay()

                                outputStream.write("HTTP/1.1 200 OK\r\n".toByteArray())
                                outputStream.write("Server: Apache/0.8.4\r\n".toByteArray())
                                outputStream.write("\r\n".toByteArray())
                            }
                            if (command == commandPause) {
                                onCommandPause()

                                outputStream.write("HTTP/1.1 200 OK\r\n".toByteArray())
                                outputStream.write("Server: Apache/0.8.4\r\n".toByteArray())
                                outputStream.write("\r\n".toByteArray())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("CommandService ResponseThread Exception ${e.message}")
            }
        }
    }

    @Synchronized
    fun ipAddress(): String? = ipAddress
    @Synchronized
    fun isServiceSunning(): Boolean = isServiceSunning
    @Synchronized
    fun baseUrl(): String? = "http://$ipAddress:$port"
    @Synchronized
    fun commandPlayUrl(): String = "${baseUrl()}/$commandLoad"
    @Synchronized
    fun commandPauseUrl(): String = "${baseUrl()}/$commandPause"
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
            Log.e(CommandService::class.java.simpleName, "Error finding IpAddress: ${e.message}", e)
        }
        return null
    }

    companion object {
        private var serviceSocket: ServerSocket? = null
        private var isServiceSunning = false
        private var ipAddress: String? = null
        private var port: String? = null
        var mediaId: String = ""
        const val commandLoad: String = "load"
        const val commandPlay: String = "play"
        const val commandPause: String = "pause"
    }
}