---
name: kyc-onboarding
description: "Dar de alta y conocer al cliente en AportaYa: identidad, verificación documental, niveles de debida diligencia, PEP y beneficiario final, MFA y dispositivos, contrato de adhesión y revisión periódica. Úsala al implementar registro, verificación, elevación de nivel o cierre de cuenta, y cuando un límite dependa del nivel del usuario."
---

# Alta y conocimiento del cliente

El alta define **cuánto puede operar** el usuario por el resto de su vida en la
plataforma. Todo el módulo de límites cuelga de acá: un alta apurada es un límite
mal puesto, y un límite mal puesto es un incumplimiento.

## El circuito

```
usuario → documento_identidad → verificacion_kyc → debida_diligencia
                                         ↓
              calificacion_riesgo_cliente ← matriz_riesgo_lft ← factor_riesgo_evaluado
                                         ↓
                        limite_operativo_billetera (por nivel)
```

Más `credencial_acceso`, `factor_mfa`, `dispositivo`, `sesion` (acceso);
`contrato_adhesion` + `aceptacion_contrato` (lo pactado); `perfil_transaccional`
(lo esperado); `expediente_cliente` (todo junto, para una inspección).

## Niveles: qué habilita cada uno

| Nivel | Qué se verificó | Qué habilita |
| --- | --- | --- |
| `SIMPLIFICADA` | Identidad básica | Montos bajos, sin transferencia entre personas |
| `ESTANDAR` | Documento verificado, prueba de vida, perfil declarado | Operación normal según el catálogo de límites |
| `REFORZADA` | Lo anterior más origen de fondos, **segunda revisión independiente** y aprobación superior | Montos altos, y es obligatoria para PEP y perfiles de riesgo |

Los montos de cada nivel **no están en el código**: viven en
`limite_operativo_billetera` con vigencia (skill `semillas-catalogos`). Si falta la
fila, se deniega (`R-LIM-01`).

## Reglas duras del alta

1. **Sin verificación no hay operación.** Una billetera creada y no verificada
   existe, pero no mueve dinero. Denegar por omisión, no permitir por omisión.
2. **La verificación se guarda con su evidencia y su fecha**, no como un booleano.
   "Verificado = true" no responde quién, cuándo ni contra qué.
3. **Un documento vencido invalida el nivel.** El vencimiento se controla solo, no
   se espera a que alguien lo mire.
4. **PEP implica reforzada, siempre**, y arrastra al `beneficiario_final`. La
   condición se declara (`declaracion_pep`) y se verifica contra listas.
5. **El perfil transaccional se declara al inicio** y es la referencia contra la
   que después se mide el desvío (`desvio_perfil`). Sin perfil declarado, el
   monitoreo no tiene contra qué comparar.
6. **La revisión periódica vence** (`revision_periodica_kyc`). Un cliente
   reforzado sin revisión al día es un hallazgo, y el sistema lo muestra como tal.

## Elevar de nivel

Es un flujo, no un campo editable: se piden los documentos que faltan, se
verifican, se registra quién aprobó, y **el límite nuevo rige desde la aprobación**
—no retroactivamente. Bajar de nivel también existe (documento vencido, alerta,
resultado de investigación) y se registra igual.

## Autenticación y dispositivos

| Regla | Por qué |
| --- | --- |
| MFA obligatorio a partir del monto que fije la política | `politica_billetera.requiere_mfa_desde`, dato, no constante |
| Dispositivo nuevo ⇒ verificación adicional | El robo de credencial es el vector más común |
| Los intentos fallidos se cuentan y bloquean (`bloqueo_cuenta`) | Y el bloqueo tiene camino de salida documentado |
| Cambio de credencial revoca sesiones | Todas, no solo la actual |
| La sesión y el dispositivo quedan en cada transacción | Es lo que responde "¿desde dónde se hizo esto?" |

## Contrato de adhesión

El usuario acepta **una versión concreta**, y esa versión se guarda
(`aceptacion_contrato`). Después:

- El usuario puede recuperar **el texto que aceptó**, no el vigente hoy.
- Un contrato nuevo exige aceptación nueva; no se aplica en silencio.
- El tarifario aceptado se congela para sus grupos en curso
  (skill `facturacion-sin`).

## Baja y cierre

`solicitud_baja` / `solicitud_cierre_billetera`. Reglas:

- **No se cierra con saldo sin devolver** ni con obligaciones abiertas en un grupo.
- El cierre **no borra** el expediente: la conservación regulatoria lo impide
  (skill `seguridad-sesion-rls`).
- El derecho a la supresión de datos se atiende por CU-07, y se resuelve
  anonimizando lo que la ley permite, no borrando la evidencia financiera.

## Checklist

- [ ] Ningún monto de límite escrito en el código.
- [ ] Sin verificación vigente, la operación se deniega —probado.
- [ ] La verificación guarda evidencia, actor y fecha.
- [ ] PEP fuerza reforzada y segunda revisión independiente (`R-UIF-10`), probado.
- [ ] El vencimiento de documento y de revisión periódica se detecta solo.
- [ ] El usuario puede recuperar la versión del contrato que aceptó.
- [ ] El cierre con saldo o con obligaciones abiertas se rechaza.

## Ver también

`cumplimiento-uif` · `seguridad-sesion-rls` · `semillas-catalogos` ·
`reclamos-consumidor` · CU-01 a CU-07, CU-16 · familias `R-LIM` y `R-UIF`
