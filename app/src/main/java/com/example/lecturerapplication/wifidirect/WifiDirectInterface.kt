package com.example.lecturerapplication.wifidirect

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo

interface WifiDirectInterface {
    fun onWiFiDirectStateChanged(isEnabled:Boolean)
    fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>)
    fun onGroupStatusChanged(groupInfo: WifiP2pGroup?, wifiP2pInfo: WifiP2pInfo)
    fun onDeviceStatusChanged(thisDevice: WifiP2pDevice)
    fun onConnectedListUpdated(deviceList: Collection<WifiP2pDevice>)
}