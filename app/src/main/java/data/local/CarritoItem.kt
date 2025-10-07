package com.example.puntodeventagenerico.data.local

data class CarritoItem(
    val producto: ProductoEntity,
    val personalizaciones: List<PersonalizacionEntity> = emptyList(),
    var cantidad: Int = 1
) {
    fun calcularSubtotal(): Double {
        val extra = personalizaciones.sumOf { it.costoExtra }
        return (producto.precioPublico + extra) * cantidad
    }

    fun descripcionCompleta(): String {
        val extras = if (personalizaciones.isNotEmpty())
            personalizaciones.joinToString(", ") { "${it.descripcion} (+$${it.costoExtra})" }
        else "Sin personalización"
        return "${producto.nombre} x$cantidad\n$extras"
    }
}
