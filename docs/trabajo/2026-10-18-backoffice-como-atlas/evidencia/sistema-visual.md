# Sistema visual del backoffice · equivalencias Atlas → AportaYa

Fuente Atlas: `AtlasAdminPortal/tailwind.config.ts`, `src/app/globals.css`, `src/shared/components/**`.
Fuente AportaYa: `docs/Views/Sistema-Diseno/estilos.css` (bóveda) → `packages/tokens/tokens.json` → `generado/tokens.css`.
Regla: **ningún literal de color, espacio, radio o tipografía en los componentes** (`verificar_frontend.py`); todo va por rol.

## 1. Color
| Uso | Atlas | AportaYa (rol) | Nota |
|---|---|---|---|
| Lienzo de la página | `atlas-bg #f5f5f7` | `--bg` | |
| Superficie (tarjetas, tabla) | `atlas-card #fff` | `--surface` | |
| Superficie suave (cabecera de tabla, hover, loseta de ícono) | `atlas-soft #f0f0f3`, `#F7F7F8`, `#FAFAFB` | `--surface-2` | |
| Línea | `atlas-border #e4e4e7` | `--border` | |
| Tinta / texto | `atlas-text #1d1d1f` · `--text #38383d` | `--text` · `--text-2` | |
| Texto tenue (eyebrow, rótulos, descripciones) | `atlas-muted #5a5a63` | `--text-3` | |
| Acento (título de página, ícono de cabecera, foco) | `atlas-accent #006a61` | `--brand-texto` (texto) · `--brand` (superficie) | un solo acento |
| Acento suave (loseta de ícono, fondo de pestaña activa) | `accentSoft #d9ebe8` · `accentWash #f0f8f7` | `--brand-bg` | |
| Botón primario | `slate-900` (tinta) | `--brand` sobre `--accent-ink` | el primario nuestro ya es verde plano (A2 del 10-15) |
| Éxito | `success #0f7b52` / `emerald-50` | `--ok` · `--ok-bg` · `--ok-texto` | |
| Aviso | `warning #a34a05` / `amber-50` | `--warn` · `--warn-bg` · `--aviso-texto` | |
| Crítico | `critical #b42318` / `red-50` | `--err` · `--err-bg` · `--err-texto` | |
| Información | `info #1f6fb2` | `--info` · `--info-bg` · `--info-texto` | |
| **Datos personales** | `pii #6b3fa0` | **`--pii` · `--pii-bg` · `--pii-texto`** (nuevos, H0) | claro 8,05:1 · oscuro 7,44:1 |
| Apagado (badge `muted`) | `slate-100 / slate-500` | `--surface-2` / `--text-3` | tono `apagado` del chip |
| Barra lateral | `atlas-mesh` (#12181a → #10322f → #00544d) | degradé `--g800 → --g900` + radial `--g400` | ya está |

## 2. Tipografía
| Uso | Atlas | AportaYa |
|---|---|---|
| Título de página | 30/38 bold, en el acento | `--t-titulo-1` (26/1.15) con `--brand-texto`; 20px en móvil |
| Eyebrow | 11px bold uppercase tracking .16em | **`--t-eyebrow`** (nuevo) + `--text-3` |
| Descripción | 14/24 muted | `--t-cuerpo-chico` + `--text-3` |
| Cabecera de sección (h2) | 14 semibold + ícono acento | `--t-titulo-3` |
| Cabecera de tabla | 11px uppercase tracking .08em muted | **`--t-cabecera-tabla`** (nuevo) + `--text-3` |
| Rótulo de KPI | 12 semibold uppercase | `--t-etiqueta` + `--text-3` |
| Cifra de KPI | 2xl bold tabular | `--t-cifra` + `font-variant-numeric: tabular-nums` |
| Celda | 14 | `--t-cuerpo-chico` |
| Código / mono | JetBrains Mono 12 | `--mono` 13px |

## 3. Espacio, radio, sombra, movimiento
| Uso | Atlas | AportaYa |
|---|---|---|
| Relleno de página | `px-3 pt-5 sm:px-4 lg:px-6` | `--s3` móvil · `--s4` tablet · `--s5` escritorio |
| Separación entre bloques | `mb-4..6` | `--s4` / `--s5` |
| Tarjeta | `rounded-2xl border shadow-card` | `--r-xl` · `--border` · `--sombra-2` |
| Tarjeta de tabla / filtros | `rounded-xl shadow-subtle` | `--r-lg` · `--sombra-1` |
| Insignia | `rounded-md px-1.5 py-0.5 text-xs` | `--r-sm` · `--s1`/`--s2` · 12px |
| Botón | `h-9 rounded-lg px-3 text-sm` | `--area-tactil` · `--r-md` · `--t-boton` |
| Campo | `h-11 rounded-xl ring-4 accent/10` | `--area-tactil` · `--r-md` · foco `--borde-foco` `--g300` |
| Hover de tarjeta interactiva | `-translate-y-0.5 shadow-card-hover` | `translateY(-2px)` + `--sombra-3` |
| Entrada de vista | `animate-fade-in` 280ms `cubic-bezier(.16,1,.3,1)` | `--mov-entrada` (existe en `pasanaku-movimiento`) |
| Esqueleto | shimmer por `transform` | igual; `prefers-reduced-motion` lo apaga |
| Diálogo / cajón | `scale-in` / `drawer-in` | igual |

## 4. Anatomía de una **pantalla de listado**
```
┌ Explicador de la vista (plegado: una línea con bombilla) ────────────────────┐
├ CABECERA DE PÁGINA ─────────────────────────────────────────────────────────┤
│ [loseta ícono]  EYEBROW                                     [acciones →→]    │
│                 Título en acento (titulo-1)                                  │
│                 Descripción en text-3, máx 3xl                                │
├ Nota de la vista (plegada) ──────────────────────────────────────────────────┤
├ CIFRAS (grid auto-fit 10rem) ────────────────────────────────────────────────┤
│ [ícono] RÓTULO      [ícono] RÓTULO      [ícono] RÓTULO                        │
│         1 234               12                  3  ← tono por significado     │
├ BARRA DE FILTROS (tarjeta) ──────────────────────────────────────────────────┤
│ 🔍 Buscar…(debounce)   [Estado ▾ Todos]  [Módulo ▾ Todos]      Limpiar       │
├ TABLA (tarjeta) ─────────────────────────────────────────────────────────────┤
│ REGISTROS                                              [copiar] [ 1 234 ]    │
│ ─────────────────────────────────────────────────────────────────────────── │
│ COLUMNA  COLUMNA  COLUMNA … (fija al desplazar)                    ACCIONES │
│ fila hover · insignias con punto · enlaces mono · acciones fijas a la derecha│
├ PAGINACIÓN ──────────────────────────────────────────────────────────────────┤
│ Página 1 de 17 · 338 registros   Filas por página [20▾]  Ir a [ 1 ]  ‹ Anterior  Siguiente ›│
└──────────────────────────────────────────────────────────────────────────────┘
Estados: cargando → esqueleto de n filas · vacío → tarjeta punteada con ícono, título, texto y acción · error → tarjeta roja con ícono, título, código de seguimiento y Reintentar · sin permiso → «Acceso restringido».
```

## 5. Anatomía de una **ficha**
```
┌ CABECERA DE PÁGINA ───────────────────────────────────────────────────────────┐
│ [ícono] EYEBROW «Catálogo de datos»                [Editar ficha] [PII][Rev][Estado] │
│         esquema.tabla                                                          │
│         descripción = qué es                                                   │
├ PESTAÑAS segmentadas (estado en ?vista=) ───────────────────────────────────┤
│ [ Vista general ] Columnas  Relaciones  Gobierno                               │
├ SECCIONES CLAVE–VALOR (grid 1–2 col) ────────────────────────────────────────┤
│ ┌ Identificación técnica ──────────┐ ┌ Contexto de negocio ─────────────────┐ │
│ │ descripción gris                 │ │                                       │ │
│ │ RÓTULO   valor (mono copiable)   │ │ RÓTULO   valor                        │ │
│ │ RÓTULO   valor   (cebra)         │ │ …                                     │ │
│ └──────────────────────────────────┘ └───────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
Edición: página aparte («Configurar …») con tarjetas de formulario, tooltip por campo, error de validación arriba, diálogo de confirmación ámbar («Se actualizará … (n campos)»). Acciones por fila: cajón lateral derecho con cabecera fija.
```

## 6. Shell
```
┌ BARRA LATERAL oscura 268px ─┬ CABECERA fija «glass» ──────────────────────────────┐
│ [logo] APORTAYA             │ ☰  Inicio › Sección › Vista      🔍 Buscar…  🔔  👤 │
│        Backoffice           ├──────────────────────────────────────────────────────┤
│ ▾ GRUPO (ícono, uppercase)  │ main (fade-in)                                       │
│    ● Ítem activo            │   Explicador de la vista                             │
│      Ítem                   │   …pantalla…                                         │
│ ▸ GRUPO plegado             │                                                      │
│ …                           │                                                      │
│ [Salir]                     │                                                      │
└─────────────────────────────┴──────────────────────────────────────────────────────┘
Móvil: cabecera con hamburguesa; la barra es un cajón con foco atrapado; el carril de acciones de la cabecera de página se desplaza en horizontal.
```
