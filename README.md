# PuntoDeVentaGenerico

Android POS (Point of Sale) application built for tablet use, focused on cafeteria and food-service operations. Offline-first, no internet required.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts + AppCompatActivity |
| Database | Room (SQLite) |
| Async | Coroutines + lifecycleScope |
| Storage | SharedPreferences (profiles), ExternalFilesDir (auto-backup) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |

---

## Architecture

```
app/src/main/java/
├── com/example/puntodeventagenerico/
│   ├── MenuPrincipalActivity.kt       # Launcher — profile guard + backup triggers
│   └── ui/
│       ├── agregarproducto/           # Add product flow
│       ├── comandas/                  # Kitchen order display
│       ├── historial/                 # Sales history + daily stats + cash drawer
│       ├── perfiles/                  # Profile selection & creation
│       └── venta/                     # Active sale + cart
│
└── data/local/
    ├── AppDatabase.kt                 # Room database definition (v8)
    ├── BackupManager.kt               # JSON serialization, auto-backup, export/import
    ├── ProfileManager.kt              # Profile CRUD + active profile via SharedPreferences
    ├── Perfil.kt                      # Profile data class
    ├── *Entity.kt                     # Room entities
    └── *Dao.kt                        # Data access objects
```

---

## Core Features

### Multi-Profile Support
Each profile maintains a completely isolated database. Switching profiles switches the entire data context — products, pricing, personalizations, sales history, and cash records are all profile-scoped.

- Profiles stored as JSON in SharedPreferences
- Each profile maps to its own Room database: `pv_<profileId>.db`
- Profile selection screen shown on first launch (no profile = blocked entry)
- Switch profiles at any time from the main menu

### Sales Flow
1. Select subcategory from spinner
2. Browse products filtered by subcategory
3. Add items to cart (in-memory `CarritoItem` list)
4. Apply personalizations per item (optional)
5. Add item comment (optional)
6. Select payment method: cash or card
7. Send comanda to kitchen + persist `VentaEntity`

### Kitchen Orders (Comandas)
- Auto-generated on sale confirmation
- Displayed in a `RecyclerView` with real-time updates
- Personalizations rendered inline in order description

### Product Management
- Full CRUD: name, public price, unit cost, subcategory
- Per-product personalizations with optional extra cost
- Personalization autocomplete from usage history

### Daily Statistics & Cash Drawer
- Total sales, net profit, card vs. cash breakdown
- Opening/closing cash register with variance calculation
- Daily expense logging
- Formula: `expected = cajaInicio + ventasEfectivo - gastos`

### Backup & Restore System
- **Auto-backup**: triggered silently on every return to main menu. Saves to `getExternalFilesDir("backups")/backup_<profile>.json`
- **Manual export**: SAF `CreateDocument` launcher — user selects destination (Downloads, Drive, USB, etc.)
- **Manual import**: SAF `OpenDocument` launcher — user selects a `.json` backup file; full DB restore with confirmation dialog
- Backup scope: all tables in the active profile's database
- Backup format: versioned JSON (`"version": 1`)

---

## Database

**File:** `AppDatabase.kt` — version 8, `fallbackToDestructiveMigration()`

| Table | Entity | Description |
|---|---|---|
| `productos` | `ProductoEntity` | Product catalog |
| `subcategorias` | `SubcategoriaEntity` | Product categories |
| `personalizaciones` | `PersonalizacionEntity` | Per-product options with extra cost |
| `historial_personalizacion` | `HistorialPersonalizacionEntity` | Autocomplete history |
| `ventas` | `VentaEntity` | Completed sales |
| `comandas` | `ComandaEntity` | Kitchen orders |
| `caja_dia` | `CajaDiaEntity` | Daily cash drawer records |
| `gastos_dia` | `GastoEntity` | Daily expense entries |

Each profile has its own instance of this schema under a separate database file.

---

## Business Rules

**Pricing**: Product price already includes any personalizations selected at the time of adding to cart. Do not recalculate extra costs at checkout.

**Cash drawer**: Only cash sales affect the physical register. Card payments (`pagoConTarjeta = true`) are tracked separately and excluded from cash reconciliation.

**Comandas**: Render personalizations as `(description)` in the order string. Avoid duplicate entries — the personalization list is deduplicated before serialization.

---

## Backup File Location

Auto-backups are written to:
```
/sdcard/Android/data/com.example.puntodeventagenerico/files/backups/
```

Accessible via any Android file manager. Not deleted on system updates; deleted on app uninstall. Use **Export Backup** before uninstalling to preserve data across devices.

---

## Author

Enrique (DCircuits / Expreso 501)
