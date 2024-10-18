package com.example.lecturerapplication.network

import com.example.lecturerapplication.models.ChatContentModel

interface NetworkMessageInterface {
    fun onContent(content: ChatContentModel)
}