package com.example.lecturerapplication.network

import android.util.Log
import com.google.gson.Gson
import com.example.lecturerapplication.models.ChatContentModel
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.Exception
import kotlin.concurrent.thread

class Server (
    private val networkMessageInterface :NetworkMessageInterface) {


    companion object {
        const val PORT: Int = 9999
    }
    //var serverIp: String = getLocalIpAddress()
    //private var svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
    private var svrSocket: ServerSocket? = null
    private var clientMap: HashMap<String, Socket> = HashMap()
    private var isRunning: Boolean = true
    //private val serverThread: Thread

    init {
        try {
            svrSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
            Log.d("SERVER", "Server initialized on Port: $PORT")

            // Start a thread to accept client connections
            thread {
                while (isRunning) {
                    try {
                        Log.d("SERVER", "Waiting for client connection...")
                        // Attempt to accept a connection
                        val clientConnectionSocket = svrSocket?.accept() // Blocking call
                        Log.e("SERVER", "The server has accepted a connection: ${clientConnectionSocket?.inetAddress?.hostAddress}")
                        clientConnectionSocket?.let { handleSocket(it) }
                    } catch (e: SocketException) {
                        // This occurs when the socket is closed
                        if (!isRunning) {
                            Log.d("SERVER", "Server socket closed, exiting accept loop.")
                        } else {
                            Log.e("SERVER", "SocketException while accepting: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("SERVER", "An error has occurred in the server: ${e.message}")
                    }
                }
                Log.d("SERVER", "Server thread exiting.")
            }
        } catch (e: Exception) {
            Log.e("SERVER", "Server initialization failed: ${e.message}")
        }
    }

    fun sendMessage(content: ChatContentModel, clientIp: String) {
        thread {
            try {
                val socket = clientMap[clientIp]
                if (socket == null || !socket.isConnected) {
                    throw Exception("No active connection for client IP: $clientIp")
                }

                // Convert content to JSON string
                val contentAsStr: String = Gson().toJson(content)

                // Write to the output stream
                val writer = socket.outputStream.bufferedWriter()
                writer.write("$contentAsStr\n")
                writer.flush()

                Log.e("SERVER", "Message sent to client $clientIp: ${content.message}")
            } catch (e: Exception) {
                Log.e("SERVER", "Failed to send message to client: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {
            clientMap[it] = socket
            Log.e("SERVER", "A new connection has been detected!")
            thread {
                val clientReader = socket.inputStream.bufferedReader()
                val clientWriter = socket.outputStream.bufferedWriter()
                var receivedJson: String?

                while(socket.isConnected){
                    try{
                        receivedJson = clientReader.readLine()
                        if (receivedJson!= null){
                            Log.e("SERVER", "Received a message from client $it")
                            val clientContent = Gson().fromJson(receivedJson, ChatContentModel::class.java)
                            networkMessageInterface.onContent(clientContent)
                        }
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun close() {
        try {
            isRunning = false  // Set the flag to stop the loop
            svrSocket?.close()  // Close the ServerSocket
            Log.d("WFDManager", "Server has been closed.")
        } catch (e: Exception) {
            Log.e("WFDManager", "Error occurred while closing the server: ${e.message}")
            e.printStackTrace()
        }
    }
}