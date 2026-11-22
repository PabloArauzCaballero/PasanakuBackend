---
tags:
  - moc
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
modulo: "11 — Tarifas, Comisiones, Impuestos y Facturación"
relaciones_fk: 68
---

# 11 — Tarifas, Comisiones, Impuestos y Facturación · relaciones

Las **68 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[aplicacion_promocion.campana_id → campana_promocional]] | [[campana_promocional]] | — | no |
| [[aplicacion_promocion.devengo_id → devengo_comision]] | [[devengo_comision]] | — | no |
| [[asignacion_tarifario.autorizado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[asignacion_tarifario.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[asignacion_tarifario.segmento_id → segmento_comercial]] | [[segmento_comercial]] | — | sí |
| [[asignacion_tarifario.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[asignacion_tarifario.usuario_id → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[calculo_impuesto.devengo_id → devengo_comision]] | [[devengo_comision]] | — | no |
| [[calculo_impuesto.impuesto_id → impuesto]] | [[impuesto]] | — | no |
| [[cambio_tarifario.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[cambio_tarifario.tarifario_anterior_id → tarifario]] | [[tarifario]] | — | no |
| [[cambio_tarifario.tarifario_nuevo_id → tarifario]] | [[tarifario]] | — | no |
| [[campana_promocional.aprobada_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[cargo_comision.deduccion_entrega_id → deduccion_entrega]] | [[deduccion_entrega]] | ↗ 04 | sí |
| [[cargo_comision.devengo_id → devengo_comision]] | [[devengo_comision]] | — | no |
| [[cargo_comision.obligacion_id → obligacion_aporte]] | [[obligacion_aporte]] | ↗ 03 | sí |
| [[cargo_comision.transaccion_id → transaccion_billetera]] | [[transaccion_billetera]] | ↗ 10 | sí |
| [[concepto_tarifa.cuenta_ingreso_id → cuenta_contable]] | [[cuenta_contable]] | ↗ 03 | sí |
| [[concepto_tarifa.hecho_generador_id → catalogo_hecho_generador]] | [[catalogo_hecho_generador]] | — | no |
| [[concepto_tarifa.politica_redondeo_id → politica_redondeo]] | [[politica_redondeo]] | — | sí |
| [[concepto_tarifa.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[costo_proveedor_operacion.liquidacion_ingresos_id → liquidacion_ingresos]] | [[liquidacion_ingresos]] | — | sí |
| [[costo_proveedor_operacion.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | ↗ 03 | no |
| [[costo_proveedor_operacion.transaccion_id → transaccion_billetera]] | [[transaccion_billetera]] | ↗ 10 | sí |
| [[cotizacion_comision.concepto_tarifa_id → concepto_tarifa]] | [[concepto_tarifa]] | — | no |
| [[cotizacion_comision.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[cuenta_por_cobrar_comision.devengo_id → devengo_comision]] | [[devengo_comision]] | — | no |
| [[cuenta_por_cobrar_comision.gestion_cobranza_id → gestion_cobranza]] | [[gestion_cobranza]] | ↗ 08 | sí |
| [[cuenta_por_cobrar_comision.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[datos_facturacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[devengo_comision.asiento_contable_id → asiento_contable]] | [[asiento_contable]] | ↗ 03 | sí |
| [[devengo_comision.concepto_tarifa_id → concepto_tarifa]] | [[concepto_tarifa]] | — | no |
| [[devengo_comision.cotizacion_id → cotizacion_comision]] | [[cotizacion_comision]] | — | sí |
| [[devengo_comision.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[devengo_comision.participante_id → participante]] | [[participante]] | ↗ 02 | sí |
| [[devengo_comision.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[devengo_comision.usuario_obligado_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[devolucion_comision.autorizada_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[devolucion_comision.devengo_id → devengo_comision]] | [[devengo_comision]] | — | no |
| [[devolucion_comision.reclamo_id → reclamo_cliente]] | [[reclamo_cliente]] | ↗ 12 | sí |
| [[devolucion_comision.transaccion_id → transaccion_billetera]] | [[transaccion_billetera]] | ↗ 10 | sí |
| [[evento_significativo_sin.registrado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[exencion_comision.autorizada_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[exencion_comision.concepto_tarifa_id → concepto_tarifa]] | [[concepto_tarifa]] | — | sí |
| [[exencion_comision.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[exencion_comision.segmento_id → segmento_comercial]] | [[segmento_comercial]] | — | sí |
| [[exencion_comision.usuario_id → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[factura_electronica.datos_facturacion_id → datos_facturacion]] | [[datos_facturacion]] | — | no |
| [[factura_electronica.devengo_id → devengo_comision]] | [[devengo_comision]] | — | sí |
| [[factura_electronica.evento_significativo_id → evento_significativo_sin]] | [[evento_significativo_sin]] | — | sí |
| [[factura_electronica.lote_envio_sin_id → lote_envio_sin]] | [[lote_envio_sin]] | — | sí |
| [[factura_electronica.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[impuesto.cuenta_contable_id → cuenta_contable]] | [[cuenta_contable]] | ↗ 03 | sí |
| [[liquidacion_ingresos.asiento_contable_id → asiento_contable]] | [[asiento_contable]] | ↗ 03 | sí |
| [[liquidacion_ingresos.cerrada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[nota_credito_debito.devolucion_comision_id → devolucion_comision]] | [[devolucion_comision]] | — | sí |
| [[nota_credito_debito.factura_id → factura_electronica]] | [[factura_electronica]] | — | no |
| [[parametro_congelado_grupo.parametro_id → parametro_financiero]] | [[parametro_financiero]] | — | no |
| [[parametro_congelado_grupo.version_parametro_id → version_parametro]] | [[version_parametro]] | — | no |
| [[regla_tarifa.concepto_tarifa_id → concepto_tarifa]] | [[concepto_tarifa]] | — | no |
| [[simulacion_tarifa.ejecutada_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[simulacion_tarifa.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[tarifa_congelada_grupo.acuerdo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[tarifa_congelada_grupo.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[tarifa_congelada_grupo.tarifario_id → tarifario]] | [[tarifario]] | — | no |
| [[tarifario.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[tarifario.tarifario_anterior_id → tarifario]] | [[tarifario]] | — | sí |
| [[version_parametro.parametro_id → parametro_financiero]] | [[parametro_financiero]] | — | no |
