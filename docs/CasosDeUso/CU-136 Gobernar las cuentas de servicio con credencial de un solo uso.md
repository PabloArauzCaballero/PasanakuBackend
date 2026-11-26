---
tags:
  - caso-uso
  - modulo/01-identidad-usuarios-y-seguridad
criticidad: alta
codigo: CU-136
actores: [Administrador de identidades internas, Responsable de seguridad, Sistema]
normas: [ASFI Seguridad de la Información, ASFI control interno, gestión de credenciales]
---

# CU-136 — Gobernar las cuentas de servicio con credencial de un solo uso

> **Objetivo.** Que las identidades con las que un servicio le habla a otro (`SERVICIO_*`)
> tengan dueño, vencimiento y rastro: quién las registró, cuándo vencen, cuándo se rotó su
> clave y quién las revocó. La clave nueva se entrega **una sola vez** y nunca se vuelve a ver.

> Decisión de Pablo P-10 (2026-11-24, GU-18): «sí, las cuentas de servicio se gobiernan desde
> el backoffice». **Alcance real de esta entrega:** gobierna cuentas ya aprovisionadas
> (registrar, rotar la clave, revocar, ver vencimientos). **Crear** el usuario de servicio desde
> cero queda abierto (ver *Decisiones requeridas*).

## Actores y disparador

- **Administrador de identidades internas** (`ACCESOS_ADMINISTRAR` o
  `USUARIOS_INTERNOS_GESTIONAR`): registra, rota y revoca. Lee con `USUARIOS_INTERNOS_LEER`.
- **Responsable de seguridad:** dispara la rotación ante una sospecha o por calendario.
- **Disparador:** alta de un servicio, vencimiento próximo, sospecha de filtración, baja del servicio.

## Precondiciones

1. Sesión de backoffice con segundo factor y una evidencia de step-up vigente por acto (se
   consume una sola vez).
2. Para registrar: el usuario ya existe y porta un rol `SERVICIO_*` vigente (se aprovisiona fuera
   del repositorio).
3. Nadie gobierna su propia cuenta.

## Flujo principal

1. El operador abre *Cuentas de servicio* y elige *Registrar*: la cuenta, un nombre, el
   vencimiento (dentro de la vigencia máxima configurada) y el motivo. Confirma con su segundo factor.
2. El sistema comprueba que la cuenta porta un rol `SERVICIO_*` vigente y no está registrada,
   la escribe `ACTIVA` y deja `identidad.cuenta_servicio_alta`.
3. Para **rotar**, el operador confirma con su segundo factor y el motivo. El sistema genera una
   credencial aleatoria de 32 bytes, guarda **solo su hash** (el anterior pasa al historial), cierra
   las sesiones vigentes de la cuenta, deja `identidad.cuenta_servicio_credencial_rotada` y devuelve el
   valor **en esa respuesta**, con `Cache-Control: no-store`. No se puede volver a pedir.
4. Para **revocar**, el sistema deja la cuenta `REVOCADA`, revoca sus asignaciones, vence su
   credencial, suspende al usuario, cierra sus sesiones y deja `identidad.cuenta_servicio_revocada`.
5. El listado muestra estado, vencimiento y última rotación; nunca la credencial ni su hash.

## Flujos alternativos

| Caso | Qué pasa |
| --- | --- |
| El usuario no porta un rol `SERVICIO_*` vigente | 422 `SIN_ROL_DE_SERVICIO`; no se registra |
| La cuenta ya está registrada | 422 `YA_REGISTRADA` |
| El vencimiento es pasado o supera la vigencia máxima | 422 `VENCIMIENTO_INVALIDO` con el tope |
| Rotar o revocar una cuenta revocada | 422 `CUENTA_REVOCADA`; una revocada no vuelve a activarse: se registra otra |
| Quien actúa es la propia cuenta | 422 `NADIE_SE_GOBIERNA_A_SI_MISMO` |
| Sin evidencia de step-up válida o ya usada | 422 `STEP_UP_REQUERIDO`; no se escribe nada |
| La cuenta no tiene credencial que rotar | 422 `SIN_CREDENCIAL` |

## Postcondiciones

Cada acto deja su evento con `actorId`, `actorRol`, `origen` y `motivo`; el valor de la
credencial no está en ninguna tabla, evento, log ni lectura posterior; una cuenta revocada no
puede abrir sesión ni conserva asignaciones vivas.

## Contrato · `openapi/identidad.yaml`

| Operación | Ruta | Exige |
| --- | --- | --- |
| `listarCuentasDeServicio` | `GET /cuentas-servicio` | `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_LEER` |
| `registrarCuentaDeServicio` | `POST /cuentas-servicio` | `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_GESTIONAR` + step-up |
| `rotarCredencialDeCuentaDeServicio` | `POST /cuentas-servicio/{cuentaId}/rotacion` | idem + step-up |
| `revocarCuentaDeServicio` | `POST /cuentas-servicio/{cuentaId}/revocacion` | idem + step-up |

```ts
const ERRORES_CU136 = {
  NOMBRE_INVALIDO:               'AP-CU136-01',
  MOTIVO_REQUERIDO:              'AP-CU136-02',
  VENCIMIENTO_INVALIDO:          'AP-CU136-03',
  STEP_UP_REQUERIDO:             'AP-CU136-04',
  SIN_ROL_DE_SERVICIO:           'AP-CU136-05',
  YA_REGISTRADA:                 'AP-CU136-06',
  CUENTA_INEXISTENTE:            'AP-CU136-07',
  CUENTA_REVOCADA:               'AP-CU136-08',
  NADIE_SE_GOBIERNA_A_SI_MISMO:  'AP-CU136-09',
  SIN_CREDENCIAL:                'AP-CU136-10',
} as const;
```

| Error | Cuándo se devuelve |
| --- | --- |
| `NOMBRE_INVALIDO` | Nombre vacío o de más de 80 caracteres |
| `MOTIVO_REQUERIDO` | Motivo vacío o de más de 160 caracteres |
| `VENCIMIENTO_INVALIDO` | Vencimiento pasado o más allá de la vigencia máxima |
| `STEP_UP_REQUERIDO` | Falta la evidencia de segundo factor, venció o ya se usó |
| `SIN_ROL_DE_SERVICIO` | El usuario no porta un rol `SERVICIO_*` vigente |
| `YA_REGISTRADA` | La cuenta ya está registrada |
| `CUENTA_INEXISTENTE` | La cuenta de servicio no existe |
| `CUENTA_REVOCADA` | La cuenta ya está revocada |
| `NADIE_SE_GOBIERNA_A_SI_MISMO` | Quien actúa es la propia cuenta |
| `SIN_CREDENCIAL` | La cuenta no tiene credencial que rotar |

### Contrato de eventos (bitácora) · forma v1

Tipos: `identidad.cuenta_servicio_alta`, `identidad.cuenta_servicio_credencial_rotada`,
`identidad.cuenta_servicio_revocada`. Carga: `version`, `actorId`, `actorRol`, `origen`
(`PANEL_ADMIN`), `motivo`, `ocurridoEn`, `rolCodigo` y los conteos del acto. **Nunca** la
credencial, su hash ni datos personales.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | credencial de 32 bytes aleatorios | Generada y hasheada con el puerto `HasheadorDeCredencial`; se descarta tras entregarse |
| Molécula | `CuentasDeServicioRepositorio` | Sentencias sobre `cuenta_servicio` |
| Organismo | `CU136GobernarCuentasDeServicio` | Transacción: step-up, reglas, escritura, evento |
| Página | `CuentasServicioController` y «Cuentas de servicio» del backoffice | Traduce y presenta |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `identidad.cuenta_servicio_alta` · `_credencial_rotada` · `_revocada` | Bitácora de auditoría | `ACCESOS_ADMINISTRAR` |

## Interfaz

- **Backoffice:** *Sistemas › Personal y accesos › Cuentas de servicio*: listado con vencimiento y
  última rotación; registrar, rotar (el valor se muestra una vez, con aviso) y revocar.

## Restricciones aplicables

`R-SEG-07` · `R-AUD-01`

## Evidencia que deja

[[cuenta_servicio]] · [[credencial_acceso]] · [[historial_credencial]] · [[asignacion_rol]] · [[sesion]] · [[bitacora_evento]] · `evento_dominio`

## Decisiones requeridas

- **A2 · vigencia máxima:** decidida por Pablo (2026-11-26): 90 días, configurable con `aportaya.cuentas-servicio.vigencia-maxima`.
- **A3 · segundo factor de la cuenta:** sin decisión. Supuesto vigente: el TOTP sigue el enrolamiento existente y todo acto exige step-up.
- **A5 · crear la cuenta desde cero:** sin decisión. Supuesto vigente: solo se registran cuentas ya aprovisionadas; cómo se identifica el usuario nuevo y cómo enrola su TOTP se decide antes de implementarlo.
- **Vencimiento en el login:** resuelto: una cuenta vencida o revocada no abre sesión (CU-04, flujo 1b).

## Criterios de aceptación

```gherkin
Dada una cuenta aprovisionada con un rol SERVICIO_* vigente
Cuando un operador con step-up la registra con nombre y vencimiento dentro del tope
Entonces la cuenta queda ACTIVA con quien la registro y deja identidad.cuenta_servicio_alta

Dada una cuenta de servicio registrada con una sesion abierta
Cuando un operador rota su credencial con step-up
Entonces recibe la credencial una sola vez, en la base queda solo su hash y la sesion se cierra

Dada una cuenta de servicio registrada
Cuando un operador la revoca con step-up
Entonces queda REVOCADA, sin asignaciones vivas, con la credencial vencida y el usuario suspendido

Dado un usuario que no porta un rol SERVICIO_*
Cuando un operador intenta registrarlo como cuenta de servicio
Entonces se rechaza con SIN_ROL_DE_SERVICIO

Dada una cuenta de servicio revocada
Cuando un operador intenta rotar su credencial
Entonces se rechaza con CUENTA_REVOCADA
```

## Ver también

[[CU-08 Asignar y revocar roles de operador]] · [[CU-135 Crear, editar y retirar roles personalizados con step-up y cuatro ojos]]
