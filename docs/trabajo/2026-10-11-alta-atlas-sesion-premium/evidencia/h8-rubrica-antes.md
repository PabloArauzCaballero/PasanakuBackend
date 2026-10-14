# H8.S1.M2 — revisión visual previa (rúbrica + anti-slop)

Fecha: 2026-10-13. Revisión de solo lectura; no se modificó UI ni se regeneraron baselines.

## Veredicto

**Auditoría completada; resultado de calidad: RECHAZADA para aprobación visual.** Se puntuaron las 20 unidades disponibles antes de corregir hallazgos. La fila de grupos/detalle (21.ª unidad conceptual del inventario) no tiene ruta ni contrato local y no se inventó una pantalla. Las puntuaciones no son una certificación AA: el contraste se juzgó visualmente; la verificación cuantitativa ejecutada cubre solo los pares de roles incluidos en los tests de tokens.

Escala por dimensión: 0 = falla clara; 1 = parcial; 2 = sólido. Orden: jerarquía (J), grilla/alineación (G), espaciado (E), sistema visual (S), contraste (C), densidad (D), estados (X), pulido (P). Para aprobar, J/S/C/X deben ser 2. El puntaje total es informativo, no compensa fallas en esos cuatro criterios.

| Superficie | Claro / oscuro (412 dp, 130 %) | J | G | E | S | C | D | X | P | Total / 16 | Hallazgo principal |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Portada | [claro](./h10-visual/h10-portada-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-portada-principal-oscuro-412-130.png) | 2 | 2 | 1 | 2 | 2 | 2 | 2 | 2 | 15 | Sin defecto bloqueante; revisar variación de tokens frente al golden histórico. |
| Tour | [claro](./h10-visual/h10-tour-lamina-1-claro-412-130.png) / [oscuro](./h10-visual/h10-tour-lamina-1-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 16 | Estados dinámicos no aplican al contenido estático; sin hallazgo grave. |
| Ingreso | [claro](./h10-visual/h10-ingreso-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-ingreso-principal-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 16 | CTA y formulario claros; estado cargando/error observado aparte. |
| MFA | [claro](./h10-visual/h10-mfa-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-mfa-principal-oscuro-412-130.png) | 2 | 2 | 1 | 2 | 1 | 0 | 1 | 1 | 10 | **Mayor:** pantalla casi vacía y sin destino del código, reenvío, ayuda ni salida visibles. |
| Alta — cuenta | [claro](./h10-visual/h10-alta-cuenta-vacia-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-cuenta-vacia-oscuro-412-130.png) | 2 | 2 | 1 | 2 | 1 | 1 | 1 | 1 | 11 | Ayuda/placeholder oscuro aparenta tenue; requiere medición por uso. |
| Alta — correo | [claro](./h10-visual/h10-alta-correo-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-correo-inicial-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 1 | 2 | 15 | Falta observar entrega/reenvío del OTP; no se infiere defecto de flujo desde esta captura. |
| Alta — documento | [claro](./h10-visual/h10-alta-documento-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-documento-inicial-oscuro-412-130.png) | 1 | 1 | 1 | 2 | 1 | 0 | 1 | 1 | 8 | **Mayor:** peek del carrusel corta el título siguiente; rótulos de caras muy apretados. |
| Alta — datos | [claro](./h10-visual/h10-alta-datos-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-datos-inicial-oscuro-412-130.png) | 2 | 2 | 1 | 2 | 1 | 2 | 1 | 2 | 13 | Ayuda/placeholder oscuro aparenta tenue; contraste por control sin medir. |
| Alta — actividad | [claro](./h10-visual/h10-alta-actividad-vacia-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-actividad-vacia-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 1 | 2 | 15 | Sin hallazgo mayor; selector cerrado no cubre estados internos. |
| Alta — contrato | [claro](./h10-visual/h10-alta-contrato-previo-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-contrato-previo-oscuro-412-130.png) | 2 | 2 | 1 | 2 | 1 | 0 | 1 | 1 | 10 | Observación menor de densidad en el paso puente; el contrato real aparece tras su CTA. |
| Cámara | [claro](./h10-visual/h10-camara-concedida-claro-412-130.png) / [oscuro](./h10-visual/h10-camara-concedida-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 1 | 0 | 1 | 12 | Estado de visor sintético; hardware/permisos/errores reales no cubiertos por la captura. |
| Prueba de vida | [claro](./h10-visual/h10-prueba-vida-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-prueba-vida-inicial-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 0 | 1 | 13 | Cámara sintética; error/rechazo/repetición en dispositivo real no cubiertos aquí. |
| Subida | [claro](./h10-visual/h10-subida-en-curso-claro-412-130.png) / [oscuro](./h10-visual/h10-subida-en-curso-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 1 | 2 | 1 | 14 | Estado de error legible; espacio libre amplio antes del CTA, observación menor. |
| Verificación | [claro](./h10-visual/h10-verificacion-pendiente-claro-412-130.png) / [oscuro](./h10-visual/h10-verificacion-pendiente-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 1 | 2 | 1 | 14 | Estado pendiente legible; variantes aprobada/revisión/rechazada están en matriz. |
| Bienvenida | [claro](./h10-visual/h10-bienvenida-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-bienvenida-principal-oscuro-412-130.png) | 1 | 2 | 2 | 2 | 2 | 1 | 2 | 1 | 13 | **Mayor:** gran tarjeta casi vacía y poco contenido para el espacio dominante. |
| Candado | [claro](./h10-visual/h10-candado-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-candado-inicial-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 1 | 0 | 1 | 12 | Solo se vio bloqueado inicial; desbloqueo/errores de dispositivo no quedan cubiertos. |
| Billetera | [claro](./h10-visual/h10-billetera-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-billetera-principal-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 1 | 15 | Buena jerarquía; skeleton de carga algo sutil (menor). |
| Grupos — mi estado | [claro](./h10-visual/h10-grupos-mi-estado-principal-deuda-claro-412-130.png) / [oscuro](./h10-visual/h10-grupos-mi-estado-principal-deuda-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 2 | 1 | 15 | Sin defecto mayor; [matriz de 4 estados](./h10-visual/INDICE.md#inventario-conceptual-del-alcance-del-plan). |
| Perfil | [claro](./h10-visual/h10-perfil-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-perfil-inicial-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 1 | 0 | 1 | 12 | La pantalla tiene acciones, no filas de ajustes; no se atribuye una ausencia de contenido inexistente al diseño. |
| Ayuda | [claro](./h10-visual/h10-ayuda-inicio-claro-412-130.png) / [oscuro](./h10-visual/h10-ayuda-inicio-oscuro-412-130.png) | 2 | 2 | 2 | 2 | 2 | 2 | 1 | 1 | 14 | Catálogo claro; apertura/navegación de artículo no cubierta por captura estática. |

## Hallazgos priorizados

| Severidad | Ubicación | Hallazgo | Corrección sugerida |
|---|---|---|---|
| Mayor | MFA, estado principal | Hay mucho espacio vacío; no se comunica a qué destino se envió el código ni se ven reenvío, ayuda o salida. | Añadir contexto de entrega parcialmente enmascarado, temporizador/reenvío y recuperación accesible; revisar la distribución vertical. |
| Mayor | Alta/documento, carrusel 412 dp | El título del elemento adyacente queda cortado y los rótulos de cara están muy juntos. | Reducir el peek o cambiar el layout para no cortar texto semántico; espaciar/redistribuir rótulos y comprobar 360/412/600 dp. |
| Menor / observación | Alta/contrato, paso puente previo | Hay espacio libre amplio, pero es una pantalla de entrada; su CTA lleva al contrato real, que debe leerse completo y requiere tres consentimientos antes de aceptar. No se clasifica como contrato ausente ni defecto del flujo. | No duplicar texto legal en el paso puente ni inventar contenido. Solo reconsiderar su composición si una prueba visual demuestra que el espacio interfiere con la acción. |
| Mayor | Bienvenida | La tarjeta vacía domina la composición; acciones y contenido quedan desproporcionados. | Reequilibrar la tarjeta con contenido útil ya disponible o reducir su tamaño; no añadir decoración genérica. |
| Moderado | Alta/cuenta y alta/datos, tema oscuro | Hint/helper parece de bajo contraste en la inspección visual; no se calculó ratio de cada instancia. | Medir pares reales de texto/campo en ambos temas y ajustar solo los roles que no cumplan el umbral aplicable. |
| Menor | Billetera, skeleton; subida | El skeleton de billetera se percibe tenue y hay espacio amplio previo al CTA en subida. | Revisar visibilidad del skeleton y ritmo vertical; conservar si la intención de carga deliberadamente discreta está documentada. |
| Cobertura | Cámara/prueba de vida/candado | Las capturas son fixtures sintéticos y no acreditan cámara, haptics, permisos o biometría físicos. | Mantener la limitación explícita y ejecutar comprobación en dispositivo al disponer de él. |

## Anti-slop

- Identidad y paleta se mantienen coherentes; se usan componentes y tokens existentes. No se observó una colección de tarjetas genéricas o decoración ajena al flujo.
- La jerarquía es clara en portada, ingreso, billetera, grupos y ayuda. Las oportunidades mayores están en MFA y bienvenida; el paso puente del contrato conserva una observación menor de espacio, no un defecto del contrato: el contenido legal vive en la siguiente pantalla.
- El carrusel de documentos privilegia el efecto de peek sobre la lectura del título vecino; corregir semántica/legibilidad antes que añadir ornamentación.
- No se propone llenar espacios por estética ni inventar ajustes inexistentes en Perfil. Las pantallas estáticas no reciben estados de carga/error artificiales.
- La revisión de contraste fue visual y conservadora. No se emite aprobación WCAG global basada en una prueba de roles limitada.

## Alcance, capturas y verificación

- La matriz H10 registra 228 celdas (20/21 unidades conceptuales), tres anchos lógicos (360, 412 y 600 dp), temas claro/oscuro, texto al 130 % y estados disponibles; índice y observaciones: [INDICE](./h10-visual/INDICE.md) y [LEIDO](./h10-visual/LEIDO.md). Para la puntuación se inspeccionaron las parejas 412 dp representativas por superficie; las variantes y anchos adicionales se contrastaron con el registro visual existente.
- Los agentes revisores abrieron las capturas 412 dp de sus lotes; la revisión adicional de los renders actuales `_testImage.png` solo estuvo disponible para parte de las celdas fallidas. La captura de referencia histórica no se trató como render vigente cuando había discrepancia.
- La matriz incluye fixtures sintéticos. El ancho 600 dp es el mayor probado; no hay ruta/configuración desktop nativa para estas pantallas. Desktop y dispositivo físico quedan **No cubiertos**.
- Prueba cuantitativa acotada: `node --test pruebas/roles-contraste.test.mjs pruebas/texto-de-ejemplo-contraste.test.mjs` (desde `packages/tokens`) → TAP: 3 tests, 3 pass, 0 fail, exit 0. Esto verifica los pares de roles contemplados, no cada combinación visible por pantalla.
- Comparación golden actual: `flutter test --no-pub --concurrency=1 --reporter expanded <20 archivos de test/goldens/h10-visual>` (desde `apps/movil`, sin `--update-goldens`) se detuvo para liberar memoria; salida al interrumpir: `01:21 +21 -201`, seis celdas restantes sin ejecutar. No se actualizaron masters. En los pares abiertos, las diferencias incluyen cambios sólidos de tokens/peso tipográfico (p. ej. CTA claro cobrizo con etiqueta blanca frente al master naranja con texto oscuro), por lo que no se atribuyen solo a antialiasing/rasterizador. Hace falta reconciliar diseño vigente y baselines tras revisión; no se declara golden suite PASS.
- Ningún dispositivo físico, backend real ni ruta de grupos/detalle fue parte de esta auditoría.
