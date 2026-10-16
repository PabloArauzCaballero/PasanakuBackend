---
name: plan-por-fases
description: "Dividir un trabajo grande de AportaYa en fases con gate de entrada y salida, y llevar el informe de progreso. Úsala cuando el alcance toque varios módulos, infraestructura, seguridad o migraciones; cuando alguien pida 'hacé todo el backend'; o cuando haga falta reportar avance, riesgos, decisiones y desviaciones."
---

# Trabajar por fases

Regla principal: **no intentar todo en una sola ejecución** cuando el alcance abarca
varios módulos, infraestructura, seguridad, cambios de esquema o integración con los
clientes. Antes de modificar archivos se escribe el plan:

```
docs/implementation/plan-de-implementacion.md
```

Cada fase entrega algo **útil, revisable y cerrado**. Ninguna fase se declara
terminada con trabajo prometido para después.

## Cabecera obligatoria de cada fase

```
Fase actual: X de N
Fases completadas: X - 1
Fases restantes: N - X
Objetivo de esta fase: ...
Entradas verificadas: ...
Gate de entrada: aprobado | bloqueado
Gate de salida: pendiente | aprobado | rechazado
```

Si el gate de entrada está bloqueado, **no se empieza**: se dice qué falta.

## Fases base

Adaptar el número al alcance real, manteniendo las dependencias. Para un módulo de
la bóveda, lo habitual son cuatro o cinco; para el sistema completo, esta secuencia:

| # | Fase | Cierra cuando |
| --- | --- | --- |
| 1 | Descubrimiento: casos de uso, restricciones, contradicciones | Toda contradicción de la bóveda está resuelta o declarada |
| 2 | Arquitectura, ADRs y amenazas | Las decisiones caras están escritas (`decisiones-adr`) |
| 3 | Esquema, roles y catálogos | `sql/aplicar.sql` aplica en limpio y las semillas mínimas cargan |
| 4 | Contratos por caso de uso | Entrada, salida y errores existen antes del código |
| 5 | Persistencia: repositorios, bloqueos, transacciones | Pruebas de molécula contra Postgres real en verde |
| 6 | Casos de uso y reglas de negocio | Criterios de aceptación como pruebas |
| 7 | API, autenticación, permisos y OpenAPI | Pruebas negativas de permisos pasan |
| 8 | Worker, outbox e integraciones | Reintento, duplicado y fuera de orden probados |
| 9 | Observabilidad y rendimiento | Métricas, trazas y baseline medidos |
| 10 | Frontend: app y backoffice | Los cuatro estados en cada pantalla con datos |
| 11 | Despliegue, respaldo y restauración | Restore drill aprobado |
| 12 | Cierre: documentación, auditoría y empaquetado | Matriz de `definicion-de-terminado` sin gate crítico rojo |

Un alcance chico combina fases; uno grande las subdivide. Lo que no se hace es
saltarlas.

## Gate de salida de una fase

Una fase cierra **solo** cuando:

- [ ] El alcance declarado está completo, no parcial.
- [ ] Lint, tipos y build de lo tocado pasan.
- [ ] Las pruebas relevantes pasan, y se dice cuáles corrieron.
- [ ] La documentación de lo tocado quedó al día (bóveda incluida).
- [ ] Los riesgos pendientes están declarados por escrito.
- [ ] No hay fallos ocultos ni pruebas desactivadas sin justificación.

## Informe de progreso

Se actualiza al cerrar cada fase, en `docs/implementation/informe-de-progreso.md`:

```md
# Informe de progreso

## 1. Resumen del ciclo
## 2. Avance realizado          ← archivos creados/modificados, pruebas ejecutadas
## 3. Riesgos detectados        | Riesgo | Impacto | Mitigación |
## 4. Decisiones clave          | Decisión | Justificación | Impacto |
## 5. Desviaciones              | Desviación | Motivo | Acción |
## 6. Fase actual
## 7. Próxima fase recomendada
## 8. Estado del entregable     completo | parcial | bloqueado | pendiente de validación
```

Nada de "se avanzó en el backend". Avance concreto y verificable: qué caso de uso,
qué archivos, qué pruebas corrieron y con qué resultado.

## Cambios de alcance

Cuando aparece un requisito nuevo a mitad del trabajo:

1. Se evalúa el impacto sobre las fases pendientes.
2. Se actualiza `N` y se dice cuántas fases se agregaron o reordenaron.
3. **No se declara el proyecto terminado** con fases nuevas abiertas.

Si el requisito nuevo contradice la bóveda, el primer entregable es corregir la
bóveda (`caso-de-uso`, `restriccion`, `boveda-modelo`), no el código.

## Trabajo en paralelo

Solo cuando dos fases no comparten archivos críticos, cambios de esquema ni
contratos. Se documenta la dependencia y cómo se integran. Dos fases tocando el
mismo caso de uso no van en paralelo.

## Antipatrones

- Prometer que una prueba "se hará luego" y cerrar el gate igual.
- Entregar diez módulos a medias en vez de dos terminados.
- Cambiar de fase sin actualizar el informe.
- Empezar con el gate de entrada bloqueado "para no perder tiempo".

## Ver también

`definicion-de-terminado` · `decisiones-adr` · `implementar-desde-boveda` ·
`ci-calidad` · `documentacion-entregables` · `docs/Arquitectura/Método de arquitectura.md`
