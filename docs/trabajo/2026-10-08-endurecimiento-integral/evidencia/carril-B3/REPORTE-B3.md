# Carril B3 · reporte (2026-10-10)

Decisiones, abiertos y a quien consultar: `DECISIONES-Y-ABIERTOS.md`. Logs: `completas-1.log` (rojo corregido), `completas-2.log` (verde, RC 0,
incluye spotlessApply y generarClientes), `conteos-1.txt` (desde XML), `gates.log`, `regresion-total.log`, `generar-compose.log`.

Conteos (XML, fallos 0, errores 0, omitidas 0): grupos test 53 / web 82 / integ 206 / barrido 2; aportes 46/36/88/2; entregas 23/54/107/2;
garantia 25/43/149/2; organizador 5/35/105/2.

Rojo ajeno (no es de este carril): verificar_criterios 14 fallas (CU-08, 62, 63, 64 y ocho CU-68 de upstream; ninguna de B3);
verificar_seguridad: secreto en `nucleo-financiero/.../PactoConEntregasContratoTest.java:37` y aleatoriedad en `apps/backoffice/e2e`; verificar_boveda: "Ver tambien"
CU-13 y cifra en planes/17; frontend: simulado 1 (inversiones/listarCorridasDeInversiones), backoffice 2 en matriz-de-roles + specs de gateway/sesion.
