# Carril B3 · decisiones tomadas y lo que solo puede cerrar una persona

Servicios: `grupos`, `aportes`, `entregas`, `garantia`, `organizador` (este ultimo sin cambios: no tiene clientes HTTP
salientes ni cambios pedidos). Fecha: 2026-10-10. Resultados de pruebas: ver `REPORTE-B3.md` de esta carpeta.

## 1 · Calendario de aportes (H8.S1.M1)

| Tema | Decision / hecho | Evidencia |
|---|---|---|
| Productor | `GET /grupos/periodos/{periodoId}/calendario-de-aportes` en `grupos` (OpenAPI `grupos.yaml`, esquema `CalendarioDeAportes`) | `RutasDeSolicitudes.consultarCalendarioDeAportes`, `CalendarioDeAportesRepositorio` |
| Permiso | `ADMIN_PLATAFORMA`. Es el permiso de quien dispara la generacion en `aportes` (`generarObligacionesDelPeriodo`) y su token viaja en la llamada, como el recaudo con `ENTREGA_EJECUTAR`. NO se uso una identidad `SERVICIO_APORTES`: nadie aprovisiona credenciales `SERVICIO_*` todavia (riesgo residual ya declarado por el carril N2) y la ruta quedaria inalcanzable | `GruposCalendarioDeAportesWebTest` (403 participante, organizador y rol vacio; 401 sin sesion) |
| Lectura | Con rol de sistema (la politica de fila no deja ver cupos ajenos); la autorizacion la hace el permiso del endpoint | `CalendarioDeAportesRepositorioTest` |
| Consumidor | `CalendarioDelPeriodoPorHttp` (reemplaza `CalendarioNoDisponible`), con timeout y circuit breaker, fallo cerrado | `CalendarioDelPeriodoPorHttpTest`, `CalendarioDelPeriodoCaidaTest` |
| Nivel de la prueba productor/consumidor | Productor: HTTP real + guardia/decodificador reales + PostgreSQL real (`ArranqueCalendarioDeAportesTest`) con la forma del archivo dorado. Consumidor: adaptador real contra un servidor HTTP real que sirve ese mismo archivo dorado. **No** se pudo: cliente de `aportes` contra servidor de `grupos` en una sola JVM, porque `ArquitecturaTest` (`ningunImportCruzado`, ADR-023) prohibe que un servicio importe al otro, tambien en pruebas. El nivel que lo cubre sin romper la frontera es el e2e sobre compose (`e2eTest`), que requiere los servicios levantados | ver reporte |

**Ambiguedad A-B3-1 (importe de un cupo fraccionado)**: el importe es `aporte del grupo x fraccion del cupo`, half-even a 2
decimales. Para fraccion 1.00 (todos los grupos de hoy) no hay redondeo. Si negocio permite fracciones, hay que decidir el redondeo y
quien absorbe el residuo (91.6.5). A: Dirección financiera / Producto.

## 2 · Oferta del mercado para el pago al vendedor (H9.S1.M5)

- `GET /entregas/mercado/ofertas/{ofertaId}` publicado en `entregas.yaml` (`OfertaParaPago`: id, vendedor, precio, estado; nada mas).
- Permiso: `SERVICIO_ENTREGAS` (ya sembrado en `seeders/minimos/10-roles-y-permisos.json`; no hizo falta sembrar nada). Motivo:
  `nucleo-financiero` consulta con el token que recibio en `pago-a-tercero`, que ES la identidad `SERVICIO_ENTREGAS` (la operacion de
  nucleo solo la ejecuta esa identidad). Un `SERVICIO_NUCLEO` exigiria que nucleo se autentique con credencial propia, y la guardia
  admite un solo permiso por ruta. Si nucleo pasa a usar credencial propia, se cambia el permiso aqui.
- Pruebas del lado productor: `OfertaParaPagoWebTest` (8: contrato, respuesta minima, cada estado, 422, 403 usuario final / otras
  identidades, 401, 400, solo lectura) y `CU130OfertaParaPagoTest` (3, PostgreSQL real). El consumidor es `OfertasPorHttp` de nucleo
  (otro agente); su prueba contra el productor real no se pudo por la misma frontera de ADR-023.

## 3 · Circuit breaker de los clientes HTTP (regla 98.2.3; plan en `carril-S2/DEUDA-circuit-breaker-clientes-http.md`)

| Cliente | Servicio | Circuito | Fallback declarado |
|---|---|---|---|
| `RecaudoPorHttp` | entregas, garantia | `aportes` | vacio => la entrega no se liquida / la cobertura se rechaza (AP-CU23-12) |
| `RespaldoPorHttp` | entregas | `garantia` | vacio => se reintenta con la MISMA Idempotency-Key del turno; abierto no sale peticion |
| `TitularPorHttp` (adicional) | entregas | `identidad` | `false` => no se registra la cuenta |
| `HechosPorHttp` | grupos | ocho circuitos (uno por dependido) | cada metodo deniega igual que con el dependido caido; emitir/consumir enlace falla con la excepcion de red |
| `HechosDeGruposPorHttp` (adicional) | aportes | `grupos` | vacio => el pago se rechaza |
| `CalendarioDelPeriodoPorHttp` | aportes | `grupos` | vacio => `PERIODO_NO_CONFIRMADO`, no se genera nada |

- Parametros (ventana 10, umbral 50 %, espera 30 s, 3 llamadas de prueba) **sinteticos**, en `application.yml` de cada servicio
  (`aportaya.resiliencia.circuito.*`); los fija el dueno de cada servicio con su trafico real. Fallo = red, timeout, 5xx, cuerpo ilegible; un 4xx NO abre.
- **Deuda de diseno**: `ClienteConCircuito` esta copiado en los cuatro servicios porque la solucion unica (agregarlo a `ClienteDeServicio`
  de `plataforma/comun-web`) toca `plataforma` y los lockfiles de dependencias, y afecta a nucleo/inversiones (otros agentes). Consolidarlo es un
  micro-PR de plataforma; las pruebas de caida de cada cliente no cambian.
- **HechosPorHttp:224 (telefono en la URL)**: `identidad` ya publica `POST /usuarios/busquedas/por-telefono` y `grupos` lo adopto;
  prueba `HechosCaidaTest.elTelefonoNoVaEnLaUrl`. **Queda abierto** (otro agente, `notificaciones`): `contactoSuprimido` sigue enviando
  `identificador` (telefono) en la query de `GET /notificaciones/supresion?identificador=...`; pasa por POST cuando `notificaciones` lo publique.
- Decision de negocio NO tomada: permitir a `entregas` seguir con el ultimo recaudo conocido si `aportes` cae. Hoy se bloquea. A: Tesoreria/Direccion.

## 4 · Vigencia de la oferta contra el cierre del turno (hallazgo de H9.S1.M1, L7)

- `HechosDeGrupos.Turno.cierreDelTurno` (instante) y regla en `ReglasDeOferta`: la oferta tiene que vencer ESTRICTAMENTE antes del cierre;
  sin cierre informado no se publica (AP-CU130-08); vencer en el cierre o despues, AP-CU130-03.
- Pruebas: `ReglasDeOfertaCierreTest` (6, tres niveles) y los CU130 existentes ajustados al nuevo campo.
- **Abierto (Producto)**: de donde sale ese instante en `grupos`. Candidatos: fin del dia de `periodo.fecha_limite_pago` en America/La_Paz, o
  `periodo.fecha_entrega_prevista`. El adaptador real de `HechosDeGrupos` no existe (`GruposNoDisponible` niega todo) y `grupos`
  tampoco expone el turno; cuando exista, debe derivar el cierre con la regla que Producto fije.

## 5 · Las dos puertas que saltean la admision humana

Interruptor `aportaya.grupos.admision.humana-obligatoria` (`ADMISION_HUMANA_OBLIGATORIA`). **true por omision en `application.yml`**.
- ENCENDIDO: `aceptarInvitacionPorEnlace` responde 422 `AP-CU69-10` sin tocar identidad ni cupos; `decidirSolicitudDeIngreso` responde 422
  `AP-CU68-15` sin invocar el caso de uso. La decision del organizador es la propuesta (`proponerAdmision`) que resuelve backoffice; la entrada por
  invitacion es el canje (`PENDIENTE_BACKOFFICE`).
- APAGADO: comportamiento de TEST sin cambios. **Para que el ambiente TEST conserve su flujo hay que fijar `ADMISION_HUMANA_OBLIGATORIA=false`
  en TEST** (variable nueva; falta cargarla en `scripts/generar_compose.py`, que no es de este carril: ver reporte).
- Limite: "redirige a solicitud pendiente" se implemento como cierre con instruccion y no como creacion silenciosa de la solicitud, porque los
  contratos actuales (`SalidaAceptacionInvitacion.participanteId` obligatorio; `SalidaDecisionDeIngreso.estado` APROBADA|RECHAZADA) no pueden
  representar «pendiente» sin romper clientes. Un redirect real exige una version nueva del contrato.
- Pruebas: `GruposAdmisionHumanaWebTest` (encendido, puerta 2 y camino con expediente), `GruposSolicitudesWebTest` (apagado, puerta 2),
  `InvitacionesAdmisionHumanaWebTest` (ambos modos, puerta 1).
- **DECISION PENDIENTE**: si toda entrada pasa por admision humana en produccion. A: Negocio y Cumplimiento.

## 6 · Sustitucion del administrador al revocarse la habilitacion (H5.S1.M7)

- Modo manual por omision (`aportaya.grupos.sustitucion.automatica=false`): el evento `organizador.habilitacion_revocada` solo emite
  `grupos.sustitucion_requerida` por grupo vivo (backoffice sustituye con `POST /grupos/{id}/administrador/sustitucion`).
- Modo automatico (true): sustituye al participante ACTIVO mas antiguo que `organizador` confirma habilitado; sin candidato o sin respuesta,
  escala (falla cerrado). La regla de seleccion es SINTETICA, no una decision de negocio. El consumidor Kafka
  (`ConsumidorDeRevocacionesKafka`) esta apagado por omision (`aportaya.grupos.sustitucion.consumo-habilitado`) hasta que operaciones confirme
  la configuracion del consumidor; no hay prueba contra un broker real.
- Pruebas: `CU68SustitucionAutomaticaTest` (ambos modos, idempotencia, fallo cerrado, rol, alcance).
- **DECISION PENDIENTE**: automatica o siempre una persona, y la regla de eleccion del entrante; retirar al saliente es otro acto (CU-65).
  A: Negocio y Cumplimiento.

## 7 · Diccionario de variables de riesgo (H6.S1.M1)

`docs/operacion/diccionario-variables-de-riesgo.md`, derivado de `CU68Postular`, `MotorDeRecomendacion`, `CriterioDeEmparejamiento` y de la
evidencia por postulacion. Ambiguedades A1-A7 marcadas (definicion de «apartamiento», consentimiento aplicable, afinidades hoy neutras, frescura,
retencion, valores sinteticos). A: Cumplimiento, Legal, Riesgos, Producto.

## 8 · No tocado

`identidad.subirDocumento` (otro agente), `nucleo-financiero`, `inversiones`, `notificaciones`, `cumplimiento`, `plataforma/*`.
