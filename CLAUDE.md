# Claude Code — Project Context: PuntoDeVentaGenerico

## Critical Rule
This is a production POS system in active use. Do not break existing functionality.

---

## System Architecture

Three primary flows:

| Flow | Entry Point | Description |
|---|---|---|
| Sale | `IniciarVentaActivity` | Cart, personalizations, comanda generation, payment |
| Kitchen | `VerComandasActivity` | Real-time comanda display for kitchen/barista |
| Admin | `HistorialVentasActivity` → `EstadisticasDiaActivity` | History, stats, cash drawer |

---

## Profile System

**Architecture**: Each profile is a separate Room database (`pv_<profileId>.db`). Profiles are stored as a JSON array in SharedPreferences via `ProfileManager`.

**Key files:**
- `data/local/Perfil.kt` — data class: `id`, `nombre`, `descripcion`
- `data/local/ProfileManager.kt` — singleton: `getPerfiles()`, `getPerfilActivo()`, `setPerfilActivo()`, `crearPerfil()`, `eliminarPerfil()`, `getDatabaseName()`
- `ui/perfiles/SeleccionPerfilActivity.kt` — shown on first launch and on "Cambiar perfil"

**Rules:**
- Every `Room.databaseBuilder(...)` call must use `ProfileManager.getDatabaseName(applicationContext)` — never hardcode `"punto_venta_db"`
- `MenuPrincipalActivity.onResume()` guards entry: if `getPerfilActivo() == null`, redirect to `SeleccionPerfilActivity`
- Deleting a profile removes access but does not delete the `.db` file

---

## Backup System

**Architecture**: JSON serialization of all tables in the active profile's database.

**Key file:** `data/local/BackupManager.kt`

| Method | Trigger | Destination |
|---|---|---|
| `autoBackup(context)` | `MenuPrincipalActivity.onResume()` | `getExternalFilesDir("backups")/backup_<profile>.json` |
| `exportar(context, uri)` | Manual button → SAF `CreateDocument` | User-chosen location |
| `importar(context, uri)` | Manual button → SAF `OpenDocument` | Active profile's database (full replace) |

**Backup JSON structure** (version 1):
```json
{
  "version": 1,
  "perfilId": "string",
  "perfilNombre": "string",
  "fechaBackup": "long",
  "subcategorias": [],
  "productos": [],
  "personalizaciones": [],
  "historialPersonalizaciones": [],
  "ventas": [],
  "comandas": [],
  "cajaDias": [],
  "gastos": []
}
```

**Rules:**
- Import performs a full replace: all tables are cleared before re-inserting
- Auto-backup is silent (no UI feedback), errors are swallowed
- Export/import use SAF — no storage permissions required on any Android version

---

## Business Rules (Do Not Break)

### Pricing
- `ProductoEntity.precioPublico` already includes personalization extras at the time the item is added to the cart
- Do NOT re-sum `PersonalizacionEntity.costoExtra` at checkout or in any total calculation

### Cash Drawer
- Only `pagoConTarjeta = false` sales count toward physical cash
- Formula: `esperado = cajaInicio + ventasEfectivo - gastos`
- Card sales are tracked in stats but excluded from cash reconciliation

### Comandas
- Do not render personalizations marked `ocultarEnComanda = true` (field not yet on entity — future migration)
- Avoid duplicate personalization text in comanda description: `(extra)(extra)` is wrong, `(extra)` is correct

---

## Entities

| Entity | Table | Key Fields |
|---|---|---|
| `ProductoEntity` | `productos` | `id`, `nombre`, `categoria`, `precioPublico`, `costoUnitario`, `ocultarEnComandas` |
| `SubcategoriaEntity` | `subcategorias` | `id`, `nombre` |
| `PersonalizacionEntity` | `personalizaciones` | `id`, `productoId`, `descripcion`, `costoExtra` |
| `HistorialPersonalizacionEntity` | `historial_personalizacion` | `id`, `descripcion`, `costoExtra` |
| `VentaEntity` | `ventas` | `id`, `productosVendidos`, `totalVenta`, `ganancia`, `fecha`, `pagoConTarjeta` |
| `ComandaEntity` | `comandas` | `id`, `descripcion`, `fechaHora` |
| `CajaDiaEntity` | `caja_dia` | `fecha` (PK), `cajaInicio`, `gastos`, `cajaFinal` |
| `GastoEntity` | `gastos_dia` | `id`, `fecha`, `nombre`, `monto` |
| `CarritoItem` | *(in-memory only)* | `producto`, `cantidad`, `personalizaciones`, `comentario`, `pagoConTarjeta` |

---

## Resolved Bugs (Do Not Reintroduce)

| Bug | Status |
|---|---|
| Double-summing extras in cart total | Fixed |
| Duplicate personalization text in comandas | Fixed |
| Null pointer on personalization checkbox | Fixed |
| Cash drawer including card sales | Fixed |
| ListView item click stolen by child Button | Fixed — `descendantFocusability="blocksDescendants"` + click on root view |

---

## Database

- Class: `AppDatabase` — Room, version 8
- Migration strategy: `fallbackToDestructiveMigration()`
- **Do not rename tables without a proper Room migration**
- **Do not change column types without incrementing the DB version**

---

## Expected Change Types

- UI improvements and new screens
- Bug fixes
- New POS features (discounts, multi-item combos, receipt printing, etc.)
- New profile-scoped functionality
- Backup format extensions (add new tables to `BackupManager.serializarDB` + `restaurarDB`)

---

## What NOT To Do

- Do not hardcode `"punto_venta_db"` — always use `ProfileManager.getDatabaseName(context)`
- Do not recalculate extra costs from personalizations after cart insertion
- Do not change table/column names without a Room migration
- Do not duplicate comanda text rendering logic
- Do not add network calls — this app is intentionally offline-first
