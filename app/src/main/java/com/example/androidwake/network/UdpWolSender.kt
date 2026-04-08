package com.example.androidwake.network

import com.example.androidwake.domain.WolSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpWolSender(
    private val port: Int = 9,
    private val broadcastAddress: String = "255.255.255.255",
) : WolSender {
    override suspend fun sendMagicPacket(normalizedMac: String) {
        withContext(Dispatchers.IO) {
            val macBytes = normalizedMac.split(":").map { it.toInt(16).toByte() }.toByteArray()
            val payload = ByteArray(6 + (16 * macBytes.size))
            for (i in 0 until 6) {
                payload[i] = 0xFF.toByte()
            }
            for (i in 6 until payload.size step macBytes.size) {
                macBytes.copyInto(payload, destinationOffset = i)
            }

            DatagramSocket().use { socket ->
                socket.broadcast = true
                val packet = DatagramPacket(
                    payload,
                    payload.size,
                    InetAddress.getByName(broadcastAddress),
                    port,
                )
                socket.send(packet)
            }
        }
    }
}
