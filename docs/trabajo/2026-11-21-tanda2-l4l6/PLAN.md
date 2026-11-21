# Plan — Tanda 2, carril L4L6: identidad, cumplimiento y frontends compartidos

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` · Base: `origin/dev` 50cf3be9
- Predecesor: `docs/trabajo/2026-11-21-f1-identidad-token-acceso/` (#412: actor del token, `PuedeAutenticarse`, TOTP real).
- Fuentes: `PLAN.md` y anexos `ANEXO-L4-identidad-cumplimiento.md` y `ANEXO-L6-frontends-compartidos.md` del plan de correcciones; `DECISIONES.md`.
- Resultado observable: una cuenta suspendida o dada de baja pierde todo acceso en 15 minutos como máximo, y un importe escrito en la app llega al servidor con el valor que la persona quiso escribir.
- Kill-test: (1) un token emitido a una cuenta que después se suspende sigue siendo aceptado por un decodificador más de 15 min después de emitido; (2) `CampoMonto.aCadenaDelContrato('12.50')` devuelve algo distinto de `12.50`.

## Alcance
- IN: `servicios/identidad/**`, `servicios/cumplimiento/**`, `apps/**`, `packages/**`, `clientes/**` (solo regenerados), `docs/entidades/*.puml`, `contratos/**` y `pactos/**` de identidad y cumplimiento, esta carpeta.
- OUT: `plataforma/**`, `.github/**`, `build.gradle*` raíz, `docs/Restricciones.md` salvo la sección propia, `docs/entidades` y `contratos` de otros servicios, `sql/` a mano. Lo que necesite esos archivos queda A MEDIAS con el cambio exacto descrito.
- Ambigüedades registradas:
  - AMB-T2-1: «un token vigente deja de servir en ≤ 15 min». Supuesto: se cumple con (a) revocar sesiones al suspender, (b) negar el refresco y (c) que vida del token + tolerancia de reloj del decodificador ≤ 15 min. Revocación inmediata en operaciones de dinero es F1.L4.M15 (toca `plataforma/comun-web`). Confirmar con Pablo.
  - AMB-T2-2: `CampoMonto` con un solo punto seguido de 3 dígitos («1.240»). Supuesto: miles (`1240.00`, formato es-BO), mostrado al vuelo para que la persona lo vea antes de enviar. El anexo admite esa salida.
  - AMB-T2-3: textos nuevos (P-16) los redacta Claude y los aprueba Pablo; quedan listados en el REPORTE.

## H1 — F1 de L4 y L6
**CA:** Dado cualquier hallazgo F1 de L4 y L6 relocalizado en `dev`, cuando se corrige, entonces su CA del anexo se cumple y su prueba dirigida pasa.
**DoD:** prueba dirigida por microtarea con salida en `evidencia/`, suites del módulo tocado en verde, CI del PR en verde.
**Estado:** EN CURSO

### H1.S1 — F1.S3.M1: el acceso de una cuenta suspendida termina
**CA:** Dada una cuenta suspendida, cuando pasa el tiempo de vida del token, entonces ningún decodificador con la configuración por omisión lo acepta después de 15 min de emitido; su sesión está revocada y no renueva.
**DoD:** prueba de unidad sobre el token emitido + prueba con `NimbusJwtDecoder` real que valida `exp` con su tolerancia por omisión.
**Estado:** HECHO para el tope de 15 min; la revocación inmediata en dinero queda en F1.L4.M15 (ver REPORTE, A medias)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| T2.M1 | Vida del token de acceso acotada para que vida + tolerancia del decodificador ≤ 15 min | Un token de identidad decodificado con el decodificador por omisión de Spring es rechazado al minuto 15 + 1 s | `./gradlew :servicios:identidad:test --tests '*VidaDelAcceso*'` verde | HECHO (`evidencia/t2-m1-token-vigente.txt`) |

### H1.S2 — F1.S4.M1 / F1.L6.M03: `CampoMonto` interpreta sin ambigüedad
**CA:** «12.50», «12,50» y «12.5» → `12.50`; «1.250,50» → `1250.50`; «1.240.50», «1,250.50» y «-5» → rechazo con error en el campo; nunca viaja un importe 100 veces mayor.
**DoD:** vectores compartidos en `packages/tokens/vectores/monto-escrito.json`, referencia TS con prueba (vitest), Dart y Angular reproducen los mismos vectores.
**Estado:** HECHO (peldaño TESTED; sin prueba en dispositivo ni capturas, ver REPORTE)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| T2.M2 | Referencia `interpretarMontoEscrito` (TS) + vectores | Los vectores pasan | `yarn vitest run` en `packages/tokens` | HECHO |
| T2.M3 | Angular `aCadenaDelContrato` delega en la referencia | Spec de `packages/ui` pasa con los vectores | `yarn ng test` en `packages/ui` | HECHO |
| T2.M4 | Dart `CampoMonto.aCadenaDelContrato` reproduce los vectores; el campo muestra el importe interpretado y el error asociado | Prueba Dart con los vectores | `flutter test` (SDK 3.50.1 en el scratchpad) | HECHO |

### H1.S3 — Resto de F1 de L4 (en orden del anexo, relocalizando cada uno en `dev`)
| ID | Microtarea | Estado |
|---|---|---|
| F1.L4.M03 | Arranque fail-closed de secretos | TODO |
| F1.L4.M05 | Diligencia PEP con cuatro ojos reales | TODO |
| F1.L4.M06 | Caso LFT analista ≠ revisor, analista del token | TODO |
| F1.L4.M07 | Segregación del reporte UIF sin hueco por NULL | TODO |
| F1.L4.M12 | OTP de correo: reintento sobre código consumido | TODO |
| F1.L4.M18 | `VerificarTitularidad` contra KYC aprobado vigente | TODO |
| F1.L4.M25 | `GET /licencia/alcance` no escribe ni acepta usuario del query | TODO |
| F1.L4.M26 | Controles regulatorios con hechos del servidor | TODO |
| Resto (M04, M08–M11, M13–M17, M19–M24) | Ver anexo; se abren en este orden después de los anteriores | TODO |

### H1.S4 — Resto de F1 de L6
| ID | Microtarea | Estado |
|---|---|---|
| F1.L6.M04 | Patrón de importe de `cumplimiento.yaml` válido y sin negativos | TODO |
| F1.L6.M05 a M12 | Ver anexo L6 | TODO |

## H2 — F2 de L4 y L6
| ID | Microtarea | Estado |
|---|---|---|
| F2.L4.M01 | Retirar la doble aprobación humana de retiros (parte identidad/cumplimiento/apps) | TODO |
| F2.L4.M02 | Retirar el organizador profesional de identidad y backoffice de cumplimiento | TODO |
| F2.L6.M13 | Shell del backoffice sin aprobaciones eliminables | TODO |
| F2.L6.M14 | Mock, clientes y catálogo regenerados sin respaldo ni aprobaciones | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin SDK de Flutter en el contenedor | Las pruebas Dart no corren acá | Vectores compartidos probados en TS; Dart declarado NO COMPROBADO con comando listo |
| Sin Docker | Pruebas `BaseDePrueba` no corren | Postgres local en 5433; lo de Testcontainers queda con comando para la máquina de Pablo |
| Cambios que requieren `plataforma/**` | No se pueden cerrar en este carril | A MEDIAS con el cambio exacto en REPORTE y PR |
