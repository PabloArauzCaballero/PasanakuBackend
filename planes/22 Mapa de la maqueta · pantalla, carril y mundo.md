---
tags:
  - plan
  - frontend
  - maqueta
  - carriles
titulo: "Mapa de la maqueta — cada pantalla, su carril y su mundo (Flutter o Angular)"
fecha: 2026-09-09
depende_de: [F0, F1]
afecta: [F1, F2, F3, F4, F5, F6, F7, F8, F13, F14]
---

# Mapa de la maqueta · pantalla, carril y mundo

> **Qué es este documento.** La traducción, pantalla por pantalla, de las dos maquetas
> —[[AportaYa-Maqueta]] (el producto núcleo, versión 3) y
> [[README|Maqueta de Crecimiento Financiero y Alianzas]]— al stack de
> [[ADR-044 Frontend en Angular y Flutter]]. Para cada pantalla dice **en qué mundo se
> escribe** (Flutter o Angular), **qué carril la posee**, **cómo se llama su ruta** en
> `go_router` o en Angular Router, **qué organismos compone** y **qué delta de
> [[20 Maqueta de referencia · deltas del frontend]] la fija**. Es lo que hace que «se
> parezca a la maqueta» sea un criterio verificable y no una opinión.

> [!important] Las tres reglas que este mapa aplica
> 1. **La maqueta manda sobre el cómo; el caso de uso manda sobre el qué.** Si difieren
>    en el *qué*, gana el CU y se corrige la maqueta ([[20 Maqueta de referencia · deltas del frontend]] §3).
> 2. **Toda pantalla se compara contra la suya en los dos escenarios**, optimista y
>    adverso. El estado feo es parte del alcance; una pantalla que solo se probó con el
>    día bueno no cierra.
> 3. **Lo que la maqueta muestra en un mundo tiene que verse igual en el otro.** El
>    mismo organismo, con el mismo nombre, en `packages/diseno_flutter` y en
>    `@aportaya/ui`; los dos leen los mismos tokens generados y se revisan lado a lado.

---

## 1 · Cómo se lee una ruta en cada mundo

La maqueta escribe rutas con la convención de archivos (`pasanaku/grupo/[codigo]`). En
el stack nuevo no hay descubrimiento por archivos: hay **un enchufe por dominio** que el
shell deja congelado, y cada carril registra sus rutas en el archivo de su dominio.

| En la maqueta | Flutter (`go_router`, rutas tipadas) | Angular (Router, `loadChildren` por dominio) |
| --- | --- | --- |
| `pasanaku/grupo/[codigo]` | `GrupoRuta(codigo: …)` en `lib/pantallas/pasanaku/rutas.dart` → `/pasanaku/grupo/:codigo` | — |
| `cumplimiento/verificaciones` | — | `{ path: 'verificaciones' }` en `rutas/cumplimiento/cumplimiento.routes.ts` → `/cumplimiento/verificaciones` |
| `[id]` | `:id` como parámetro tipado; **ids opacos**, nunca un documento ni un teléfono | `:id` por `withComponentInputBinding()`; ídem |

**Un archivo por pantalla**, en `lib/pantallas/<dominio>/` o en `rutas/<dominio>/<pagina>/`,
más una entrada en el archivo de rutas **de ese dominio**. Nada del shell cambia.

---

## 2 · App del participante — Flutter · `apps/movil/lib/pantallas/`

### 2.1 · Identidad · carril **M1** (F3)

| Pantalla de la maqueta | Ruta Flutter | Organismos (`diseno_flutter` salvo nota) | CU | Delta |
| --- | --- | --- | :-: | :-: |
| Bienvenida | `/identidad/bienvenida` | `PanelBienvenida` | — | D-8 |
| Tour (4 pantallas, saltable) | `/identidad/tour` | `Onboarding` + `BarraDePuntos` | — | D-8 |
| Crear cuenta · 1 de 8 | `/identidad/registro` | `BarraDePasos` · `FormularioRegistro` (contraseña con medidor) | 01 | D-1 |
| Confirmar el celular · 2 de 8 | `/identidad/verificar-celular` | `CampoOTP` con cuenta de intentos y bloqueo por hora **a la vista** | 01 | D-1 |
| Anverso · 3 de 8 | `/identidad/documento/anverso` | `MarcoDeCamara` (**`apps/movil`**, sobre el puerto `Camara`) con los cuatro controles de calidad | 02 | D-1 |
| Reverso · 4 de 8 | `/identidad/documento/reverso` | `MarcoDeCamara` con zona de lectura mecánica | 02 | D-1 |
| Selfie de seguridad · 5 de 8 | `/identidad/vivacidad` | `MarcoDeCamara` con reto de movimiento y puntaje contra umbral | 02 | D-1 |
| Revisá tus datos · 6 de 8 | `/identidad/cotejo` | `FilaDeCotejo` ×n, con la diferencia marcada y **editable** | 02 | D-1 |
| Perfil del cliente · 7 de 8 | `/identidad/perfil-cliente` | `FormularioKYCReforzado` (`DeclaracionPEP` en lenguaje llano) | 03 | D-1 |
| Contrato y tarifario · 8 de 8 | `/identidad/contrato` | `VisorContrato` (entero, con hash) + tres consentimientos **separados** | 05 | D-1 |
| Tu cuenta está abierta | `/identidad/alta-completa` | `PantallaResultado` con nivel, **límites concretos**, qué falta y plazo | — | D-1, D-9 |
| Iniciar sesión | `/identidad/sesion` | `FormularioLogin` (teléfono + PIN o biometría por el puerto `Biometria`) | 04 | — |
| Segundo factor | `/identidad/mfa` | `CampoOTP` | 04 | — |
| Dispositivos de confianza | `/identidad/dispositivos` | `ListaDispositivos` · `RegistroDispositivo` | 04 | — |
| Elevar verificación | `/identidad/verificacion-profunda` | `FormularioKYCReforzado` mostrando **qué desbloquea cada nivel antes** | 02, 03 | — |
| Perfil · contraseña · baja | `/identidad/{perfil,contrasena,baja}` | `FormularioPerfil` · `FormularioCambioContrasena` · `AsistenteBaja` (impedimentos listados) | 07, 09 | — |
| Servicio no habilitado | `/identidad/no-habilitado` | `EstadoVacio` variante explicativa, sin jerga | 46 | — |

**Lo que la maqueta fija y el carril no puede aflojar:** el código de verificación es un
átomo con **tope de 3 intentos y bloqueo por hora**, y el mensaje dice cuántos quedan;
la prueba de vida bajo el umbral reintenta con motivo y al tercer intento pasa a
revisión asistida; el cotejo se puede corregir **antes** de seguir; al terminar se
acredita el bono de bienvenida y la app abre en el **estado de cuenta nueva** (D-9), no
en el de alguien con historial.

### 2.2 · Shell · carril **M** (F2) · `lib/navegacion/` y `lib/pantallas/notificaciones/`

| Pieza de la maqueta | Flutter | Delta |
| --- | --- | :-: |
| Tab bar: Inicio · Grupos · Movimientos · Perfil · campana | `StatefulShellRoute` con `BarraPestanas`; la campana con `Dot` de no leídos | — |
| Avisos (bandeja) | `/notificaciones/bandeja` · `BandejaNotificaciones` (`FilaNotificacion` con ícono y tono) | D-11 |
| Notificación emergente | `NotificacionEmergente`: **se encolan**, contador de las que esperan, tocar abre la bandeja | D-11 |
| Eventos que **no** notifican | Lista cerrada en `dominio/notificaciones.dart`: LGI/FT y límite de intentos nunca producen aviso | D-11 |
| Escenario adverso | `proveedorConexion` + `EstadoError.sinConexion`; el mando de escenario de la maqueta es Prism con `Prefer: example=adverso` | — |

### 2.3 · Billetera y alianzas · carril **M2** (F4) · `lib/pantallas/{billetera,alianzas}/`

| Pantalla de la maqueta | Ruta Flutter | Organismos | CU | Delta |
| --- | --- | --- | :-: | :-: |
| Inicio (portada) | `/billetera/inicio` | **En este orden:** línea de saldo · `TarjetaDeRacha` · `RielDeTurnos` · tarjeta de grupo en formación · `AccesosRapidos` (Recargar · Retirar · Vales · Aportes) · `TarjetaSaldo` completa (custodia, retenido, en pasanakus) · `ListaMovimientos` (5) · `BannerDePauta` **al pie** | 13 | D-4, D-7, D-9, D-14 |
| Portada de cuenta nueva | ídem, estado propio | `PanelBienvenida` con el bono y los tres pasos siguientes; `EstadoVacio` en grupos y movimientos | — | D-9 |
| Recargar (QR) | `/billetera/recargar` | `CampoMonto` + `TecladoNumerico` · `PantallaQR` (**`apps/movil`**, `CodigoQR` con cuenta regresiva a la vista) · `ResumenRecarga` | 10 | — |
| Retirar | `/billetera/retirar` | `SelectorCuentaBancaria` · `CampoMonto` · **costo y neto antes de confirmar** · `HojaDeConfirmacion` con biometría | 11, 18 | — |
| Mis cuentas de destino | `/billetera/cuentas-bancarias` | `ListaCuentasBancarias` · `FormularioCuentaBancaria` · `CuentaEnmascarada` | 18 | — |
| Mis aportes · lista | `/billetera/aportes` | `SelectorSegmentado` (lista · calendario) · `ChipsDeFiltro` (grupo · estado, **en varias líneas**) · `ResumenDePeriodo` · `FilaAporte` ×n con recargo desglosado · «quedan N hasta …» | 21 | D-4, D-12, D-22 |
| Mis aportes · calendario | `/billetera/aportes?vista=calendario` | `CalendarioDeCuotas`: relleno **y borde** por estado, `FUTURA` punteada y **sin sumar**, dos grupos en un día, aro de *hoy*, leyenda con las mismas reglas, «mes 6 de 14», **volver a hoy** | 21 | D-12, D-22 |
| Pagar una cuota | `/billetera/aportes/:id` | `FormularioAporte` (**un** botón, doble envío bloqueado) | 21 | D-4 |
| No voy a poder pagar | `/billetera/aportes/:id/no-puedo-pagar` | `OpcionConCosto` ×4 · `EscaleraDeEtapas` (las 6, con canales y tope) · `RelojDePlazo` ×2 (descargo y apelación, **guardados**) · cierre con el fondo de garantía | 23, 25 | D-17 |
| Confirmación | `/billetera/confirmacion` | `PantallaResultado`; el `202` se muestra **pendiente** | — | — |
| Movimientos (extracto) | `/billetera/extracto` | `ResumenDePeriodo` (saldo inicial → hoy, entró, salió) · `ChipsDeFiltro` (5, cada uno con ícono, **sin «otros»**) · `ListaMovimientos` agrupada por día con **un saldo por día** · `FilaDeMovimiento` tipada por lo que pasó | 14, 15 | — |
| Cobrás tu turno | `/billetera/entregas/mi-turno/cobro` | `DesgloseDeCobro`: bolsa · comisión · **descuento por nivel del contrato de 2B** · neto en grande; la nota de «bolsa chica, piso de Bs 10» con la tabla | 22, 31 | D-19 |
| Mis vales | `/alianzas/mis-vales` | total ahorrable · `Vale` ×n con estado en fila propia y **de dónde salió** | 113 | D-21 |
| Usar el vale | `/alianzas/vales/:id/uso` | `Vale` con `CodigoQR` **rotativo cada 30 s**, código corto, condiciones; al canjear «ahorraste Bs X y **no se descontó del saldo**»; `AP-VAL-03` al segundo canje | 113 | D-21 |
| Sobre este aviso | `/billetera/publicidad/espacio` | explicación, anunciante, control de **segmentación** (no de cierre) | — | D-7 |

**Lo que la maqueta fija:** ninguna barra de filtros se desliza en horizontal; el
segmentado **se ve elegido en los dos temas** con relleno de marca; el saldo **se relee**
tras cada operación, nunca se ajusta en memoria; el banner no tiene cierre y vive solo en
la portada; el número de cuenta va enmascarado; ningún porcentaje de descuento cableado.

### 2.4 · Pasanaku y soporte · carril **M3** (F5) · `lib/pantallas/{pasanaku,soporte}/`

| Pantalla de la maqueta | Ruta Flutter | Organismos | CU | Delta |
| --- | --- | --- | :-: | :-: |
| Mis pasanakus (pestaña Grupos) | `/pasanaku/mis-grupos` | `ListaGrupos` (`TarjetaGrupo` con **el rol en ese grupo**, estado, turno, avance; en formación: cupos que faltan **y nada de turnos**) · `EstadoVacio` invita a entrar con código | — | D-9, D-13 |
| Unirme a un pasanaku | `/pasanaku/unirse` | `SelectorSegmentado` (**Escanear QR** · Con el código) · `LectorQR` (**`apps/movil`**) · `CampoCodigo` (5 dígitos) → la misma confirmación | 69 | D-10, D-13 |
| Confirmación del canje | `/pasanaku/unirse/confirmar` | ficha del grupo · **compromiso del ciclo en números** · `ReglamentoGrupo` entero · casilla de confirmación · botón ***Pedir mi cupo*** · `404` sin código vs `AP-CU69-05` | 68, 69 | D-10, D-15 |
| Tu pedido de cupo | `/pasanaku/solicitudes/:id` | quién decide **con nombre** · `RelojDePlazo` (48 h, guardado) · **dos columnas** qué ve y qué no ve · las tres salidas · «no se te cobra ni se aparta saldo» | 68 | D-15 |
| Quién quiere entrar (organizador) | `/pasanaku/grupo/:codigo/solicitudes` · guard `soyOrg` → `AP-CU68-07` | `TarjetaDeSolicitud` ×n con puntaje **descompuesto** · plazo por pedido, el que vence hoy en rojo · **rechazar exige motivo**, confirmar en blanco imposible · aceptar idempotente | 68 | D-15 |
| Emitir una invitación | `/pasanaku/grupo/:codigo/invitar` · solo quien organiza (`AP-CU69-07`) | `CodigoQR` + código corto · cupos, vencimiento, un solo uso · **qué ve quien la recibe** · qué pasa en cada borde · anular | 69 | D-13 |
| Postular a un grupo | `/pasanaku/postular` | `FormularioPostulacion` | 68 | — |
| Detalle de grupo y turnos | `/pasanaku/grupo/:codigo` | `TarjetaGrupo` · `ReglamentoGrupo` · `ListaTurnos` (`FichaTurno`; tocar a alguien abre su perfil público) · acceso a *Tu turno* | 60 | D-6 |
| Verificar el sorteo | `/pasanaku/grupo/:codigo/sorteo` | `PanelSorteo`: los **cinco pasos con su hash**, reproducción con la semilla, comparación con el guardado; enlace a la verificación pública | 61 | D-5 |
| Tu turno (mercado) | `/pasanaku/grupo/:codigo/mi-turno` | `MedidorDeRango` (**estimado**), demanda e interesados; por debajo del puntaje mínimo **no se abre y explica**, con enlace a *Tu nivel* | 62 | D-20 |
| Ceder mi turno | `/pasanaku/grupo/:codigo/ofertas/nueva` | hasta dónde correrse · cuánto pedir · **tope del 5 % marcado antes** (`AP-CU62-05`) · cancelar mientras nadie la tomó | 62 | D-20 |
| Ofertas del grupo | `/pasanaku/grupo/:codigo/ofertas` | `TarjetaDeOferta` ×n · aceptar → `PanelDeFactores` (**cinco factores con umbral**), `EN_VALIDACION` **después** de `ACEPTADA` | 62 | D-20 |
| Perfil público de otro | `/pasanaku/participantes/:codigo` | `TarjetaReputacion` pública: puntaje, nivel, ciclos, aportes a tiempo, insignias. **Ni documento, ni teléfono, ni saldo**; en mora dentro de plazo **no se le llama deudor** | 71 | D-6 |
| Detalle de una insignia | `/pasanaku/insignias/:clave` | qué mide · cómo se gana · para qué sirve · cuánta gente la tiene | 74 | D-6 |
| Mi perfil (pestaña Perfil) | `/pasanaku/reputacion` | `TarjetaReputacion` **con desglose**; `SIN_HISTORIAL` con los factores en cero y su motivo | 71, 74 | D-9 |
| Tu nivel | `/pasanaku/nivel` | qué habilita cada nivel (topes) · **la aritmética** contra `COM_ENTREGA` y el respaldo uno a uno · sin bono en efectivo | 71 | D-19 |
| Organizar un grupo | `/pasanaku/organizador` | `ListaDeRequisitos` (14, **del contrato de 2E**, tu valor al lado del umbral y el código de la fila) · los cinco pasos · «capacitación vencida suspende, no quita» | 90 | D-16 |
| Crear grupo 🔒 | `/pasanaku/crear` | `AsistenteOrganizador` · `FormularioGrupo` con **costo total del ciclo antes de confirmar** | 20 | — |
| Reseñar | `/pasanaku/resenar/:id` | `FormularioReseña` (`EstrellasCalificacion`) | 76 | — |
| Decisiones del grupo | `/pasanaku/grupo/:codigo/decisiones` | tarjeta por acuerdo con propuesta, plazo y quórum | 63–65 | — |
| Aviso de incumplimiento · Mis avales · Restricción · Mi entrega · Fondo | `/pasanaku/{incumplimientos/:id,avales,restriccion,entregas/:id,fondo}` | `RelojDePlazo` (plazo guardado) · `LineaDeTiempo` · aviso **persistente no bloqueante** | 23, 25–29 | — |
| Ayuda | `/soporte/ayuda` | los **cuatro canales = un Punto de Reclamo** · consulta / reclamo / denuncia / desconocimiento de cargo · «nadie te pide la contraseña ni el PIN» | 52 | D-18 |
| Hacer un reclamo | `/soporte/reclamos/nuevo` | `SelectorCategoria` · adjuntos · al enviar: **número correlativo y fecha límite** (`RelojDePlazo`), prórroga a 10 avisando, segunda instancia y ASFI | 52, 53 | D-18 |
| Denunciar | `/soporte/denuncias/nueva` | `FormularioDenuncia` (puede ser anónima) | — | D-18 |

### 2.5 · Crecimiento y alianzas · **fuera de alcance hasta M15–M18**

Las seis pantallas de `Maqueta-Crecimiento/app/` (Mi Dinero, Objetivo de ahorro, Mercado
de turnos, Mis vales, Tu turno llegó, Invertir) **no tienen módulo en la bóveda** todavía
([[21 Crecimiento Financiero y Alianzas — deltas de backend, frontend y carriles]] §3).
Dos ya entraron al núcleo por los deltas D-20 y D-21 (mercado de turnos, vales) y están
arriba. Las otras cuatro esperan a M15–M18. Cuando entren: **Flutter, mismo paquete de
diseño, carriles `F15`–`F18`**, y las piezas de `maqueta.css` (vale con muesca, medidor,
embudo) se portan a `diseno_flutter` como parte de ese carril, no antes.

---

## 3 · Backoffice — Angular · `apps/backoffice/src/app/rutas/`

Cada pantalla lleva arriba la banda **«Para qué sirve»** con una frase de negocio (regla
§0.6 de [[Flujo de pantallas · backoffice administrador]]). Es un organismo de
`@aportaya/ui` (`BandaDeProposito`) y su texto vive en `textos.ts` del dominio.

### 3.1 · Acceso y shell · carril **B** (F6)

| Pantalla | Ruta Angular | Organismos (`@aportaya/ui`) | Delta |
| --- | --- | --- | :-: |
| Credencial · Segundo factor · Enrolamiento · Recuperación · Aprobaciones · Reinscripción | `/acceso/{ingreso,desafio,enrolamiento,recuperacion,aprobaciones,reinscripcion-factor}` | `FormularioLogin` · `CampoOTP` · `PanelAprobacion` (**otra identidad aprueba**) · `AP-CU04-06` para el operador sin TOTP | — |
| Tablero | `/tablero` | `PanelKPIs` (`TarjetaKPI` con «ampliar» y «qué es esto») | — |
| Estado de plataforma | `/operacion/estado` | `PanelEstadoPlataforma` | — |
| Los dos shells | `layout/financiero/` · `layout/sistemas/` | menú por rol; **no comparten menú ni permisos** | D-2 |

### 3.2 · Operación · carril **B1** (F7) · `rutas/operacion/`

| Pantalla de la maqueta | Ruta Angular | Organismos | CU | Delta |
| --- | --- | --- | :-: | :-: |
| Conciliación y encaje | `/operacion/conciliacion` | `TablaDeDatos` · `PanelDescuadre` · ratio de encaje por día; en adverso, **bajo el mínimo** | 50 | — |
| Cierre diario | `/operacion/cierre-diario` | `ResumenCierre` con **la lista de lo que impide cuadrar**; no se confirma si no cuadra | 51 | — |
| Desembolsos | `/operacion/desembolsos` | `TablaDeDatos` por estado y antigüedad · `PanelAprobacion` **o** `PanelEjecucion` según rol, nunca los dos | 28, 14 | — |
| Reclamos | `/operacion/reclamos` | `TablaDeDatos` ordenada por **vencimiento** · `FichaReclamo` (`LineaDeTiempo`, `RelojDePlazo` guardado) · favorable **sin reparación no cierra** | 52, 53 | D-18 |
| Solicitudes escaladas | `/operacion/solicitudes-escaladas` | `TarjetaDeSolicitud` ×n, las que el organizador dejó vencer | 68 | D-15 |
| Políticas de resolución | `/operacion/politicas-resolucion` | `EditorDePolitica` · `SimuladorDeReglas` | — | — |
| Roles y accesos | `/operacion/roles` | `EditorDeRoles` | 08 | — |
| Tarifario | `/operacion/tarifario` | `FormularioTarifario` · `PanelPreaviso` · **simulación obligatoria antes de aprobar** | 34 | — |
| Reportes | `/operacion/reportes` | `ConstructorDeReporte` con **el permiso que exige** · `Exportador` por CU-58 | 58 | — |
| Fondeo (QR) | `/operacion/fondeo` | `FormularioInstrumentoFondeo` · `VistaPreviaQR` · `HistorialFondeo` · `PanelSaludProveedor` | 99 | — |
| Mensajería | `/operacion/mensajeria` | `EditorDePlantilla` · `MatrizDeCanales` · `PanelRuteoProveedor` · `PanelSaludProveedor` | 80, 83 | — |
| Cobranza y mora · entregas · fondo por grupo | `/operacion/{cobranza,entregas,fondo}` | `TablaDeDatos` con **brecha para completar la bolsa** · tablero de entregas con doble control · `EscaleraDeEtapas` | 21–29 | D-17 |
| Reversos · oficios · verificación de cuentas | `/operacion/{reversos,oficios,cuentas}` | reverso con **doble aprobación y motivo**; oficio **sin archivo no se ejecuta** | 14, 17, 18 | — |

### 3.3 · Cumplimiento y gobierno · carril **B2** (F8.A–C) · `rutas/cumplimiento/`

| Pantalla de la maqueta | Ruta Angular | Organismos | CU | Delta |
| --- | --- | --- | :-: | :-: |
| Verificaciones (cola + expediente) | `/cumplimiento/verificaciones` · `/…/:id` | `RevisorDeIdentidad` = cola + **`SeccionDeExpediente` ×9** (identidad, autenticidad, biometría con 1:N, listas con puntaje difuso, perfil, dispositivo, composición del riesgo, historial, decisión) · **causal del catálogo obligatoria** · segunda firma en riesgo alto · **quien cargó no decide** | 01, 02 | D-3 |
| Habilitación de organizadores | `/cumplimiento/organizadores` · `/…/:id` | cola con nivel pedido, **puntaje congelado**, requisitos cumplidos sobre el total · `ListaDeRequisitos` del contrato de 2E · el bloque **«lo que no puede pasar»** | 90 | D-16 |
| Alertas · Casos y ROS · UIF | `/cumplimiento/{alertas,casos,uif}` | `PanelTriaje` · `FichaCaso` · `PanelROS` · `FormularioRequerimiento`; **ningún rastro para el investigado** | 41–45 | D-11 |
| Reglas con simulador | `/cumplimiento/reglas` | `SimuladorDeReglas`; activar **deshabilitado sin corrida** | 48 | — |
| Reportes, contabilidad y tarifas | `/cumplimiento/{calendario,libro-mayor,cierre-mensual,encaje,fiscal,segmentos,dias-no-habiles}` | cierre mensual **con el cuadre a la vista** · tablero de encaje · calendario **con la fuente de cada día** | 30–36, 40 | — |
| Gobierno · riesgo de producto · comités · indicadores | `/cumplimiento/{productos,gobierno,comites,indicadores}` | `ActaComite` (**quórum y voto nominal**) · matriz de riesgo con no objeción · tablero con lo **provisorio marcado** | 91–98 | — |

### 3.4 · Sistemas · carril **B5** (F8.D) · `rutas/sistemas/` + `layout/sistemas/`

| Pantalla de la maqueta | Ruta Angular | Organismos | Delta |
| --- | --- | --- | :-: |
| Estado de servicios | `/sistemas/estado` | criticidad y **qué se cae con qué** | D-2 |
| Salud y SLO | `/sistemas/slo` | `PanelSLO`: p95/p99 y **presupuesto de error consumido**; agotado ⇒ **lo declara** | D-2 |
| Despliegues | `/sistemas/despliegues` | `PanelDespliegues` · interruptores con **doble firma** si tocan dinero | D-2 |
| Base y migraciones | `/sistemas/base-de-datos` | versión, pendientes, retraso de réplica, pool | D-2 |
| Respaldos y restauración | `/sistemas/respaldos` | `PanelRespaldos`: RPO/RTO objetivo contra medido · **última restauración probada**, vencida a los 30 días | D-2 |
| Proveedores externos | `/sistemas/proveedores` | `PanelSaludProveedor` con **costo real por operación** y reglas de conmutación | D-2 |
| Outbox y trabajos | `/sistemas/outbox` | `PanelColas` con **descartados** visibles y motivo; reintentar exige confirmar | D-2 |
| Webhooks entrantes | `/sistemas/webhooks` | `PanelWebhooks`: duplicados, fuera de orden, firma inválida y qué hace con cada uno | D-2 |
| Accesos · Incidentes | `/sistemas/{accesos,incidentes}` | `EditorDeRoles` · `FichaIncidente` con los tres relojes (**se mudan** de B1 y B2) | D-2 |

### 3.5 · Contabilidad y publicidad · carriles **B3** (F13) y **B4** (F14)

Las pantallas están en [[Flujo de pantallas · backoffice administrador]] §5 y §6; las tres
de `Maqueta-Crecimiento/backoffice/` (Alianzas, Campañas y vales, Riesgo de permutas)
entran cuando exista M15–M18, salvo **Riesgo de permutas**, que ya tiene contrato por D-20
y va en **B2** como `/cumplimiento/riesgo-permutas` con `PanelDeFactores`.

### 3.6 · Las cinco pantallas de negocio de la maqueta

*Ingresos y tarifario*, *Libro contable*, *Fondo de garantía*, *Arquitectura* y *Licencia
ASFI* existen en la maqueta para una audiencia mixta. **No son pantallas del producto**:
son la presentación. Lo que sí queda de ellas son tres organismos reutilizables que
suben a `@aportaya/ui`: `PanelDeIngresos` (`/cumplimiento/ingresos`, B2),
`VisorEstadoFinanciero` (B3) y `PanelFondo` (`/operacion/fondo`, B1). *Arquitectura* y
*Licencia* viven en el sitio público como `/legal/estado-regulatorio` (W).

---

## 4 · Sitio público — Angular SSR · `apps/web/`

Las cuatro rutas de verificación (`/verificar/:codigo`, `/publico/grupos/:codigo`,
`/publico/sorteos/:id`) y las cuatro islas están en
[[14 Fases F9 a F11 · Sitio público, SEO y GEO]]. La maqueta no las dibuja: su criterio
visual es el de `PanelSorteo` de la app (D-5), portado a `@aportaya/ui` como
`VerificadorDeSorteo` con **los cinco pasos y su hash**.

---

## 5 · Portal Partner — **cuarta superficie, reservada**

`Maqueta-Crecimiento/partner/` dibuja cuatro pantallas para el comercio aliado (Dashboard,
Publicaciones, Validar vale, Audiencia) en **su propio dominio**. No existe en ADR-044 y
**no se planifica** hasta que M16 tenga módulo. Lo que se deja fijado para ese día:

| Decisión | Por qué |
| --- | --- |
| Es **Angular**, en `apps/partner`, con `@aportaya/ui` | Misma tecnología que las otras dos superficies web; comparte tokens y biblioteca |
| Tiene **su propio shell y sus propios roles** (`COMMERCIAL_PARTNER`, `PARTNER_MANAGER`) | Misma regla que separa el backoffice financiero del de sistemas (D-2): un partner nunca ve el backoffice |
| *Validar vale* es la contraparte de `AP-VAL-03` | El doble canje se rechaza del lado del comercio, con firma |
| Umbral de **50 personas por segmento** en *Audiencia* | Por debajo, cruzar filtros es identificación (decisión 7 de la maqueta) |

---

## 6 · Las piezas que la maqueta fija, con su mundo

Complementa [[20 Maqueta de referencia · deltas del frontend]] §2 diciendo **dónde se
implementa cada una**. `F1-M` construye la columna Flutter; `F1-W`, la Angular. «Ambos»
significa el mismo nombre en los dos paquetes y **revisión lado a lado** en el punto de
sincronización.

| Componente | Nivel | Flutter (`diseno_flutter`) | Angular (`@aportaya/ui`) | Dónde se usa |
| --- | :-: | :-: | :-: | --- |
| `SelectorSegmentado` | átomo | ✔ | ✔ | Lista/calendario · QR/código · vistas equivalentes. **Relleno de marca en el elegido** |
| `BarraDePuntos` | átomo | ✔ | — | Tour |
| `CodigoQR` | átomo | ✔ | ✔ | Depósito, invitación, vale (rotativo), fondeo |
| `CuentaEnmascarada` | átomo | ✔ | ✔ | Retiro, cuentas, desembolsos |
| `Fecha` (zona horaria visible) | átomo | ✔ | ✔ | Todo expediente y toda tabla |
| `BarraDePasos` | molécula | ✔ | ✔ | Alta de ocho pasos · asistentes del backoffice |
| `FilaDeCotejo` | molécula | ✔ | ✔ | Cotejo del alta · expediente de verificación |
| `ChipsDeFiltro` (varias líneas, ícono por chip, sin «otros») | molécula | ✔ | ✔ | Movimientos, aportes, bandejas |
| `ResumenDePeriodo` | molécula | ✔ | ✔ | Extracto, aportes, libro mayor |
| `FilaDeMovimiento` (tipada, saldo corrido) | molécula | ✔ | ✔ | Extracto · billetera del backoffice |
| `EstadoVacio` (por qué y qué hacer; **vacío por filtro ≠ vacío por permiso**) | molécula | ✔ | ✔ | Toda lista |
| `TarjetaDeRacha` | molécula | ✔ | — | Portada |
| `RielDeTurnos` | molécula | ✔ | ✔ | Portada · detalle de grupo · backoffice de grupos |
| `RelojDePlazo` (plazo guardado, de qué norma sale) | molécula | ✔ | ✔ | Solicitud, mora, reclamo, bandejas con plazo |
| `OpcionConCosto` | molécula | ✔ | — | *No voy a poder pagar* |
| `EscaleraDeEtapas` | molécula | ✔ | ✔ | Cobranza, en la app y en el backoffice |
| `MedidorDeRango` | molécula | ✔ | ✔ | Mercado de turnos · riesgo de permutas |
| `DesgloseDeCobro` | molécula | ✔ | ✔ | Cobro del turno · entregas del backoffice |
| `MarcoDeCamara` | organismo | ✔ (`apps/movil`) | — | Documento y selfie, sobre el puerto `Camara` |
| `BannerDePauta` (sin cierre, formato único) | organismo | ✔ | — | Portada |
| `SeccionDeExpediente` | organismo | — | ✔ | Verificación, incumplimientos, disputas, reclamos |
| `PanelBienvenida` | organismo | ✔ | — | Portada de cuenta nueva |
| `CalendarioDeCuotas` | organismo | ✔ | — | *Mis aportes* |
| `ListaDeRequisitos` | organismo | ✔ | ✔ | Organizador (app) · habilitaciones (backoffice) |
| `TarjetaDeSolicitud` | organismo | ✔ | ✔ | Cola del organizador · solicitudes escaladas |
| `Vale` (muesca, QR rotativo, estado, condiciones) | organismo | ✔ | ✔ | Mis vales · campañas y vales · portal partner |
| `TarjetaDeOferta` | organismo | ✔ | ✔ | Ofertas del grupo · riesgo de permutas |
| `PanelDeFactores` | organismo | ✔ | ✔ | Veredicto de riesgo, en la app y en el backoffice |
| `PanelSorteo` / `VerificadorDeSorteo` | organismo | ✔ | ✔ | Sorteo en la app · verificación pública en el sitio |
| `BandaDeProposito` («Para qué sirve») | organismo | — | ✔ | Toda pantalla del backoffice |
| `NotificacionEmergente` (con cola) | organismo | ✔ | — | Shell móvil |
| `EstadoDePantalla` | organismo | ✔ | ✔ | **Toda** pantalla con datos; es el único que pinta los cuatro estados |

Y las cuatro reglas de estilo que la maqueta fijó y que valen en los dos mundos:

1. **El ícono dice qué pasó**, no si el número sube o baja.
2. **Los movimientos se agrupan por día**, con el neto del día y el saldo corrido.
3. **Un estado elegido no depende de que dos tokens de fondo sean distintos**: lleva
   relleno de marca.
4. **Un color de estado en una superficie chica lleva relleno y borde**, y la leyenda se
   pinta con las mismas reglas que la pieza.

---

## 7 · Cómo se verifica que «quedó como la maqueta»

Entra al gate de **todo** carril de pantallas, además de los seis comunes:

- [ ] Cada pantalla del carril está en este mapa, con su ruta en el mundo que corresponde;
      si la maqueta la tiene y este mapa no, se agrega acá **antes** de escribirla
- [ ] Cada pantalla se abrió en la maqueta en los **dos escenarios** y el informe lista, por
      pantalla, qué se reprodujo igual, qué se justificó distinto y qué se corrigió en la maqueta
- [ ] Golden (Flutter) o captura (Angular) de cada pantalla **al lado de la captura de la
      maqueta**, en claro y en oscuro, adjunta al PR
- [ ] Los organismos de §6 que la pantalla usa existen en el paquete de su mundo con ese
      nombre; ninguno se reimplementó en el directorio del carril
- [ ] Los tres escenarios de red de la maqueta corren contra Prism con `Prefer: example=`
      (`ok`, `intermitente`, `rechazo`) y la pantalla los muestra como la maqueta:
      reintento con la **misma** clave, error **traducido**, `202` como pendiente
- [ ] Ninguna cifra de la maqueta terminó cableada: umbrales, topes, comisiones y
      plazos llegan por contrato o por catálogo

## Ver también

[[20 Maqueta de referencia · deltas del frontend]] · [[AportaYa-Maqueta]] · [[README|Maqueta de Crecimiento Financiero y Alianzas]] · [[Flujo de pantallas · app del participante]] · [[Flujo de pantallas · backoffice administrador]] · [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · [[12 Fases F2 a F5 · App móvil]] · [[13 Fases F6 a F8 · Backoffice]] · [[16 Carriles de frontend]] · [[ADR-044 Frontend en Angular y Flutter]] · `disenar-frontend`
