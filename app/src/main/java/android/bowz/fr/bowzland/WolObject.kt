package android.bowz.fr.bowzland

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {

    val PORT = 9

    @JvmStatic
    fun main(args: Array<String>) {

        if (args.size != 2) {
            println("Usage: java WakeOnLan <broadcast-ip> <mac-address>")
            println("Example: java WakeOnLan 192.168.0.255 00:0D:61:08:22:4A")
            println("Example: java WakeOnLan 192.168.0.255 00-0D-61-08-22-4A")
            System.exit(1)
        }

        val ipStr = args[0]
        val macStr = args[1]

        try {
            val macBytes = getMacBytes(macStr)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            var i = 6
            while (i < bytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                i += macBytes.size
            }

            val address = InetAddress.getByName(ipStr)
            val packet = DatagramPacket(bytes, bytes.size, address, PORT)
            val socket = DatagramSocket()
            socket.send(packet)
            socket.close()

            println("Wake-on-LAN packet sent.")
        } catch (e: Exception) {
            println("Failed to send Wake-on-LAN packet: + e")
            System.exit(1)
        }

    }

    @Throws(IllegalArgumentException::class)
    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split("(\\:|\\-)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (hex.size != 6) {
            throw IllegalArgumentException("Invalid MAC address.")
        }
        try {
            for (i in 0..5) {
                bytes[i] = Integer.parseInt(hex[i], 16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex digit in MAC address.")
        }

        return bytes
    }


}