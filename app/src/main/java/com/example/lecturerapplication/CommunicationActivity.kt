package com.example.lecturerapplication

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.net.InetAddress
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lecturerapplication.peerlist.PeerListAdapter
import com.example.lecturerapplication.peerlist.PeerListAdapterInterface
import com.example.lecturerapplication.wifidirect.WifiDirectInterface
import com.example.lecturerapplication.wifidirect.WifiDirectManager
import com.example.lecturerapplication.chatlist.ChatListAdapter
import com.example.lecturerapplication.models.ChatContentModel
import com.example.lecturerapplication.network.NetworkMessageInterface
import com.example.lecturerapplication.network.Server
import kotlin.concurrent.thread

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, NetworkMessageInterface {
    private var wfdManager: WifiDirectManager? = null
    private var peerListAdapter:PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null

    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasAttendees = false
    private var hasDevices = false

    private lateinit var btnStartClass : Button
    private lateinit var btnEndCurrentClass : Button
    private lateinit var attendeesView: RecyclerView
    private lateinit var chatView : RecyclerView

    private var server: Server? = null
    private var deviceIp : String = ""
    private var studentID : String = ""
    private var selectedPeerIp: String = ""
    val studentList: List<String> = listOf("816117992", "816001001", "816001002", "816001003", "81600104", "816001005", "816001006")

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)

        btnStartClass = findViewById(R.id.btnStartClass)
        btnStartClass.setOnClickListener { view ->
            startClass(view)
        }

        btnEndCurrentClass = findViewById(R.id.btnEndCurrentClass)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val manager: WifiP2pManager =
            getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)


        attendeesView = findViewById(R.id.rvAttendees)
        peerListAdapter = PeerListAdapter { selectedPeer ->
            showPeerChat(selectedPeer)
        }
        attendeesView.adapter = peerListAdapter
        attendeesView.layoutManager = LinearLayoutManager(this)

        chatView = findViewById(R.id.rvChat)
        chatListAdapter = ChatListAdapter()
        chatView.adapter = chatListAdapter
        chatView.layoutManager = LinearLayoutManager(this)
    }

    fun startClass(view: View){
        wfdManager?.createGroup()
    }

    fun endClass(view: View) {
        wfdManager?.disconnect()
        peerListAdapter = PeerListAdapter { selectedPeer ->
            showPeerChat(selectedPeer)
        }
        attendeesView.adapter = peerListAdapter
        chatListAdapter = ChatListAdapter()
        chatView.adapter = chatListAdapter
        findViewById<TextView>(R.id.tvStudentNumber).text = ""
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }
        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onConnectedListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Updated listing of connected WiFi Direct devices", Toast.LENGTH_SHORT)
        toast.show()
        hasAttendees = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        updateUI()
    }


    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?, wifiP2pInfo: WifiP2pInfo) {
        val text : String

        if (groupInfo == null){
            text = "Group is not formed"
        } else {
            text = "Group has been formed"
            val className = groupInfo.networkName
            findViewById<TextView>(R.id.tvClassName).text = className
        }

        var toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        wfdHasConnection = groupInfo != null
        updateUI()

        if (groupInfo == null){
            server?.close()
            server = null
        } else if (groupInfo.isGroupOwner && server == null){
            thread {
                server = Server(this)
            }
        }
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
    }

    private fun updateUI() {
        val wfdAdapterErrorView:ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val noAttendeesMessage: TextView = findViewById(R.id.tvNoAttendeesMessage)
        noAttendeesMessage.visibility = if (wfdAdapterEnabled && wfdHasConnection && !hasAttendees) View.VISIBLE else View.GONE

        val rvAttendees: RecyclerView= findViewById(R.id.rvAttendees)
        rvAttendees.visibility = if (wfdAdapterEnabled && wfdHasConnection && hasAttendees) View.VISIBLE else View.GONE

        val wfdConnectedView:ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE


    }

    fun sendMessage(view: View) {
        val etMessage:EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString()
        val content = ChatContentModel(etString, "192.168.49.1")
        etMessage.text.clear()
        queueMessage(content)
        chatListAdapter?.addItemToEnd(content)
    }

    private fun showPeerChat(peer: WifiP2pDevice) {
        val studentNumber = peer.deviceName
        val studentNumberView = findViewById<TextView>(R.id.tvStudentNumber)
        studentNumberView.text = studentNumber
    }

    override fun onContent(content: ChatContentModel) {
        runOnUiThread{
                chatListAdapter?.addItemToEnd(content)
        }
    }

    fun queueMessage(message: ChatContentModel) {
        server?.messageQueue?.add(message)
    }


    fun goToSettings(view: View) {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }
}