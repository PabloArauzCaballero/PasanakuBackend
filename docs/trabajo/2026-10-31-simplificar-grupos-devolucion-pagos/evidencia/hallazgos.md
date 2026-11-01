# Hallazgos — lo que existe hoy, con ruta

Lectura del árbol de trabajo al 2026-10-31 (rama `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18`) y de
`origin/test` 5c06f9e95 para el carril de inicio al 75 %. Es peldaño `DISCOVERED`: nada de esto se ejecutó.

## A · Alta de grupo (el recorrido de hoy)

| # | Paso que hace la persona | Dónde está definido | Dónde está implementado |
|---|---|---|---|
| 1 | Registro + verificación básica (documento, rostro) → **aprobación por una persona en backoffice** | `docs/Flujo funcional · recorrido del usuario.md` §0 y RF-01; `PENDIENTES.md` («Aprobación de cuenta: sigue siendo manual») | `servicios/identidad/.../aplicacion/CU02RevisarExpediente.java` |
| 2 | Aceptar contrato de adhesión y tarifario | RF-01 → CU-05 | — |
| 3 | «Organizar un grupo» → aceptar **contrato Aprendiz** (habilitación sin bandeja) | `docs/trabajo/2026-10-17-organizador-basico/REPORTE.md` | `servicios/organizador/.../aplicacion/CU90HabilitacionBasica.java` (límites `aportaya.organizador.limite-inicial-*`); app `apps/movil/lib/pantallas/pasanaku/contrato_de_organizador.dart` |
| 4 | Formulario de **7 campos obligatorios** + 1 opcional: nombre, monto, periodicidad, cupos (3–40), día de cobro (1–28), modalidad de turnos, fecha de inicio; permutas | `servicios/grupos/src/main/resources/openapi/grupos.yaml` → `EntradaGrupo.required` | `apps/movil/lib/pantallas/pasanaku/pantalla_crear_grupo.dart`, `organismos_crear_grupo.dart`, `textos.dart:7-29` |
| 5 | El grupo nace `BORRADOR` y espera a que `tarifas` congele el precio para pasar a `ABIERTO_A_INSCRIPCION` | `docs/CasosDeUso/CU-20 Crear grupo y congelar tarifario.md` flujo 7; ADR-052 DD-5 | `servicios/grupos/.../aplicacion/CU20CrearGrupo.java` |
| 6 | Compartir enlace/QR del grupo (WhatsApp, SMS, copiar) | `docs/trabajo/2026-10-18-app-aprobacion-recarga-invitar/REPORTE.md` H4 | `servicios/grupos/.../aplicacion/CU69EnlaceDeGrupo.java` (javadoc: «No da entrada: cada pedido es una solicitud PENDIENTE que el organizador aprueba (CU-68), decisión del dueño») |
| 7 | Cada invitado: registro + KYC (manual) + contrato + abrir enlace + **pedir cupo** | CU-68 flujo 1-4; `textos.dart:31-44` («Quién decide: el organizador…») | `CU68Postular.java`, `apps/movil/lib/pantallas/pasanaku/pantalla_pedir_cupo.dart` |
| 8 | **Admisión**: el organizador acepta; en producción el interruptor `aportaya.grupos.admision.humana-obligatoria=true` convierte su decisión en propuesta que resuelve **backoffice** | CU-68 nota «Interruptor de admisión humana» (ADR-052 DD-2) | `servicios/grupos/src/main/resources/application.yml:74-78`; `servicios/grupos/.../web/RutasDeSolicitudes.java:28`; `CU68AceptarIngreso.java` |
| 9 | **Firmar el reglamento** (ocupa el cupo; la última firma conforma el grupo) | `docs/trabajo/2026-10-20-firma-ocupa-cupo/` | `CU69FirmarReglamento.java` (javadoc) |
| 10 | El organizador **inicia** el grupo al 100 % o ≥ `ceil(0,75 × cupos)`; el grupo se achica | `docs/trabajo/2026-10-30-regla-sorteo-75-y-comision-por-turno/PLAN.md` ambigüedades 1-3; R-GRP-20 en `docs/Restricciones.md:1909` | `origin/test`: `servicios/grupos/.../aplicacion/CU69IniciarGrupo.java`, `GrupoAbierto.java` |
| 11 | Cada cupo **paga la primera cuota** (recargar + aportar, o QR) | CU-60 precondición 4; R-GRP-20 | `servicios/aportes/.../CU21CobrarAporte.java` |
| 12 | Sorteo commit-reveal con entropía opcional de cada participante | `docs/CasosDeUso/CU-60 Sortear los turnos.md` | `servicios/grupos/.../CU60Sortear.java`; app `pantalla_sorteo.dart`, `ruleta_del_sorteo.dart` |

## B · Por qué hoy hace falta un organizador humano

- **Decisión**: ADR-051 D-12 — «¿Grupos sin organizador humano? **No.** Todo grupo lo crea y administra un
  organizador habilitado por backoffice. El «organizador digital» deja de ser una vía de alta».
  `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:56`.
- **Estado de la decisión**: «para el modo simulado y como diseño de producción **sujeto a ratificación**»
  (línea 48). Ratifica **Producto** (línea 202). Alternativa descartada: «Mantener «organizador digital» como vía
  de alta — … Si Producto lo quiere, es una decisión explícita nueva» (línea ~96).
- **Dónde lo exige el código** (dos capas): `servicios/grupos/.../web/MapeoDeAltaDeGrupo.java:38-44`
  («El creador debe estar habilitado por backoffice y administrar su propio grupo», `AP-CU20-02`) y
  `servicios/grupos/.../aplicacion/CU20CrearGrupo.java:88-90`.
- **Lo que el modelo todavía conserva del grupo sin organizador**:
  - `docs/entidades/02_grupos_turnos.puml:104` → `organizadorId: UUID <<null si es autogestionado>>`;
    `:673` → `es_autogestionado : BOOLEAN`.
  - `servicios/grupos/src/main/resources/openapi/grupos.yaml` → `EntradaGrupo.organizadorId` es
    `type: [string, 'null']` con descripción «Un grupo sin organizador lo lleva la plataforma, y entonces nadie
    cobra por administrarlo (RN-18)».
  - `docs/Restricciones.md:2703` → trigger `grupos.fn_org_validar_contrato_grupo`: `IF NEW.organizador_id IS
    NULL THEN RETURN NEW; -- grupo autogestionado: no hay organizador que deba contrato`.
  - `docs/entidades/07_organizador_automatizacion.md:290-330` → paquete «Organizador Digital», `nivelAutonomia`
    con `SOLO_SUGIERE` / `EJECUTA_CON_AVISO` / `AUTONOMO` («Para grupos autogestionados maduros»).
  - Código vivo del organizador digital: `servicios/organizador/.../CU95DefinirAutomatizacion.java`,
    `CU96EjecutarTarea.java`. Restricción R-ORG-06 en el javadoc de CU-95: «Las que mueven plata ajena exigen
    confirmación humana».
- **Emparejamiento algorítmico ya diseñado**: `docs/CasosDeUso/CU-68 Postular a un grupo y ser emparejado.md`
  flujo 5-7 (`postulacion_emparejamiento`, `propuesta_grupo` con `puntaje_cohesion`, `riesgo_estimado`,
  `expira_en`; materializa por CU-20). Implementado solo el tramo de postulación a grupo existente:
  `CU68Postular.java`, `CU68AceptarIngreso.java`. **No hay clase para `propuesta_grupo`** (búsqueda
  `grep -rln "propuesta_grupo\|PropuestaGrupo" servicios/grupos/src/main` → solo `CU68Postular.java` lo nombra).

## C · Devolución de dinero (los cinco caminos de hoy)

| Caso | CU | Qué exige hoy | Código |
|---|---|---|---|
| Sacar saldo de la billetera al banco | CU-11 + CU-18 | Cuenta bancaria registrada **y verificada** (micro-depósito, consulta al proveedor o comprobante revisado por soporte), `titular_documento` igual al del titular, **ventana de enfriamiento**, MFA, segundo aprobador sobre el tope, ejecución por Tesorería (CU-28) | `servicios/nucleo-financiero/.../CU11RetirarSaldo.java`, `CU11InstruirRetiro.java`; app `pantalla_retirar.dart` |
| Irse de un grupo | CU-65 | Solicitud con motivo; el sistema calcula posición acreedora/deudora; acreedor sin reemplazo **cobra al cierre del ciclo**; deudor necesita plan de pago; el grupo aprueba si el reglamento lo exige | `servicios/grupos/.../CU65Retirarse.java`; app `pantalla_retiro.dart` |
| Grupo que no arrancó | CU-65 flujo 2a | «Retiro inmediato: se libera el cupo, se devuelve lo aportado íntegro» — **por pedido de cada uno**, no automático | — |
| Disolución anticipada | CU-67 | `acuerdo` con quórum o causal del reglamento; prelación de 5 niveles; liquidación por cupo | `servicios/garantia/.../CU67DisolverGrupo.java` |
| Devolución del fondo de garantía | CU-29 | Fondo mutual; ADR-051 D-15 lo declaró **legado** (la reserva empresarial es el único respaldo) | `servicios/garantia/.../CU29DevolverFondo.java` |
| Pago duplicado / erróneo | CU-19 | Soporte solicita, **supervisor aprueba** (`R-SEG-04`), se llama al proveedor, estado hasta el acuse | `servicios/aportes/.../CU19ReembolsarPago.java` |

## D · Pagos (el recorrido de hoy)

| # | Paso | Definición | Código |
|---|---|---|---|
| 1 | Recargar saldo: orden de recarga → QR → webhook firmado → acreditación | CU-10; `docs/Flujo de pantallas · app del participante.md` §4.2 («El único medio de recarga en la app es el QR») | `servicios/nucleo-financiero/.../CU10RecargarSaldo.java`; app `pantalla_recargar.dart` (ofrece además `TARJETA` y `TRANSFERENCIA`, en contradicción con §4.2) |
| 2 | Pagar el aporte desde saldo (saga S1) **o** QR directo a la cuenta del grupo (`orden_cobro` + `qr_cobro`) | CU-21 flujo 2 | `servicios/aportes/.../CU21CobrarAporte.java`, `CU100RecibirWebhookPasarela.java`; app `pantalla_aportar.dart` (medio `QR_INTEROPERABLE`) |
| 3 | Débito programado: retención el día de la autorización, ejecución en la fecha | CU-21 flujo 4 → CU-13 | sin verificar en código |
| 4 | Enlace de pago rápido en el recordatorio | CU-99 flujo 5; CU-81 | sin verificar en código |
| 5 | Proveedor real | CU-99 | **No hay pasarela real**: `seeders/minimos/14-proveedores-externos.json` (`QR_INTEROP`, `ACH_INTERBANCARIA`, …) apunta a `example.bo`; TEST corre `servicios/nucleo-financiero/.../infraestructura/ClienteProveedorSimulado.java` (modo `simulado`, exige ambiente simulado y loopback) |
| 6 | Comisión por turno | `docs/trabajo/2026-10-30-regla-sorteo-75-y-comision-por-turno/ANALISIS-COMISION.md` | recomendación 2 % / piso Bs 10 / techo Bs 300, **pendiente de decisión** |

## E · Límites

- `seeders/minimos/03-limites-operativos.json`: `limite_operativo_billetera` por nivel de debida diligencia
  (`SIMPLIFICADA`, `ESTANDAR`, …), estado `PROVISIONAL`, «El área legal debe verificarlos». Es el único lugar
  del que la propuesta toma topes: no se inventa ninguno (regla 97.5.4).
