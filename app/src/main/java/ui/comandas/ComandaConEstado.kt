package com.example.puntodeventagenerico.ui.comandas

import com.example.puntodeventagenerico.data.local.ComandaEntity

/**
 * Wrapper de ComandaEntity con estado de alerta visual.
 *
 * [esNueva] = true  → comanda recibida por red, pendiente de atender (fondo naranja, botón Atender visible)
 * [esNueva] = false → comanda normal o ya atendida (fondo blanco, sin botón Atender)
 */
data class ComandaConEstado(
    val comanda: ComandaEntity,
    var esNueva: Boolean = false
)
