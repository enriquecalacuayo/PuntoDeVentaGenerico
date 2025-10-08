package com.example.puntodeventagenerico.data.local

data class CarritoItem(
    val producto: ProductoEntity,
    var cantidad: Int = 1,
    var personalizaciones: List<PersonalizacionEntity> = emptyList()
) {
    /**
     * Retorna el precio total de este ítem considerando cantidad y extras.
     */
    fun precioTotal(): Double {
        val precioBase = producto.precioPublico
        val extras = personalizaciones.sumOf { it.costoExtra }
        return (precioBase + extras) * cantidad
    }

    /**
     * Retorna una descripción legible para mostrar en el carrito.
     */
    fun descripcionCompleta(): String {
        val extras = if (personalizaciones.isNotEmpty()) {
            personalizaciones.joinToString(", ") { "${it.descripcion} (+$${"%.2f".format(it.costoExtra)})" }
        } else ""

        return "${producto.nombre} x$cantidad\n$extras"
    }
}
