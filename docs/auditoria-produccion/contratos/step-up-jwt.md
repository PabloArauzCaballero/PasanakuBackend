# Contrato — evidencia step-up MFA (JWT) entre `identidad` y `nucleo-financiero`

- Fijado el 2026-09-21 en el reparto `PasanakuPromptManager/repartos/2026-09-21/PromptNoche`.
- Emite: `identidad` (`EmisorDeEvidencia`, carril PR1 · Richard). Valida: `nucleo-financiero` (`SegundoFactorStepUp`, carril PR2 · Justin) con el JWKS público de `identidad`, **sin llamar a `identidad` y sin leer su base** (invariante 11). Exige `iss`/`aud`: el decodificador común de `comun-web` (carril PR3 · Leo).
- **No se modifica durante el turno.** Si un carril necesita cambiarlo, lo anota en su daily §6 y se decide al cierre.

## Claims

| Claim | Valor | Quién lo exige |
|---|---|---|
| `alg` / `kid` | `RS256`; `kid` de la misma `RSAKey` que firma el token de acceso, publicada en `/.well-known/jwks.json` | `nucleo-financiero` (JWKS) |
| `iss` | `aportaya-identidad` | `comun-web` y `nucleo-financiero` |
| `aud` | `["aportaya-nucleo-financiero"]` (el token de acceso lleva `["aportaya"]`) | `nucleo-financiero` |
| `sub` | UUID del usuario; debe coincidir con el usuario de la sesión que pide el retiro | `nucleo-financiero` |
| `jti` | UUID, **un solo uso**: se consume con `INSERT … ON CONFLICT DO NOTHING` en `nucleo_financiero.evidencia_mfa_consumida (jti PK, usuario_id, proposito, consumida_en)` dentro de la transacción del CU-11 | `nucleo-financiero` |
| `iat` / `exp` | `exp − iat ≤ PT5M` (`aportaya.mfa.vigencia-evidencia`, configuración, nunca literal); tolerancia de reloj `aportaya.jwt.tolerancia` (default `PT60S`) | ambos |
| `proposito` | `RETIRO` \| `CAMBIO_CUENTA` \| `ADMIN`; el retiro exige `RETIRO` | `nucleo-financiero` |
| `desafio_id` | UUID del desafío verificado (`identidad.token_verificacion.id`, `proposito='MFA_RETIRO'`) | trazabilidad |
| `acr` | `mfa` | `nucleo-financiero` |
| `amr` | `["totp"]` \| `["otp"]` \| `["biometria"]` | informativo |

## Cómo se obtiene

1. `POST /sesiones/desafios` `{ proposito: "RETIRO" }` + `Idempotency-Key` → `201 { desafioId, expiraEn }`.
2. `POST /sesiones/desafios/{desafioId}/verificacion` `{ factor: FactorPresentado }` → `200 { evidencia, expiraEn, jti }`. El desafío queda consumido: un segundo intento da `409`.
3. `POST /billetera/retiros` con `evidenciaMfa: <evidencia>` (el campo `factorMfa` se acepta hasta 2026-12-31 por compatibilidad).

## Rechazos que el validador debe producir (y que el doble de prueba debe cubrir)

| Caso | Nivel | Resultado |
|---|---|---|
| Todos los claims correctos, `exp` futura, `jti` nuevo | correcto | acepta → `mfa_verificado = true` |
| `exp` = ahora + 1 s | límite | acepta |
| `exp` = ahora − 1 s (más allá de la tolerancia) | límite | `MFA_INVALIDO` |
| Mismo `jti` presentado dos veces | límite | la segunda → `MFA_INVALIDO` |
| Sin evidencia | inválido | `MFA_REQUERIDO` |
| Firmada con otra clave / `kid` desconocido | inválido | `MFA_INVALIDO` |
| `sub` distinto del usuario de la sesión | inválido | `MFA_INVALIDO` |
| `proposito` ≠ `RETIRO` | inválido | `MFA_INVALIDO` |
| `aud` sin `aportaya-nucleo-financiero` | inválido | `MFA_INVALIDO` |
| Sin `acr = mfa` | inválido | `MFA_INVALIDO` |

## Ejemplo (claves de prueba, nunca reales)

```json
{
  "iss": "aportaya-identidad",
  "aud": ["aportaya-nucleo-financiero"],
  "sub": "00000000-0000-0000-0000-00000000a001",
  "jti": "6f1c2c1e-2a7b-4c1d-9d3e-1b2c3d4e5f60",
  "iat": 1790000000,
  "exp": 1790000300,
  "proposito": "RETIRO",
  "desafio_id": "0d3f9a10-5b6c-4e7d-8f90-1a2b3c4d5e6f",
  "acr": "mfa",
  "amr": ["totp"]
}
```
