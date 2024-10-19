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
        const val PORT: Int = 10009
    }
    var serverIp: String = getLocalIpAddress()
    private var svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName(serverIp))
    private var clientMap: HashMap<String, Socket> = HashMap()
    private val serverThread: Thread

    /*init {
        Log.e("WFDManager", "Server initialized on IP: $serverIp, Port: $PORT")
        thread {
            while (true) {
                try {
                    Log.e("WFDManager", "Waiting for client connection...")
                    val clientConnectionSocket = svrSocket.accept()
                    Log.e("WFDManager", "The server has accepted a connection from: ${clientConnectionSocket.inetAddress.hostAddress}")

                    // Handle the new client connection
                    handleSocket(clientConnectionSocket)

                } catch (e: Exception) {
                    Log.e("WFDManager", "An error has occurred in the server while accepting connections: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }*/

    init {
        // Initialize and start the server thread
        serverThread = thread {
            while (!svrSocket.isClosed) {
                try {
                    Log.e("WFDManager", "Waiting for client connection...")
                    // This line will block until a client connects or the socket is closed
                    val clientConnectionSocket = svrSocket.accept()
                    Log.e("WFDManager", "The server has accepted a connection from: ${clientConnectionSocket.inetAddress.hostAddress}")
                    // Handle the new client connection
                    handleSocket(clientConnectionSocket)

                } catch (e: SocketException) {
                    if (svrSocket.isClosed) {
                        Log.e("WFDManager", "Server socket closed.")
                    } else {
                        Log.e("WFDManager", "An error has occurred in the server while accepting connections: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("WFDManager", "An unexpected error has occurred: ${e.message}")
                }
            }
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

                            /*val reversedContent = ChatContentModel(clientContent.message.reversed(), serverIp)
                            val reversedContentStr = Gson().toJson(reversedContent)
                            clientWriter.write("$reversedContentStr\n")
                            clientWriter.flush()
                            // To show the correct alignment of the items (on the server), I'd swap the IP that it came from the client
                            // This is some OP hax that gets the job done but is not the best way of getting it done.
                            val tmpIp = clientContent.senderIp
                            clientContent.senderIp = reversedContent.senderIp
                            reversedContent.senderIp = tmpIp
                            networkMessageInterface.onContent(reversedContent)*/

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

    fun close(){
        svrSocket.close()
        clientMap.clear()
    }

    fun getLocalIpAddress(): String {
        try {
            // Get all network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // Filter out loopback and inactive interfaces
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        // Check for IPv4 address
                        if (address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return "0.0.0.0" // Default value if no IP found
    }

}