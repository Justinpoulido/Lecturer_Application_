package com.example.lecturerapplication.peerlist

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lecturerapplication.R

class PeerListAdapter(private val onPeerSelected: (WifiP2pDevice) -> Unit) : RecyclerView.Adapter<PeerListAdapter.ViewHolder>() {
    private val peersList:MutableList<WifiP2pDevice> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val peer: TextView = itemView.findViewById(R.id.Peer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peer_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peer = peersList[position]

        holder.peer.text = peer.deviceName
        holder.itemView.setOnClickListener {
            onPeerSelected(peer)
        }
    }

    override fun getItemCount(): Int {
        return peersList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newPeersList:Collection<WifiP2pDevice>){
        peersList.clear()
        peersList.addAll(newPeersList)
        notifyDataSetChanged()
    }
}