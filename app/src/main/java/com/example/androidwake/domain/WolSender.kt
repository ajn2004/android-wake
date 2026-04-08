package com.example.androidwake.domain

interface WolSender {
    suspend fun sendMagicPacket(normalizedMac: String)
}
