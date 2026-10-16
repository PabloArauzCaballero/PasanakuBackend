# Contrato que `inversiones` consume de `nucleo-financiero` y el doble contra el que se cerró (regla 65)

Carril I2 · 2026-10-09. El productor real (`debitarInversion` / `acreditarRescate` en `servicios/nucleo-financiero`)
lo construye **otro agente**: este carril no lo toca. Todo lo de abajo está cerrado **contra un doble declarado**,
no contra el productor. Complementa `evidencia/carril-D/contrato-requerido-nucleo.md` (qué se le pide al núcleo);
este documento fija **cómo lo llama el cliente** y **qué prueba qué**.

## 1. Operaciones y quién las firma

| Operación del puerto `LibroDelTitular` | Ruta | Identidad con que sale | Idempotency-Key |
| --- | --- | --- | --- |
| `saldo(cuentaId)` | `GET /billetera/{id}/saldo` | Token de la persona | — |
| `retener` | `POST /billetera/retenciones` | Token de la persona; sin sesión (reintento del reloj), la credencial de servicio | UUID derivado de `RETENER:<orden>` |
| `liberar` | `POST /billetera/retenciones/{id}/cierre` | Ídem | UUID derivado de `LIBERAR:<orden>` |
| `debitarInversion` | `POST /billetera/inversiones/debitos` | **Solo credencial de servicio** (nunca el token de una persona) | UUID derivado de `DEBITAR:<orden>` |
| `acreditarRescate` | `POST /billetera/inversiones/acreditaciones` | **Solo credencial de servicio** | UUID derivado de `ACREDITAR:<rescate>` |

- La credencial de servicio sale de `aportaya.inversiones.libro.credencial-servicio`, que se alimenta de la variable
  `INVERSIONES_LIBRO_CREDENCIAL_SERVICIO` (declarada en `scripts/generar_compose.py`, vacía en el repo; el valor real lo
  pone quien opera el entorno). Se envía como `Authorization: Bearer <credencial>` y **no se registra jamás**.
- Sin credencial configurada `debitarInversion` y `acreditarRescate` **fallan cerrados** (`LibroNoDisponible`) sin
  mandar nada: la instrucción queda `PENDIENTE`, se reintenta con la misma clave cuando haya credencial, y si pasa el
  máximo de intentos queda **atascada y visible** (medidor `inversiones.instrucciones.atascadas`).
- Qué debe aceptar el productor (pedido al carril del núcleo): esas dos rutas, solo con identidad de servicio con el
  alcance de inversión; `422 AP-CU13-01` = saldo insuficiente (definitivo); cualquier otro no-2xx el cliente lo trata
  como «no se sabe» y reintenta con la misma clave; partida doble con cuenta de custodia de inversión propia.

## 2. El doble y sus tres niveles

El doble es `LibroDoble` (puerto, para los casos de uso) y un servidor HTTP local (`BaseDelLibroHttp`, para el
adaptador `LibroPorHttp`). Ninguno es el productor.

| Nivel | Qué se ejerce | Dónde |
| --- | --- | --- |
| Correcto | Saldo con titular y comprometido en pozos; retener con motivo `INVERSION_VOLUNTARIA` y clave estable; liberar, debitar y acreditar a sus rutas; debitar/acreditar con `Bearer <credencial>` aunque haya sesión de persona | `LibroPorHttpContratoTest`, `LibroPorHttpCredencialContratoTest` |
| Límite | Misma clave dos veces = mismo UUID; credencial con espacios se recorta y una en blanco equivale a no tenerla; sin sesión usa la credencial para retener/liberar; el 422 de saldo insuficiente no abre el circuito | ídem |
| Inválido | 422 con otro código, 5xx, cuerpo ilegible, saldo sin titular o sin `comprometidoEnPozos`, timeout, circuito abierto tras fallas repetidas, **sin credencial de servicio**: no sale ni una petición | ídem |

Los casos de uso se cierran contra `LibroDoble` con sus caídas inyectadas por operación (`fallarEn`): CU121–CU125,
`OrdenSagaTest` y `CU122ReintentoTest`.

## 3. Qué NO está verificado (queda para cuando exista el productor)

- Que `nucleo-financiero` publique y acepte las dos rutas con la forma y la identidad de arriba (prueba de contrato del
  lado productor: del carril del núcleo).
- Que el `titularId` y `comprometidoEnPozos` del saldo existan en el productor.
- El flujo de punta a punta con trazas reales y `correlationId` cruzando los dos servicios.
- La rotación de la credencial de servicio y su alcance real (decisión de seguridad).

## 4. Estado de H10.S1.M1–M6 contra este contrato

| ID | Estado | Detalle |
| --- | --- | --- |
| H10.S1.M1 | HECHO (backend y contrato) | Catálogo con fecha de cotización, fuente, riesgo, titularidad y costos; `GET /inversiones/productos/{codigo}`; `AP-CU120-01/03` ahora los emite un camino real. **No cubierto:** las pantallas diferenciadas son del frontend |
| H10.S1.M2 | HECHO contra el doble | Saldo retenido y consentimiento versionado; `retener` contra `LibroDoble` y el servidor HTTP del contrato. Falta la prueba contra el productor |
| H10.S1.M3 | HECHO contra el doble | Conciliación posición/efectivo/operación (`CU122Test`, cuadre global de `OrdenSagaTest`); `debitarInversion` con identidad de servicio contra el doble. Falta el productor |
| H10.S1.M4 | HECHO | Cálculo independiente y liquidación al vencimiento (180 devengos = 197,26); devengo ahora por reloj |
| H10.S1.M5 | HECHO | Caída de valor, dato desactualizado visible; sincronización de valores ahora por reloj |
| H10.S1.M6 | HECHO contra el doble | Rescate demorado, rechazo de doble disponibilidad, acreditación con identidad de servicio. Falta el productor de `acreditarRescate` |
| H10.S1.M7 | HECHO | Bloqueo `AP-CU121-05` + simulación de caja separada (CU-129) |
