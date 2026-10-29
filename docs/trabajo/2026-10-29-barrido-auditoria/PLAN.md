# Plan — dejar en verde el barrido de reglas propias de `servicios/auditoria` (`auditoria:testBarrido`)

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend (servicio `auditoria`, rama de trabajo sobre `dev`) · Predecesor: `2026-10-29-slo-disponibilidad-servicios` (de donde viene la deuda) y `2026-10-25-bandeja-de-admisiones` (a quien esta deuda le corta el CI)
- Resultado observable: `auditoria:testBarrido` pasa en el CI y el paso «Formato, reglas propias y compilacion» deja de cortar antes de `grupos:webTest`.
- Kill-test: las dos pruebas de `servicios/auditoria/src/test/java/bo/aportaya/auditoria/BarridoTest.java` fallan hoy en `dev` (CI run 40878468838, paso 3): `sin-umbral-literal` por 3 líneas y `tamano-archivo` por 1 archivo.

## Alcance
- IN: `SaludDeServicios.java`, `DisponibilidadDelServicio.java`, `IndicadoresController.java` (+ un archivo nuevo en `web/` con lo que salga de él) y el único test que usa el constructor de conveniencia que se retira (`SaludDeServiciosWebTest`, línea 121, sin tocar aserciones).
- OUT: cualquier cambio de comportamiento, de contrato OpenAPI o de rutas; el valor del SLO (99,5 %, decidido por Pablo el 2026-10-29: no se toca); el resto de reds de `dev` (bóveda, f5, `AdmisionDelGrupoSustitucionTest`); `grupos`.
- Hechos (con ruta, ver `plataforma/comun-pruebas/.../barrido/SinUmbralLiteral.java`): la regla marca (a) constantes con nombre `UMBRAL|LIMITE|TOPE|TARIFA|COMISION|MONTO|MINIMO|MAXIMO|PORCENTAJE|TASA… = <dígito>` y (b) `new BigDecimal(<dígito>` / `BigDecimal.valueOf(<dígito>`, en `src/main` solamente. Líneas marcadas: `SaludDeServicios.java:43` (`new BigDecimal("99.5")`), `SaludDeServicios.java:61` (`BigDecimal.valueOf(100)`), `DisponibilidadDelServicio.java:18` (`CIEN = BigDecimal.valueOf(100)`). `IndicadoresController.java` tiene 379 líneas; el límite bloqueante es 300.
- El SLO ya vive en configuración: `servicios/auditoria/src/main/resources/application.yml:36` (`objetivo-slo: ${SALUD_OBJETIVO_SLO:99.5}`). El `99.5` del código es un segundo valor por omisión que duplica el del yml.
- Decisiones tomadas (a confirmar con Pablo):
  1. **99,5 %**: se retira la constante `OBJETIVO_POR_OMISION` y el `:99.5` del `@Value`; la única fuente es `application.yml`. Sin la propiedad, el servicio no arranca (regla 90.3.4), en vez de arrancar con una cifra escondida en el código. Consecuencia: el constructor de conveniencia `Salida(consultadoEn, servicios)` solo lo usaba un test, y se retira (el test pasa el objetivo explícito).
  2. **100**: es la base del porcentaje (una unidad, no un umbral regulatorio). Se expresa como `BigDecimal.TEN.pow(2)` (mismo valor, misma escala 0). La validación `(0, 100)` del objetivo, que estaba duplicada en dos clases, pasa a un solo método en `DisponibilidadDelServicio`. Declarado: esto satisface la forma de la regla; la regla no distingue «unidad» de «umbral» y no se le agrega una excepción aquí.
  3. **Controlador**: el bloque del catálogo de datos (6 operaciones, ~150 líneas) pasa a `CatalogoDeDatosController`, clase abstracta de paquete que implementa `IndicadoresApi`, y `IndicadoresController` la extiende. Es el mismo patrón que `grupos/web/RutasDeAdmision → RutasDeSolicitudes → …`. Rutas, permisos y cuerpos no cambian.
- Limitación de verificación: Gradle/Maven no corren en este entorno (descargas bloqueadas, ver `2026-10-25-checks-diseno-flutter/REPORTE.md`). Se reproduce la regla con un script que replica las dos reglas del barrido sobre `servicios/auditoria`; la verificación real es la del CI de la rama.

## H1 — `auditoria:testBarrido` pasa
**CA:** Dado `dev` con este cambio, cuando corre `BarridoTest` de `auditoria`, entonces `sin-umbral-literal` y `tamano-archivo` pasan, y las pruebas existentes de salud, disponibilidad e indicadores siguen verdes.
**DoD:** réplica local del barrido sin hallazgos · `spotlessCheck` equivalente en el CI · `auditoria:test` y `auditoria:webTest` verdes en el CI de la rama.
**Estado:** A MEDIAS (código escrito y réplica del barrido en verde; falta el CI real)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M1 | Retirar el `99.5` del código de `SaludDeServicios` (constante y valor por omisión) y el constructor de conveniencia; ajustar la línea 121 del web test | no queda `99.5` en código de `src/main/java` de `auditoria` (solo un ejemplo en un javadoc) | `grep -rn '99\.5' servicios/auditoria/src/main/java` + réplica del barrido (`evidencia/01-replica-del-barrido.txt`) | HECHO (contra la réplica; el CI aún no corrió) |
| H1.M2 | Retirar los `BigDecimal.valueOf(100)` (`CIEN` y la validación del constructor) con una sola validación del objetivo | la réplica del barrido no marca ni `DisponibilidadDelServicio` ni `SaludDeServicios` | réplica del barrido (`evidencia/01-replica-del-barrido.txt`) | HECHO (contra la réplica; el CI aún no corrió) |
| H1.M3 | Mover el bloque del catálogo de datos de `IndicadoresController` a `CatalogoDeDatosController` | `IndicadoresController` < 300 líneas, ningún archivo nuevo ≥ 300, mismas anotaciones por método | `wc -l` + réplica del barrido + comparación de las firmas y `@Permiso` antes/después (`evidencia/01-replica-del-barrido.txt`) | HECHO (contra la réplica; el CI aún no corrió) |
| H1.M4 | Entrega: push, CI de la rama y estado del PR | `auditoria:testBarrido` pasa en el CI | `actions_list` sobre el run de la rama | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Una propiedad faltante en algún perfil de arranque (`SALUD_OBJETIVO_SLO` sin valor) | el servicio no arranca | el yml trae `:99.5` como valor por omisión de la variable de entorno, así que el arranque normal no cambia; las pruebas de contexto cargan el mismo yml |
| `CatalogoDeDatosController` de paquete no se ve en un escaneo de Spring/Security | rutas del catálogo sin `@Permiso` aplicado | mismo patrón ya en producción en `grupos`; las pruebas web de catálogo y `AutorizacionNegativaTest` lo ejercen en el CI |
| `spotless` (palantir, 120 col.) reformatea | job rojo | se formatea a mano a 120 columnas y se corrigen los diffs que imprima el CI |
| El CI sigue sin llegar a `grupos` por otro rojo nuevo | PR #266 no se desbloquea | se lista en el REPORTE, sin esconderlo |
