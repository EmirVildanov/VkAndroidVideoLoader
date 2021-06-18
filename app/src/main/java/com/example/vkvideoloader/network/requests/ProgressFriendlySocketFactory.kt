package com.example.vkvideoloader.network.requests

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class ProgressFriendlySocketFactory(private val sendBufferSize: Int = DEFAULT_BUFFER_SIZE) : SocketFactory() {

    override fun createSocket(): Socket {
        return setSendBufferSize(Socket())
    }

    override fun createSocket(host: String, port: Int): Socket {
        return setSendBufferSize(Socket(host, port))
    }

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return setSendBufferSize(Socket(host, port, localHost, localPort))
    }

    override fun createSocket(host: InetAddress, port: Int): Socket {
        return setSendBufferSize(Socket(host, port))
    }

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return setSendBufferSize(Socket(address, port, localAddress, localPort))
    }

    private fun setSendBufferSize(socket: Socket): Socket {
        socket.sendBufferSize = sendBufferSize
        return socket
    }

    companion object {
        const val DEFAULT_BUFFER_SIZE = 1000000
    }
}