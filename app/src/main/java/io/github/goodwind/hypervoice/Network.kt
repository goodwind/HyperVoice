package io.github.goodwind.hypervoice

import java.net.Inet4Address
import java.net.NetworkInterface

object Network {
    /** The TV's address on the local network (a private IPv4 one), or null if it is not on one. */
    fun lanAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    } catch (e: java.net.SocketException) {
        null
    }
}
