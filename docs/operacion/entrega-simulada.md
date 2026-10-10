---
tags:
  - operacion
  - entrega
  - pruebas
titulo: "Entrega simulada reproducible"
estado: vigente
fecha: 2026-10-10
---

# Entrega simulada reproducible

> Un recorrido que cualquiera repite desde un clon limpio, con **fixtures propios** y **sin ningún
> proveedor real**. Sirve para demostrar qué anda hoy y, sobre todo, **qué no**.

Responde a H14.S1.M5. La regresión contractual y visual que lo acompaña (H14.S1.M4) corre dentro del
mismo recorrido: contratos → clientes → simulado → tipos/lint/pruebas → capturas del **mismo commit**.

## Entorno soportado

| Pieza | Versión | Para qué |
|---|---|---|
| Windows 11 o Linux, Git Bash | — | los scripts son `bash` |
| Node | ≥ 22 | frontend (Angular), simulado, Playwright |
| Yarn | 4.18 (`packageManager`) | `yarn install --immutable` desde el lockfile |
| Python | ≥ 3.10 | gates (`verificar_*`) |
| Java | 21 (Temurin) | solo para `generarClientes` y el backend |
| Docker Desktop | 4 GB asignados | solo para el backend, **por tandas** |
| Chromium de Playwright | el que instala `npx playwright install chromium` | aceptación y capturas |

Con 4 GB de Docker el stack completo **no** entra: se levanta en tandas de 4 servicios (ver
`despliegue/ensayos/arranque_completo.sh`, `D3_MODO=tandas`).

## Cómo se corre

```bash
git clone <repositorio> aportaya && cd aportaya
npx playwright install chromium            # una vez por máquina
bash scripts/entrega_simulada.sh frontend  # un paso a la vez, bajo el guardián de RAM
```

Cada paso pesado pasa por el guardián de RAM de la máquina (`HEAVY_SH`), de a uno. Sin guardián corre
directo y avisa. La evidencia queda en `evidencia/entrega-simulada/<fecha-hora>/`: un log por paso, las
capturas por viewport (móvil 360, tablet 768, escritorio 1280) y tema (claro/oscuro) y `RESUMEN.md`.

| Paso | Qué comprueba | Falla si… |
|---|---|---|
| `instalar` | `yarn install --immutable` | el lockfile no coincide con los `package.json` |
| `tokens`, `contenido-sitio` | artefactos generados que ni el compilador ni el CI traen | falta uno |
| `simulado-build` | contratos OpenAPI → JSON, ejemplos validados, mock | un contrato cambió y un ejemplo ya no valida |
| `simulado-*` | tipos, lint y pruebas de contrato del simulado | un ejemplo no cumple su esquema; un QR interoperable se afirma |
| `rutas-vs-contratos` | cada ruta que llama el frontend existe en un OpenAPI | aparece una ruta sin contrato que no está declarada |
| `backoffice-*`, `web-*` | tipos, lint, gate de la casa, pruebas y accesibilidad (axe) | cualquiera |
| `aceptacion-mock` | dos usuarios (A en el sitio, B en el backoffice) contra el doble con estado, **axe estricto** y capturas | una violación de accesibilidad, un desborde horizontal o un paso funcional |

El backend se levanta aparte, por quien opera el entorno:

```bash
bash scripts/entrega_simulada.sh backend   # arranque por tandas + humo por el gateway; baja todo al terminar
```

## Qué datos usa

- **Cuentas**: las genera el doble en el momento (`POST /__doble/sembrar`); teléfono, contraseña y factor
  salen de un contador, no del azar, y no están en el repositorio. En el backend, `sembrar_usuarios.sh` las
  escribe **fuera** del clon.
- **Contratos y ejemplos**: los de `servicios/*/openapi` y `packages/simulado/ejemplos`. Todo importe,
  tarifa, tasa o feriado es **sintético** (`apto_produccion=false`); nada sale de producción.
- **Proveedores**: `herramientas/proveedor_simulado` y `herramientas/aliado_simulado`. Ninguno real.

## Límites declarados

1. La parte web es **`VERIFIED_FUNCTIONAL_ONLY`**: corre contra un doble con estado y contra Prism, no
   contra los servicios reales. No prueba integración.
2. La aceptación en modo `real` **no corre**: falta implementar `sembrarReal`
   (`apps/backoffice/e2e/aceptacion/apoyo/siembra-real.ts`). Hace falta, sobre el stack, registrar a los
   usuarios, habilitar a un organizador y crear un grupo; hoy no hay contrato de siembra.
3. El retiro con doble aprobación solo se ejercita contra el doble: `nucleo-financiero` no publica un
   listado ni un alta de prueba de órdenes en revisión.
4. El mercado de turnos **no tiene pantalla** en web ni backoffice (es de la persona participante; no hay
   un endpoint de supervisión). Existe solo en `apps/movil`.
5. `apps/movil` (Flutter) no entra: sus pruebas (`flutter test`) necesitan el SDK y no están en el guardián.
6. No hay lector de pantalla real. Accesibilidad = axe-core (WCAG 2.0/2.1/2.2 A y AA + mejores prácticas)
   + recorrido por teclado + capturas inspeccionadas a mano.
7. Hay **19 rutas** que el frontend llama y ningún OpenAPI declara (listados de erp y publicidad, bandeja
   de reclamos, cola de solicitudes escaladas, refresco de sesión, exportaciones). Están nombradas, con su
   motivo, en `scripts/verificar_rutas_frontend.py`. Que el contrato no las publique no dice qué responde el
   backend real: hay que verificarlo en la corrida contra el stack.
8. `pagarFacturaDeProveedor` (erp) recibe un cuerpo **sin** `autorizadoPor`, que el contrato exige: contra
   el backend real es un 400 hasta que erp derive ese dato de la sesión. Divergencia fijada por prueba en
   `apps/backoffice/src/app/nucleo/entrada-estricta.spec.ts`.
9. Un enlace de invitación ya consumido o revocado se ve igual que uno vigente en el sitio: el sitio no
   tiene sesión y ninguna operación responde sin ella. La pantalla lo declara.

## Cómo se actualiza esto

Cuando un límite se cierra (por ejemplo, `sembrarReal` existe) se saca de la lista **en el mismo cambio**
que lo cierra y el paso correspondiente entra a la tabla. Una limitación que desaparece de acá sin
evidencia es un error de este documento.
