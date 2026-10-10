# Carril O2 · Ambiguedades y decisiones abiertas

Cada fila es una pregunta que el sistema NO resuelve por su cuenta: el supuesto implementado esta ahi para poder seguir,
no como hecho. Ninguna cifra, fecha ni norma de esta lista se dio por verificada.

## Calendario y feriados (H7.S1.M3)

| ID | Pregunta | Supuesto implementado | Confirmar con |
|---|---|---|---|
| O2-C1 | ¿Quien escribe `catalogo.dia_no_habil`? El esquema `catalogo` es de SOLO LECTURA para todo servicio (ADR-029) pero CU-59 describe una carga desde backoffice con fuente (`CU59CargarCalendario` sigue sin implementar) | El calendario entra por semilla; `FeriadoConProcedencia` valida fuente/alcance/fecha pero no persiste | Arquitectura (ADR-029) + Operaciones |
| O2-C2 | Procedencia de los feriados 2026-2028 ya sembrados (`seeders/minimos/12-calendario-habil.json`): citan D.S./Ley en la descripcion pero **no hay copia de la Gaceta Oficial ni de la norma** en el repo; las fechas moviles se derivan de la Pascua | No se modifican ni se amplian; se tratan como "con cita, sin verificar". Antes de produccion hay que contrastarlos | Legal / Operaciones (Gaceta Oficial del Estado Plurinacional) |
| O2-C3 | ¿El sabado es dia habil? `PlazoHabil` ya trataba sabado y domingo como no habiles; el corrimiento de CU-59 (`correrSiCae`) NO lo hacia (corria un viernes feriado a sabado) y se corrigio para alinearlo | Sabado y domingo no habiles en TODO el calculo | Legal (ASFI plazos en dias habiles) + Producto |
| O2-C4 | Feriados DEPARTAMENTALES: la tabla no tiene departamento y la consulta de CU-59 no puede decir a quien aplican | Aportes ignora los departamentales; CU-59 los trata como un alcance mas. Nunca se aplican a quien no corresponde por inferencia | Producto + Operaciones |
| O2-C5 | Zona horaria del contrato: America/La_Paz (UTC-4, sin horario de verano) por omision; el endpoint de vencimiento acepta otra zona IANA | La fecha contractual es el dia calendario en la zona del contrato | Legal / Producto |
| O2-C6 | Fin de gracia al correr el vencimiento: conserva su largo (se corre los mismos dias) y si cae en no habil pasa al siguiente habil | Supuesto a favor de quien paga | Producto + Legal |
| O2-C7 | Un anio sin calendario cargado: se rechaza (CALENDARIO_VACIO / AP-CU21-09), nunca se cuenta corrido. Hoy 2029 en adelante no tiene calendario | Rechazo por omision | Operaciones: cargar cada anio con fuente antes de diciembre |

## Obligaciones y cobro (H8.S1.M1)

| ID | Pregunta | Supuesto | Confirmar con |
|---|---|---|---|
| O2-O1 | `grupos` todavia no expone el hecho "cupos ocupados, titulares, importe y fechas del periodo". Ruta propuesta `GET /grupos/periodos/{periodoId}/calendario-de-aportes` (esquema = `CalendarioDelPeriodo.Periodo`) | Adaptador por omision que deniega; pruebas con doble de tres niveles | Carril grupos |
| O2-O2 | ¿Quien dispara la generacion al abrir el periodo? Endpoint `POST /aportes/periodos/{id}/obligaciones` con `ADMIN_PLATAFORMA`; el disparo automatico (evento de grupos o trabajo) no esta cableado | Disparo manual/de operaciones | Arquitectura + grupos |
| O2-O3 | Importe de un cupo fraccionado: lo decide `grupos` (aportes no multiplica) | `montoEsperado` viene dado | Producto |

## Falta extraordinaria de caja (H8.S1.M6)

| ID | Pregunta | Supuesto | Confirmar con |
|---|---|---|---|
| O2-F1 | ¿Que contiene "contingencia" cuando falta caja? | Contencion automatica: deuda conservada, entrega en espera, **no reservar ciclos nuevos** mientras haya una viva, escalar a `TESORERIA` y `RESPONSABLE_RIESGOS` (nombres del catalogo de permisos, no personas) y comunicar al grupo con hechos. Se guarda como `plan_contingencia` (`FONDO_AGOTADO`/`EXTENSION_DE_PLAZO`/`APROBADO`) sin acuerdo del grupo | Direccion financiera + Producto |
| O2-F2 | Cobertura parcial cuando el respaldo no alcanza | Todo o nada (ya era A4) | Direccion financiera |
| O2-F3 | Reponer capacidad: quien decide y como se cierra una contingencia sin cobertura | Se cierra al cubrirse el faltante o cuando una consulta de cobertura confirma faltante cero. **No hay cierre manual**: si nadie vuelve a consultar, la contingencia queda viva y bloquea reservas nuevas (falla hacia el lado seguro) | Tesoreria |
| O2-F4 | Alternativas del grupo (prorrateo, reduccion de bolsa, disolucion) | Fuera de este carril; requieren acuerdo | Producto + Legal |
| O2-F5 | Texto y canal de la comunicacion al grupo | Texto de hechos, sin porcentajes ni fechas prometidas; no hay consumidor que lo envie todavia | Producto + notificaciones |

## Cesion (H9) — ver `H9-S1-M1-ficha-contractual-cesion.md` (L1 a L9, todas abiertas)

## Sorteo (hallazgo de seguridad MEDIO, cerrado)

Sin ambiguedad pendiente: la semilla ya no sale en la respuesta de comprometer.
