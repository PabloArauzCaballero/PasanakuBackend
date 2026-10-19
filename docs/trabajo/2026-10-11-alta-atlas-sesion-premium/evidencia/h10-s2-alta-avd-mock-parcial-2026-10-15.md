# H10.S2 — intento parcial del alta en AVD con API simulada

Fecha: 2026-10-15 (America/La_Paz). AportaYa solamente. Samsung_A06-L, Android 13/API 33, AVD limitado a 768 MiB; APK debug ya compilada antes de iniciar este intento; mock Prism local en `10.0.2.2:4010`. No se usó backend remoto ni teléfono físico.

## Resultado observado

- Desde instalación/datos limpios se recorrieron portada, las cuatro láminas del tour y el formulario de cuenta. La validación de contraseña detuvo el primer intento; al corregir los dos campos de prueba, el formulario avanzó a confirmación de correo.
- La confirmación usó el fixture de Prism, no un email ni OTP real. El mock respondió como verificado y la app avanzó a fase 2, «Tu identidad».
- La fase mostró las cinco capturas requeridas: frente, dorso, cara de frente, perfil izquierdo y perfil derecho. Se detuvo ahí: no se fabricaron rostros ni se saltó la cámara/prueba de vida.
- No se completó el alta, no se emitió ni utilizó token sintético, y no se accedió a backend real. Los dumps temporales de UI se dejaron fuera del repositorio porque contenían datos de prueba; este registro omite esos valores.

## Alcance y estado

Esto confirma parcialmente el orden de pantallas del alta con un API simulado; no satisface H10.S2.M2 (alta completa con capturas, liveness, contrato, carga y estado), ni H10.S2.M6 (selectores «Expedido en» y «Actividad económica» con teclado y tres filas visibles). Esos hitos permanecen bloqueados/pendientes según sus DoD; no se rebaja el alcance ni se cuenta el mock como backend real.

El AVD y el proceso Prism iniciados para la prueba se cerraron. RAM final medida: **11,22/15,32 GiB usados**. Docker no se inició. No hubo cambios de producto derivados de este recorrido.
