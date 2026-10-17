# Plan — Organizador Aprendiz automático (CU-90 vía básica)

- Fecha: 2026-10-17 · Repos: PasanakuBackend (identidad, organizador, seeders) · Predecesor: R-ORG-02, CU-90/91.
- Resultado observable: un participante con KYC aprobado acepta en la app el contrato de organizador (provisional, solo TEST) y queda habilitado como Aprendiz para crear su grupo y jugar en él, sin aprobación manual.
- Kill-test: con KYC aprobado y contrato aceptado, `POST /grupos` sigue respondiendo 403/422.

## Alcance
- IN: organizador (`GET/POST /organizadores/yo/habilitacion-basica`, `POST …/firma`), identidad (`POST /usuarios/{id}/roles/organizador`), política `FIRMA_CONTRATO`, Postman generado.
- OUT: app móvil (otro repo de trabajo), backoffice, texto legal definitivo, producción.
- Ambigüedad: texto del contrato. Supuesto: texto provisional rotulado «no válido para producción», versión `APR-PROV-TEST-2610`. Confirmar con negocio/cumplimiento.
- Decisión del usuario (2026-10-17): habilitación automática nivel básico; se usan los límites ya configurados de APRENDIZ (2 grupos, Bs 30.000).

## H1 — Habilitación básica sin bandeja
**CA:** Dado un participante con KYC aprobado, cuando acepta y firma el contrato, entonces queda organizador HABILITADO APRENDIZ con contrato firmado y rol ORGANIZADOR otorgado por quien aprobó su KYC.
**DoD:** integrationTest + webTest de organizador e identidad en verde.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Organizador: aceptar crea ficha + contrato pendiente; firmar con token | correcto/límite/inválido | `:servicios:organizador:integrationTest --tests *CU90HabilitacionBasicaTest` 8/8 | HECHO |
| H1.S1.M2 | Identidad: token FIRMA_CONTRATO, firma vía organizador, rol ORGANIZADOR | correcto/límite/inválido con doble de organizador | `:servicios:identidad:integrationTest --tests *CU90RolOrganizadorTest` 5/5 | HECHO |
| H1.S1.M3 | Autorización negativa de los endpoints nuevos | 401 sin sesión, 403 rol sin PARTICIPANTE, 400 campo de más | webTest organizador 41/41, identidad 124/124 | HECHO |
| H1.S1.M4 | Política FIRMA_CONTRATO en el catálogo | semilla generada | `scripts/generar_semillas.py` 1514 filas validadas | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Texto de contrato provisional | No vale legalmente | Rotulado; reemplazar subiendo VERSION |
| Rol ORGANIZADOR trae GRUPO_ADMINISTRAR | Endpoints sin chequeo por grupo | Modelo existente; auditar endpoints aparte |
