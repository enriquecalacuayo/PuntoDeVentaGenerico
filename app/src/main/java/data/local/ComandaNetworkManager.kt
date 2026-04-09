package com.example.puntodeventagenerico.data.local

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

object ComandaNetworkManager {

    const val PORT = 9090
    private const val BUFFER_SIZE = 8192
    private const val SOCKET_TIMEOUT_MS = 1500
    private const val TAG = "ComandaNetwork"

    // ── Identificador único del dispositivo ──────────────────────────────────
    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    // ── MulticastLock ─────────────────────────────────────────────────────────
    fun adquirirMulticastLock(context: Context): WifiManager.MulticastLock {
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifi.createMulticastLock("comandas_lock").apply {
            setReferenceCounted(false)
            acquire()
            Log.d(TAG, "MulticastLock adquirido")
        }
    }

    // ── ENVIAR comanda por broadcast UDP ────────────────────────────────────
    fun enviar(context: Context, descripcion: String) {
        val payload = JSONObject().apply {
            put("descripcion", descripcion)
            put("fechaHora", System.currentTimeMillis())
            put("deviceId", getDeviceId(context))
        }.toString().toByteArray(Charsets.UTF_8)

        val destinos = getBroadcastAddresses()
        // Usar solo la primera dirección de subred encontrada.
        // Si se envía a varias (subred + 255.255.255.255), el receptor
        // recibiría el paquete una vez por cada destino → comandas duplicadas.
        val destino = destinos.firstOrNull() ?: "255.255.255.255"
        Log.d(TAG, "Enviando comanda a: $destino | payload: ${payload.size} bytes")

        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(
                    DatagramPacket(
                        payload, payload.size,
                        InetAddress.getByName(destino), PORT
                    )
                )
                Log.d(TAG, "Paquete enviado a $destino:$PORT")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando a $destino: ${e.message}")
        }
    }

    // ── ESCUCHAR comandas entrantes ───────────────────────────────────────────
    fun escuchar(
        context: Context,
        shouldContinue: () -> Boolean,
        onRecibida: (descripcion: String, fechaHora: Long) -> Unit
    ) {
        val miId = getDeviceId(context)
        val buffer = ByteArray(BUFFER_SIZE)

        Log.d(TAG, "Iniciando listener UDP en puerto $PORT")

        try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = SOCKET_TIMEOUT_MS
                bind(InetSocketAddress(PORT))
            }
            Log.d(TAG, "Socket UDP vinculado al puerto $PORT")

            socket.use {
                val packet = DatagramPacket(buffer, buffer.size)
                while (shouldContinue()) {
                    try {
                        socket.receive(packet)
                        val texto = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        Log.d(TAG, "Paquete recibido de ${packet.address}: $texto")

                        val json = JSONObject(texto)
                        if (json.optString("deviceId") == miId) {
                            Log.d(TAG, "Paquete propio ignorado")
                            continue
                        }

                        val descripcion = json.getString("descripcion")
                        val fechaHora = json.getLong("fechaHora")
                        Log.d(TAG, "Comanda recibida: $descripcion")
                        onRecibida(descripcion, fechaHora)

                    } catch (_: java.net.SocketTimeoutException) {
                        // Normal — loop continúa
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recibiendo paquete: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "Listener detenido")

        } catch (e: Exception) {
            Log.e(TAG, "Error fatal en listener UDP: ${e::class.simpleName}: ${e.message}")
        }
    }

    // ── Obtener todas las direcciones broadcast disponibles ───────────────────
    // Usa NetworkInterface para leer las IPs reales de la red — más confiable
    // que WifiManager.dhcpInfo que da resultados incorrectos en muchos dispositivos.
    private fun getBroadcastAddresses(): List<String> {
        val result = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
            for (iface in interfaces.asSequence()) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.interfaceAddresses) {
                    val broadcast = addr.broadcast ?: continue
                    val broadcastStr = broadcast.hostAddress ?: continue
                    result.add(broadcastStr)
                    Log.d(TAG, "Interfaz ${iface.name} → broadcast: $broadcastStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo interfaces de red: ${e.message}")
        }
        return result
    }
}
