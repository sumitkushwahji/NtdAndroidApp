package com.example.ntdapp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

class NTPClient {

    companion object {
        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_TIMESTAMP_OFFSET = 2208988800L
        private const val NTP_MODE_CLIENT = 3

        fun requestTime(server: String): Long {
            val buffer = ByteArray(NTP_PACKET_SIZE)
            buffer[0] = (NTP_MODE_CLIENT shl 3 or (4 shl 0)).toByte()

            val address = InetAddress.getByName(server)
            val socket = DatagramSocket()
            socket.soTimeout = 5000

            val packet = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(packet)

            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            val transmitTime = ByteBuffer.wrap(buffer, 40, 8)
                .order(ByteOrder.BIG_ENDIAN)
                .long

            return (transmitTime - NTP_TIMESTAMP_OFFSET) * 1000
        }
    }
}
