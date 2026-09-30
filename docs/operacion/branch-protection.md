# Protección de ramas — `dev`, `test` y `main`

> H2.S5.M2 (carril PR5, Pablo). Nada de esto se aplicó todavía en este turno
> (AMB-R1/Q-05: `dev` no exige PR ni aprobaciones durante la noche del
> 2026-09-21, para que los cinco carriles mergeen solos con su gate local). El
> ruleset **mínimo** queda listo para que Pablo lo aplique con un comando
> (H2.S5.M3); el **completo** (este documento) se activa recién en la
> promoción `dev → main`.

## 1. Qué exige cada rama

| Regla | `dev` | `main` |
|---|---|---|
| PR obligatorio | Sí | Sí |
| Aprobaciones | 1 | 2 |
| Revisión de CODEOWNERS obligatoria | Sí | Sí |
| Descarta aprobaciones ante un push nuevo | Sí | Sí |
| Último push aprobado por otra persona | No | Sí |
| Hilos de revisión resueltos antes de mergear | Sí | Sí |
| Checks requeridos | los 11 de abajo (sin `e2e`, que solo corre en `main`) | los 12 (con `e2e`) |
| Rama al día con la base antes de mergear | Sí (`strict_required_status_checks_policy`) | Sí |
| Force-push | Bloqueado | Bloqueado |
| Borrado de la rama | Bloqueado | Bloqueado |
| Commits firmados | **No se exigen** (Q-06, decidido 2026-09-21) | No se exigen |

## 2. Checks requeridos — nombres EXACTOS

GitHub identifica un check por su `name:` de job (el título legible), no por el id
del job en el YAML. Verificado contra una corrida real de esta rama
(`gh api repos/PabloArauzCaballero/PasanakuBackend/commits/<sha>/check-runs --jq
'.check_runs[].name'`, 2026-09-21):

- `Formato, reglas propias y compilacion` (`ci.yml` → `codigo`)
- `La boveda y el esquema no divergen` (`ci.yml` → `boveda`)
- `Base efimera, semillas y permisos` (`ci.yml` → `base`)
- `Contratos y clientes generados` (`ci.yml` → `contratos`)
- `Frontend — Flutter y Angular (ADR-044)` (`ci.yml` → `frontend`)
- `Los cinco corredores` (`ci.yml` → `pruebas`)
- `bootJar y docker build, multietapa y sin root` (`ci.yml` → `imagenes`, incluye Trivy H2.S4)
- `Dependencias, secretos e imagen` (`ci.yml` → `seguridad`)
- `Dependencias vulnerables (OSV-Scanner) / osv-scan` (`ci.yml` → `dependencias`, workflow reusable)
- `Bóveda, seguridad, carriles y despliegue coherentes` (`boveda-y-esquema.yml`)
- `Esquema, semillas y prueba de humo sobre PostgreSQL real` (`boveda-y-esquema.yml`)
- Solo en `main`: `Punta a punta — solo en main` (`ci.yml` → `e2e`)

Si un job cambia de `name:`, este documento y los dos JSON de abajo quedan
desactualizados — no hay forma de que GitHub lo avise, así que es responsabilidad
de quien edite `ci.yml` revisar esta lista.

## 3. Los rulesets, como JSON

- Mínimo (aplicar ahora, un comando de Pablo): [`../entregables/ruleset-minimo.json`](../entregables/ruleset-minimo.json)
- `dev`, completo (aplicar en la promoción): [`../entregables/ruleset-dev.json`](../entregables/ruleset-dev.json)
- `main`, completo (aplicar en la promoción): [`../entregables/ruleset-main.json`](../entregables/ruleset-main.json)

Estructura verificada contra la documentación oficial de la API de rulesets de
GitHub (`POST /repos/{owner}/{repo}/rulesets`) el 2026-09-21.

## 4. Comandos

```bash
# Mínimo — el único que se aplica esta noche, y solo con confirmación explícita
# de Pablo (el agente no tiene permiso para tocar recursos compartidos del repo):
gh api --method POST repos/PabloArauzCaballero/PasanakuBackend/rulesets \
  --input docs/auditoria-produccion/entregables/ruleset-minimo.json

# En la promoción dev -> main (no antes):
gh api --method POST repos/PabloArauzCaballero/PasanakuBackend/rulesets \
  --input docs/auditoria-produccion/entregables/ruleset-dev.json
gh api --method POST repos/PabloArauzCaballero/PasanakuBackend/rulesets \
  --input docs/auditoria-produccion/entregables/ruleset-main.json

# Verificar lo que quedó activo:
gh api repos/PabloArauzCaballero/PasanakuBackend/rulesets --jq '.[].name'
```

## 5. CODEOWNERS — estado provisional

`.github/CODEOWNERS` (H2.S5.M1) asigna hoy **todo** a `@PabloArauzCaballero`: es
el único de los cinco puestos con handle de GitHub verificado en el historial de
commits, junto con `@richardparra99` (que no tiene ninguna de las siete rutas
sensibles asignadas todavía). Asignarle una ruta a Justin, Leo o Marcelo sin
saber su usuario real violaría la regla 00 (anti-invención) y podría enrutar la
revisión de un área de dinero a la persona equivocada. Es una decisión que
requiere confirmación humana (`DECISION_REQUIRED`), no algo que este carril
pueda simular.

## Ver también

[[ADR-025 Empaquetado y despliegue de los servicios]] · [[_Arquitectura]]
