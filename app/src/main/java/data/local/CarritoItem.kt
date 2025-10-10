package com.example.puntodeventagenerico.data.local

data class CarritoItem(
    val producto: ProductoEntity,
    var cantidad: Int = 1,
    val personalizaciones: List<PersonalizacionEntity> = emptyList()
) {
    // ✅ Ahora solo devuelve el precio del producto * cantidad
    // No suma los costos extra otra vez porque ya están incluidos
    fun precioTotal(): Double {
        return producto.precioPublico * cantidad
    }

    // ✅ Mantén esta función solo para mostrar el texto completo en el carrito
    fun descripcionCompleta(): String {
        val extras = if (personalizaciones.isNotEmpty()) {
            personalizaciones.joinToString(", ") { it.descripcion }
        } else {
            "Sin personalización"
        }
        return "${producto.nombre} ($extras)"
    }
}

