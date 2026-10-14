# H8.S4.M4 — inventario de capturas oscuras

Estado actual: BLOQUEADO solo por la unidad conceptual «grupos/detalle», que no tiene pantalla, ruta ni contrato local. El historial de auditoría empezó el 12 de octubre de 2026 y se actualizó el 13.

El alcance está en `PLAN.md`, sección H8: portada, tour, ingreso, MFA, seis pantallas del alta, cámara, prueba de vida, subida, estado de verificación, bienvenida, candado, billetera/inicio, grupos (mi estado y detalle), perfil y ayuda.

| Parte del alcance | Evidencia automatizada localizada | Falta |
|---|---|---|
| Candado | Revisión oscura registrada en `h1-s5-revision-visual.md`: 360, 412 y escritorio; sin recorte y con texto legible | Resolver las baselines H1 fallidas; verificar otros estados/tamaños del alcance |
| Billetera/inicio | `pantalla_de_saldo_golden_test.dart`; `h8-break-ui.md`: saldo cero oscuro al 200 % y feed de 40 movimientos claro/oscuro al 200 % revisados | Revisar baseline actual de saldo, otros estados del alcance y pantalla con navegación global |
| Cámara | Escáner QR: [permiso denegado revisado en claro y oscuro, móvil/tableta/escritorio](./h8-camara-denegada-visual.md), seis capturas inspeccionadas. Identidad: [permiso denegado y bloqueado en oscuro](./h8-camara-identidad-permiso-oscuro.md), dos capturas sintéticas 360 dp/130 % inspeccionadas, sin iniciar cámara ni tomar foto. Para identidad concedida, [tres capturas oscuras H10 de `PantallaDeCamara`](./h10-visual/INDICE.md), 360/412/600 dp/130 %, ya inspeccionadas; el visor es un `ColoredBox` sintético. Estas referencias acreditan legibilidad/distribución de UI, no sensor, preview real ni permiso del SO. | Otros estados/tamaños de cámara y validación de cámara/permisos en dispositivo; la simulación no demuestra permisos ni ajustes del sistema reales |
| Portada | [Capturas clara y oscura 360 × 740 al 130 % revisadas](./h8-s4-m4-portada-mfa-oscuro.md) | Otros tamaños/estados y dispositivo |
| Tour | [Cuatro láminas oscuras 360 × 740 al 130 % revisadas](./h8-s4-m4-tour-oscuro.md) | Otros tamaños/tema claro y recorrido en dispositivo |
| MFA | [Estado inicial y código sintético rechazado en oscuro 360 × 740 al 130 %](./h8-s4-m4-portada-mfa-oscuro.md); error `AP-CU04-04` revisado | OTP válido/navegación, teclado del sistema, otros tamaños, tema claro y dispositivo |
| Ingreso | [Vacío y rechazo sintético de credenciales en oscuro 360 × 740 al 130 %](./h8-s4-m4-ingreso-oscuro.md); prueba sin red y haptics reales | Teclado del sistema, otros errores/tamaños y prueba en dispositivo |
| Bienvenida | [Captura oscura 360 × 740 al 130 % revisada](./h8-s4-m4-bienvenida-oscuro.md) | Otros tamaños/tema claro, bono real y dispositivo |
| Perfil | [Captura oscura 360 × 740 al 130 % revisada](./h8-s4-m4-perfil-oscuro.md) | Claro, valores largos, teclado, otros tamaños y dispositivo |
| Prueba de vida | [Pose frontal oscura 360 × 740 al 130 % revisada](./h8-prueba-vida-oscura-frente-360-130.png); test con cámara simulada | Otras poses/estados y tamaños; verificar cámara física/permisos en dispositivo |
| Subida del expediente | [Primera cara en curso y cuatro pendientes, más fallo de red con reintento; oscuro 360 × 740 al 130 %, y error a 320 × 640/200 %](./h8-subida-oscura-360-130.md); proveedores/capturas sintéticos; golden 2/2, widget 2/2 y unidad 10/10 | Otros tamaños/temas/estados, navegación global y dispositivo/backend |
| Grupos — mi estado | [Carga, error/reintento, deuda/restricción y al día/sin restricción en oscuro 360 × 740 al 130 %](./h8-grupos-mi-estado-oscuro.md); 4/4 estados cubiertos con providers falsos; dos PNG deuda son duplicados exactos | Claro, detalle, navegación global, otros tamaños y backend |
| Centro de ayuda | [Inicio y lista desplazada en oscuro 360 × 740 al 130 %](./h8-ayuda-oscura.md) | Claro, vacío/error, búsqueda sin resultados, teclado, otros tamaños y recorrido en dispositivo |
| Estado de verificación | [Pendiente, en revisión, aprobada y rechazada en oscuro 360 × 740 al 130 %](./h8-verificacion-oscura.md) | Carga/error, claro, otros tamaños y navegación/dispositivo |
| Cobertura visual complementaria H10 | [Matriz H10 e índice de celdas](./h10-visual/INDICE.md) y [lectura individual](./h10-visual/LEIDO.md): 20 de las 21 unidades conceptuales, 228/228 celdas existentes inspeccionadas; incluye capturas oscuras en anchos de 360/412/600 dp y referencias H8 reutilizadas | Grupos/detalle no tiene ruta/pantalla ni contrato local para producir una captura. Resolver su alcance con producto/arquitectura antes de atribuirle UI inexistente; H8.S4.M4 sigue parcial por esta unidad fuera de ruta |

La matriz H10 amplía la cobertura visual oscura a 20 unidades conceptuales existentes; sus 228 celdas se anotaron una a una en `h10-visual/LEIDO.md`. Esta evidencia sintética respalda legibilidad/layout y no demuestra hardware, permisos del SO, backend ni navegación integrada. No contar las capturas deuda/inicio y deuda/acciones de grupos como estados distintos: son idénticas. La única unidad conceptual sin pantalla/ruta local es grupos/detalle; no se inventa una captura.

## Revalidación de cobertura y bloqueo — 2026-10-13

### Auditoría complementaria del contrato local de grupos — 2026-10-13

Se verificó `servicios/grupos/src/main/resources/openapi/grupos.yaml`: existe `GET /grupos/participaciones` (`listarMisParticipaciones`), pero cada elemento `Participacion` solo contiene `grupoId`, `participanteId` y `estado`. `/grupos` publica únicamente `POST crearGrupo`; no existe `GET /grupos/{grupoId}` ni una operación equivalente de detalle. La lectura de participaciones sirve para conocer en qué grupos está la persona, pero no define ni entrega el contenido de una pantalla de detalle. H8.S4.M4 sigue bloqueado por alcance/contrato de producto, no por ausencia absoluta de lecturas de grupos.

Se ejecutó en serie, sin `--update-goldens`, el conjunto H8 oscuro disponible:

```text
flutter test --no-pub --concurrency=1 --timeout 60s --reporter compact test/goldens/h8_alta_oscura_test.dart test/goldens/h8_bienvenida_oscuro_test.dart test/goldens/h8_camara_identidad_permiso_oscuro_test.dart test/goldens/h8_centro_ayuda_oscuro_test.dart test/goldens/h8_grupos_mi_estado_oscuro_test.dart test/goldens/h8_ingreso_oscuro_test.dart test/goldens/h8_perfil_oscuro_test.dart test/goldens/h8_portada_mfa_oscuro_test.dart test/goldens/h8_prueba_vida_oscura_test.dart test/goldens/h8_subida_oscura_test.dart test/goldens/h8_tour_oscuro_test.dart test/goldens/h8_verificacion_oscura_test.dart
Resultado: All tests passed; 21/21 PASS.
```

El lote cubre las capturas oscuras dedicadas de alta, bienvenida, permiso de cámara, ayuda, estado de grupo, ingreso, perfil, portada/MFA, prueba de vida, subida, tour y verificación. La matriz H10 cubre además cámara concedida, candado y billetera, dentro de las 20 unidades implementadas; `LEIDO.md` registra 228/228 celdas inspeccionadas. En el código vigente no existe ruta/pantalla de detalle de grupo y falta un contrato de lectura para producir datos reales; el inventario de rutas declara explícitamente ese hueco.

**Resultado:** las 20 superficies existentes tienen evidencia visual oscura revisada. La unidad 19/21 («grupos — detalle») continúa en el alcance escrito pero no existe en el producto local. No añadir una UI/fixture inventada para simularla: resolver con producto/arquitectura si se implementa con contrato real o se retira formalmente del alcance. **H8.S4.M4 = BLOQUEADO** por decisión/dependencia externa; dueño: producto + arquitectura de grupos. Capturas sintéticas no se presentan como prueba de sensores/permisos reales.
