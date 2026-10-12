> **SUPERADO por H17 (2026-10-12).** Lo que este archivo daba por «escrito sin verificar» se compiló, se formateó y se probó después: ver «Retomada 2026-10-12 (H17)» en [REPORTE.md](../../REPORTE.md) y `../carril-H17/`. Se conserva el texto original de abajo como estado del corte del 2026-10-11.

# Carril Q — ESTADO al cierre de sesion (2026-10-11)

Plan: `PLAN-CARRIL-Q.md`. Informe completo: `INFORME.md`. Decisiones de negocio: adoptadas por delegacion del dueno 2026-10-11 (ADR-051/052 de carril W: DD-1 precio - cargos, DD-8 billeteras).

## Cambios (archivos)
* nucleo-financiero: `aplicacion/AperturaDeBilleteras`, `consumo/{SobreDeEvento,ConsumidorDeUsuariosKafka,ConsumidorDeGruposKafka}`, `infraestructura/CuentaBilleteraRepositorio` (abrir, politicaVigente), `clientes/{HechosPorHttp,OfertasPorHttp}`, `aplicacion/{ConsumoDeAportes,CU13EjecutarConDestino,PagoDeCompraEnMercado}`, `dominio/puertos/OfertasDelMercado`, `application.yml`, `openapi/nucleo-financiero.yaml` (texto), README.
* aportes: `aplicacion/CU21CompensarAporte`, `consumo/ConsumidorDeAportesNoAplicadosKafka`, `PagoRepositorio.reversar`, `application.yml`, `openapi/aportes.yaml` (texto), README.
* entregas (aditivo minimo): `cargos` en `OfertaParaPago` (yaml), `OfertaParaElPago`, `GestionDeOfertas`, `MercadoWeb`, 2 pruebas.
* Contratos: `contratos/pactos/{aportes-nucleo-financiero.json (nuevo), nucleo-financiero-entregas.json (v2)}`; `scripts/generar_compose.py` + `docker-compose.coolify.yml`; `recorridos.py` (H9.S1.M5); ejemplo simulado; docs CU-13, CU-16, CU-21.
* Pruebas nuevas: nucleo `CUApertura{DeBilleteras,YAporte}Test`, `BaseDeApertura`, `CUAporteNoAplicadoTest`, `CU16CierreConDeudaTest`, `HechosPorHttpPorUsuarioTest`, `ConsumidorDeAperturasKafkaTest`; aportes `CU21CompensaTest`, `ConsumidorDeAportesNoAplicadosKafkaTest`. Adaptadas (sin debilitar): CU13MercadoTest, CUConsumoDeAportes{Rechazos,Concurrencia}Test, ContratosConOtros..., PactoConEntregas..., BaseDeInversiones.

## VERIFICADO
* javac main+test de nucleo y aportes: RC 0. 22 + 3 pruebas sin BD en verde (`javac-y-pruebas-sin-bd.txt`, script `q_javac.sh`).
* verificar_criterios/boveda/seguridad/pruebas_web/contratos_limites: RC 0 (incluye el renombre de las 2 pruebas «limite:» -> «cuadre: cargos iguales al precio...» y «rechaza compensar un pago que no existe...»).

## ESCRITO SIN VERIFICAR
* Todo lo que usa PostgreSQL (lista en INFORME «Ejecucion»); entregas (no compilado); precio - cargos contra la base real; spotless (formato palantir no aplicado); clientes OpenAPI regenerados (nucleo-financiero y entregas cambian su texto/esquema; ejecutar `generarClientes`); vitest de simulado.

## FALTA
1. Correr: `bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-Q/q_jj.sh --continue $(cat docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-Q/args_full.txt) :servicios:nucleo-financiero:generarClientes :servicios:aportes:generarClientes :servicios:entregas:generarClientes` (Docker + Postgres 5454; args en `scratchpad/q/args_full.txt`; es el mismo comando que quedo encolado y cancelado). Corregir lo que salga rojo y re-correr `verificar_*.py`.
2. Regenerar clientes y commitearlos; `npm run test:front` en packages/simulado (ejemplos vs esquema).
3. Dejar en rojo/verde conteos desde `build/test-results/*.xml`.
4. Fuera de mi alcance, desactualizado (carril P): CU-20 Supuesto A1 y `grupos.yaml crearGrupo` dicen que nucleo «todavia no» abre la cuenta del grupo; CU-01.
5. Recorrido real con stack (`recorridos.py h9m5`, aporte sin saldo, apertura) y creacion del topico (`crear-topicos.sh` lo deriva del codigo).

## Supuestos / decisiones
Billetera de persona ACTIVA/SIMPLIFICADA al registrarse (interruptor `APERTURA_ESTADO_INICIAL_USUARIO=EN_APERTURA`; sin senal de KYC aprobado nadie la activaria): confirman Cumplimiento y Producto. Solo BOB. Se consume `grupo_creado` (el `grupo_activado` del diseno no se emite). Solo AP-CU12-01 es rechazo definitivo/compensable; AP-CU12-02/03 siguen reintentandose. Venta de turno: comprador paga el precio, vendedor precio - cargos, plataforma cargos. Canales distintos de BILLETERA_MOVIL y limites CU-40: sin implementar (duenos: Contabilidad/Producto y Riesgos). Riesgo: `grupos` solo lista participaciones vigentes (deuda de quien ya salio no frena el cierre).
