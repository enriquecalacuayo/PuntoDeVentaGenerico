package com.example.puntodeventagenerico.data.local

data class CarritoItem(
    val producto: ProductoEntity,
    var cantidad: Int = 1,
    val personalizaciones: List<PersonalizacionEntity> = emptyList(),
    var comentario: String = "",
    var pagoConTarjeta: Boolean = false // 💳 Nuevo campo
) {
    fun precioTotal(): Double {
        return producto.precioPublico * cantidad
    }

    fun descripcionCompleta(): String {
        val baseNombre = if (producto.nombre.contains("(")) {
            producto.nombre
        } else {
            val extras = personalizaciones.joinToString(", ") { it.descripcion }
            if (extras.isNotEmpty()) "${producto.nombre} ($extras)" else producto.nombre
        }

        // 👇 Muestra comentario si existe
        val comentarioTexto = if (comentario.isNotBlank()) "\n🗒️ $comentario" else ""

        // 👇 Muestra tipo de pago (tarjeta o efectivo)
        val pagoTexto = if (pagoConTarjeta) "\n💳 Pago con tarjeta" else ""

        return baseNombre + comentarioTexto + pagoTexto
    }
}



