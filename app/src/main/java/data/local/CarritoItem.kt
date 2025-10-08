package com.example.puntodeventagenerico.data.local

data class CarritoItem(
    val producto: ProductoEntity,
    var cantidad: Int,
    var personalizaciones: List<PersonalizacionEntity> = emptyList()
) {
    fun descripcionCompleta(): String {
        // Si el producto ya incluye las personalizaciones en el nombre (por ejemplo, "Latte (Sin chispas)"),
        // o no tiene personalizaciones, no mostramos nada extra debajo.
        return if (personalizaciones.isEmpty()) {
            "${producto.nombre} x$cantidad"
        } else {
            // Si tiene personalizaciones, las dejamos solo dentro del nombre (ya están entre paréntesis)
            "${producto.nombre} x$cantidad"
        }
    }

    fun precioTotal(): Double {
        val costoExtras = personalizaciones.sumOf { it.costoExtra }
        return (producto.precioPublico + costoExtras) * cantidad
    }
}
