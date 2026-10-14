# Carril R · rutas del frontend sin contrato en erp y publicidad: ABIERTO con dueño

Fecha: 2026-10-11 · Rama justin/feature/cierre-endurecimiento-2026-10-08 · Continúa `evidencia/carril-K/rutas-sin-contrato-con-dueno.md`.

## Qué se verificó (hechos)

- `servicios/erp/src/main/resources/openapi/erp.yaml` tiene UN solo `get` (línea 366, `validarPlantillaDeAsiento`); `servicios/publicidad/src/main/resources/openapi/publicidad.yaml` tiene UN solo `get` (línea 312, consumo del período). Ningún `ErpController` ni `PublicidadController` implementa un listado, y los repositorios de ambos servicios no tienen consultas de listado.
- `python scripts/verificar_rutas_frontend.py` (corrido el 2026-10-11): `rutas distintas que usa el frontend: 50 · con contrato: 31 · sin contrato: 19`, `TODO OK` (las 19 son las conocidas; la lista no se pudre porque el gate falla si una conocida llega a tener contrato).
- Los endpoints de listado NO existen. Por eso no se inventó semántica de negocio: ni columnas, ni filtros, ni permisos, ni paginación. Declarar un `get` en el contrato sin construirlo rompe la compilación (el generador crea métodos abstractos) y publicaría una ruta falsa; construirlo exige las decisiones de la columna "Decisión que falta". Regla 00 §1.7: la ambigüedad se registra, no se resuelve por conveniencia.

## Inventario y decisiones (estado: ABIERTO, nadie las tomó)

Toda decisión de listado necesita además lo que dicta la regla 96.6: paginación con límite, orden determinista con desempate, campo de orden por lista blanca. Eso es técnico y se resuelve al construir; lo de abajo es lo que NO se puede resolver sin una persona.

| Ruta (la usa el backoffice) | Dueño de la decisión | Decisión que falta | Dueño del código |
|---|---|---|---|
| GET /erp/activos | Contabilidad (jefatura contable) | columnas del inventario (¿valor en libros, depreciación acumulada, categoría?), filtros (estado, categoría), si el permiso de lectura es el mismo `CONTABILIDAD_ERP_ACTIVOS_FIJOS` o uno nuevo de solo lectura | servicio erp |
| GET /erp/cuentas-por-cobrar | Contabilidad / Tesorería | columnas y filtros (tercero, vencimiento, estado); permiso `CONTABILIDAD_ERP_COBRAR` u otro de lectura | servicio erp |
| GET /erp/facturas-de-proveedor | Contabilidad / Tesorería | columnas; el estado y lo pagado NO están en la fila (`factura_proveedor` es append-only): se derivan de `pago_a_proveedor`, así que hay que decidir si el listado los calcula y cómo; filtros; permiso | servicio erp |
| GET /erp/ordenes-de-compra | Compras | columnas y filtros (estado, tercero); permiso `CONTABILIDAD_ERP_COMPRAS` u otro de lectura | servicio erp |
| GET /erp/partidas-presupuestarias | Contabilidad / Presupuesto | qué ejercicio/centro de costo se lista y con qué ejecución (ya existe el cálculo en `CU101Presupuestar.ejecucion`); permiso `CONTABILIDAD_ERP_PRESUPUESTO` u otro | servicio erp |
| GET /erp/ejercicios/{x}/periodos | Contabilidad | permiso (`CONTABILIDAD_ERP_CERRAR` hoy es de escritura); si un usuario sin permiso de cierre ve los períodos | servicio erp |
| GET /erp/estados-financieros/{x}/documento | Contabilidad (formato del documento) | formato (PDF o XLSX), quién lo sella y con qué plantilla; hoy `estado_financiero_generado` guarda `hash_contenido`, no un archivo | servicio erp |
| GET /publicidad/socios-comerciales/{x} | Cumplimiento / privacidad (regla 90.2.8) | qué campos del socio ve el backoffice (datos personales: solo con consentimiento y mínimo necesario) y qué lectura deja rastro en la bitácora de lectura (90.2.7) | servicio publicidad |
| GET /publicidad/anunciantes | Publicidad (comercial) | columnas, filtros (tipo, estado), permiso | servicio publicidad |
| GET /publicidad/campanas | Publicidad (comercial) | columnas, filtros (estado, anunciante), permiso | servicio publicidad |
| GET /publicidad/piezas-creativas | Moderación | criterio de orden y estados de la cola (¿solo `PENDIENTE`? ¿por antigüedad?); R-PUB-05: quien sube no modera, ¿la cola oculta las propias piezas? | servicio publicidad |

Las otras ocho rutas sin contrato del inventario (reclamos, grupos, identidad, sesión, extracción) NO son de erp ni de publicidad y no se tocaron: siguen como las dejó K.

## Qué hace falta para cerrarlas

1. La persona dueña responde la columna "Decisión que falta" (por escrito, en el CU correspondiente de `docs/CasosDeUso/`).
2. Se agrega la operación al `*.yaml` del servicio ANTES de implementarla (ADR-020), se implementa en el controlador y el repositorio, y se sale de `CONOCIDAS_SIN_CONTRATO` en `scripts/verificar_rutas_frontend.py` (el gate lo obliga).
3. Se agrega la matriz negativa (sin token, rol insuficiente, otra organización) y, para publicidad/socios, la prueba de que no se devuelven datos de más.

## No cubierto

Nada de lo anterior está construido: el backoffice sigue pidiendo estas rutas contra el simulado (`packages/simulado`), y contra el backend real devolverán 404 hasta que el dueño decida y se construyan.
