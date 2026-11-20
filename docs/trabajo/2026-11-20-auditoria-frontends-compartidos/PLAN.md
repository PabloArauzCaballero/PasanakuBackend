# Plan — Auditoría de solo lectura: frontends-compartidos (L6)

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo esta carpeta) · Predecesor: metaprompt `docs/trabajo/2026-11-20-metaprompt-auditoria/` (rama `claude/nice-fermat-essf9e`)
- Rama: `auditoria/frontends-compartidos-2026-11-20` desde `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`
- Resultado observable: Pablo abre `AUDITORIA-frontends-compartidos.md` y encuentra, con ruta y línea, todo lo que está mal en `packages/*`, `clientes/*` y el shell/compartido de `apps/{movil,backoffice,web}`, más la distancia al flujo nuevo (R-NUEVA-1/2).
- Kill-test: un hallazgo S1/S2 cuya línea citada no dice lo que el informe afirma; o un archivo de la huella ausente del anexo.

## Alcance
- IN: `packages/{diseno_flutter,dominio-cliente,simulado,tokens,tutoriales,ui}`, `clientes/{angular,dart}`, shell de `apps/movil` (`lib/*.dart`, `lib/navegacion`, `lib/infraestructura`, `lib/proveedores`, `lib/dominio`, widgets compartidos), shell de `apps/backoffice` (`src/app/*.ts`, `nucleo/`, `layout/`), shell de `apps/web` (`src/app/*.ts`, `nucleo/`, `layout/`, `seo/`, páginas institucionales sin módulo dueño y `contenido/`).
- OUT: pantallas de módulo (`apps/movil/lib/pantallas/<modulo>`, `apps/backoffice/src/app/rutas/<modulo>`) salvo el punto de contacto; servicios Java; `sql/`. Ninguna modificación de código.
- Ambigüedades registradas: A-1 a A-4 del metaprompt (se registran, no se resuelven). Frontera de "componente compartido" en apps: supuesto en «Límites» del informe.

## H1 — Informe de auditoría L6 entregado
**CA:** Dado el commit auditado, cuando una persona abre el informe, entonces cada hallazgo tiene ubicación, evidencia y severidad, y las 12 secciones del metaprompt §9 existen.
**DoD:** `git status` muestra solo esta carpeta · commit y push a la rama · `REPORTE.md` con avance en la primera línea.
**Estado:** EN CURSO

| ID | Microtarea (fase §8) | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M0 | Fase 0 — preparación | commit anotado, historial completo, reglas leídas | `git rev-parse HEAD`; `git rev-parse --is-shallow-repository` → false | HECHO (07fc697; false tras `git fetch --unshallow`) |
| H1.S1.M1 | Fase 1 — huella y mapa de conexiones | sección 3 del informe escrita | inspección del informe | EN CURSO |
| H1.S1.M2 | Fase 2 — lectura con registro de cobertura | anexo con una fila por archivo | conteo de filas vs `git ls-files` | TODO |
| H1.S1.M3 | Fase 3 — inventarios APROB / RESPALDO | tablas 4a/4b/4c escritas | `grep` de términos documentado | TODO |
| H1.S1.M4 | Fase 4 — vista de conjunto | hallazgos DUP/REESCRITURA/FRAGIL | inspección | TODO |
| H1.S1.M5 | Fase 5 — contratos y verificadores | matriz + salidas de verificadores pegadas | salidas en informe | TODO |
| H1.S1.M6 | Fase 6 — estándares | cada hallazgo con ISO 25010 / regla | inspección | TODO |
| H1.S1.M7 | Fase 7 — refutación | S1/S2 re-abiertos | líneas re-leídas | TODO |
| H1.S1.M8 | Entrega | commit + push | `git push` exit 0 | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Huella ~80 k líneas | cobertura parcial | subagentes de solo lectura (≤4, lotes disjuntos) + verificación propia de cada hallazgo; declarar cobertura real |
| Sin Flutter/Node en la nube | verificadores no corren | probar una vez, pegar error, NO COMPROBADO |
