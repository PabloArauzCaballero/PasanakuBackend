# Plan — Carril Q (brechas de dinero abiertas de los carriles G e I)

- Fecha: 2026-10-11 · Repos: PasanakuBackend (rama justin/feature/cierre-endurecimiento-2026-10-08) · Predecesor: carriles G, I, O2, C
- Resultado observable: alguien abre las billeteras USUARIO y GRUPO; el cierre de billetera ve la deuda abierta; un aporte PAGADO sin saldo se compensa; la venta de un turno paga PRECIO - CARGOS al vendedor y CARGOS a la plataforma, con linea explicita.
- Kill-test: GET /billetera/cuentas/propia de una persona recien registrada sigue dando 422 AP-CU13-01.
- Alcance IN: servicios/nucleo-financiero, servicios/aportes, contratos/pactos/nucleo-financiero-entregas.json y el aditivo minimo `cargos` en entregas.yaml/MercadoWeb (necesario para el pacto). OUT: grupos, identidad (P), erp (R), apps/movil (T), origin/test, VPS.
- Decisiones de negocio: delegadas por el dueno (2026-10-11, «lo mas recomendado, pero cerralo»). Ver INFORME.md.

## H1 — Apertura de billeteras USUARIO/GRUPO
CA: Dado un usuario_registrado / grupo_creado, cuando nucleo lo consume (una o N veces, en cualquier orden), entonces existe exactamente una billetera BOB del titular y el aporte posterior puede aplicarse.
DoD: tests CU01/CU20 de nucleo en verde (test+integrationTest), spotless, verificadores.
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H1.M1 | AperturaDeBilleteras + repositorio (idempotente por evento y por titular) | CUApertura*Test | TODO |
| H1.M2 | Consumidores Kafka usuario_registrado / grupo_creado, interruptores y compose | ConsumidorDeAperturasKafkaTest + generar_compose | TODO |
| H1.M3 | Cadena completa: abrir ambas + aporte_cobrado aplica y cuadra desde el mayor | CUAperturaYAporteTest | TODO |

## H2 — HechosPorHttp por usuarioId
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H2.M1 | Resolver usuario -> participantes (grupos /grupos/participaciones con el token del titular) -> estado en aportes | HechosPorHttpTest (deuda abierta rechaza cierre, sin deuda cierra, caida = que si) | TODO |

## H3 — Aporte PAGADO sin saldo se compensa
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H3.M1 | nucleo: saldo insuficiente = rechazo definitivo, publica aporte_no_aplicado, marca consumido | CUConsumoDeAportes*Test | TODO |
| H3.M2 | aportes: consumidor de aporte_no_aplicado, reversa idempotente del pago y de la obligacion | CU21CompensaTest | TODO |

## H4 — Venta de turno: PRECIO - CARGOS (delegacion del dueno)
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H4.M1 | nucleo pagarAlTercero: retenido = precio; vendedor +(precio-cargos), plataforma +cargos, cargos desde entregas | CU13MercadoTest + pacto | TODO |
| H4.M2 | entregas publica `cargos` en OfertaParaPago (aditivo) y pacto actualizado | EntregasPublicaLaOfertaTest, CU130OfertaParaPagoTest | TODO |

## H5 — Documentar lo que no se inventa
| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H5.M1 | Canales != BILLETERA_MOVIL y limites CU-40: estado, dueno; README/CU/INFORME | revision | TODO |
