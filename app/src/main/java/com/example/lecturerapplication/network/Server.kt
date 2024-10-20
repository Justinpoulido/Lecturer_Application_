package com.example.lecturerapplication.network

import android.util.Log
import com.google.gson.Gson
import com.example.lecturerapplication.models.ChatContentModel
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.Exception
import kotlin.concurrent.thread

class Server (
    private val networkMessageInterface :NetworkMessageInterface) {

    companion object {
        const val PORT: Int = 9999
    }

    private var svrSocket: ServerSocket? = null
    private var clientMap: HashMap<String, Socket> = HashMap()
    private var isRunning: Boolean = true
    var messageQueue = ConcurrentLinkedQueue<ChatContentModel>() // to retrieve messages to be sent from teacher to student

    init {
        try {
            svrSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
            Log.d("SERVER", "Server initialized on Port: $PORT")
            thread {
                while (isRunning) {
                    try {
                        Log.d("SERVER", "Waiting for client connection...")
                        val clientConnectionSocket = svrSocket?.accept()
                        Log.d("SERVER", "The server has accepted a connection: ${clientConnectionSocket?.inetAddress?.hostAddress}")
                        clientConnectionSocket?.let { handleSocket(it) }
                    } catch (e: SocketException) {
                        if (!isRunning) {
                            Log.d("SERVER", "Server socket closed.")
                        } else {
                            Log.d("SERVER", "SocketException while accepting: ${e.message}")
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


    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {
            clientMap[it] = socket
            Log.d("SERVER", "A new connection has been detected!")

            // handling incoming messages
            thread {
                val clientReader = socket.inputStream.bufferedReader()
                var receivedJson: String?
                while(socket.isConnected){
                    try{
                        receivedJson = clientReader.readLine()
                        if (receivedJson!= null){
                            Log.d("SERVER", "Received a message from client $it")
                            val clientContent = Gson().fromJson(receivedJson, ChatContentModel::class.java)
                            networkMessageInterface.onContent(clientContent)
                        }
                        if (receivedJson == "I am here"){
                            Log.d("SERVER", "Received initial message from client $it")
                            val clientContent = Gson().fromJson(receivedJson, ChatContentModel::class.java)
                            networkMessageInterface.onContent(clientContent)
                        }

                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }

            // handling outgoing messages
            thread {
                val clientWriter = socket.outputStream.bufferedWriter()
                while (socket.isConnected) {
                    while (messageQueue.isNotEmpty()) {
                        val message = messageQueue.poll()
                        if (message != null) {
                            val messageJson = Gson().toJson(message)
                            clientWriter.write(messageJson)
                            clientWriter.newLine()
                            clientWriter.flush()
                            Log.e("SERVER", "Sent message to client $it: $messageJson")
                        }
                    }
                    Thread.sleep(100)
                }
            }
        }
    }

    fun close() {
        // modified close() because init kept entering into infinite loop when server.close() was called...for some undetected strange reason
        try {
            isRunning = false
            svrSocket?.close()
            Log.d("SERVER", "Server has been closed.")
        } catch (e: Exception) {
            Log.e("SERVER", "Error occurred while closing the server: ${e.message}")
            e.printStackTrace()
        }
    }
}