# Contexto del Proyecto - Punto de Venta

## ⚠️ IMPORTANTE
Este es un sistema POS real en uso. 
NO romper funcionalidades existentes.

---

## 🧠 Arquitectura mental del sistema

El sistema funciona en 3 flujos principales:

1. Venta (cajero)
2. Comandas (cocina/barista)
3. Historial + Caja (administración)

---

## 🛒 Flujo de Venta

Archivo principal:
- IniciarVentaActivity

Proceso:
1. Seleccionar subcategoría
2. Mostrar productos
3. Agregar al carrito
4. Aplicar personalizaciones (si existen)
5. (Opcional) agregar comentario
6. Generar comanda
7. Guardar venta

---

## 💰 Reglas de negocio IMPORTANTES

### 🔴 PRECIOS
- El precio del producto YA incluye extras
- NO recalcular extras en el carrito

---

### 🔴 COMANDAS
- No mostrar personalizaciones con flag:
  `ocultarEnComanda = true`
- Evitar duplicar texto:
  ❌ (extra)(extra)
  ✅ (extra)

---

### 🔴 CAJA
- Solo considerar EFECTIVO
- Ventas con tarjeta NO afectan caja

Fórmula:

esperado = cajaInicio + ventasEfectivo - gastos


---

### 🔴 PERSONALIZACIONES
Tienen:
- descripcion
- costoExtra
- ocultarEnComanda (boolean)

---

## 🧱 ENTIDADES

### ProductoEntity
- id
- nombre
- categoria
- precioPublico
- costoUnitario

---

### PersonalizacionEntity
- id
- productoId
- descripcion
- costoExtra
- ocultarEnComanda (IMPORTANTE)

---

### CarritoItem
- producto
- cantidad
- personalizaciones
- comentario

---

### VentaEntity
- id
- fecha
- productosVendidos
- totalVenta
- ganancia
- pagoConTarjeta (boolean)

---

## 🧠 Problemas ya resueltos (NO romper)

- ❌ doble suma de extras → YA solucionado
- ❌ duplicación de personalizaciones → YA solucionado
- ❌ crash por null checkbox → YA solucionado
- ❌ caja incorrecta → lógica separada efectivo/tarjeta

---

## 🎯 Qué debe hacer Claude

Cuando modifiques código:

1. NO romper lógica existente
2. Mantener consistencia de base de datos
3. Validar cálculos financieros
4. Evitar duplicaciones de texto
5. Seguir estructura actual (Room + Activities)

---

## 🛠️ Tipo de cambios esperados

- UI improvements
- Corrección de bugs
- Nuevas funcionalidades POS
- Optimización de flujo de venta

---

## 🚫 Qué NO hacer

- No cambiar nombres de tablas sin migración
- No duplicar lógica de precios
- No alterar flujo de comandas sin cuidado