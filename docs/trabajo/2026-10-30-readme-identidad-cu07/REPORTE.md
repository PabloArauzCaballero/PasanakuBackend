# Reporte — README de `identidad` nombra el CU-07 que el código implementa

> **AVANCE: 1 / 2 — 50,0 %.** La fila está escrita y la réplica local de la prueba pasa; falta abrir el PR y ver `contractTest` en el CI real.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/awesome-turing-5wmr4r` (reiniciada desde `dev` ae38c444 con un avance rápido: `dev` ya contenía la rama)
- Peldaño de evidencia alcanzado: `WRITTEN` + réplica local de la prueba. No `TESTED` (Gradle no corre aquí).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `servicios/identidad/README.md` tiene la fila CU-07 con la aclaración de numeración | `python3 -I replica_cu_readme.py .` | PASS contra la réplica: de `["identidad: implementa ['07'] y su README no los nombra"]` a `[]` ([evidencia](./evidencia/01-replica.txt)); `verificar_boveda`, `verificar_criterios`, `verificar_carriles` y `verificar_pruebas_web` siguen en exit 0 |

## A medias
### H1.M2 — abrir el PR y comprobar que es mergeable
- Qué anda: el cambio está commiteado y listo.
- Qué no anda: el CI real aún no corrió sobre este cambio.
- Qué falta exactamente: abrir el PR hacia `dev` (no draft), leer `mergeable`/`draft` del PR y confirmar que `15 · contractTest` pasa.
- Dónde quedó: rama `claude/awesome-turing-5wmr4r`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| PR par hacia `test` | TODO | Que se confirme el nombre de una rama propia (la regla de esta sesión es una sola rama de trabajo) |

## Evidencia
- [01-replica.txt](./evidencia/01-replica.txt) y [replica_cu_readme.py](./evidencia/replica_cu_readme.py): copia de la lógica de `DocumentacionVivaContratoTest.elReadmeDiceLosCuQueElServicioTiene` (mismas dos expresiones regulares y el mismo recorrido de `servicios/*`).

## No cubierto
- La prueba Java real (`contractTest`): no se ejecutó aquí. La réplica cubre el mismo recorrido y las mismas expresiones, pero no es la prueba.
- Trivy (4 CVE HIGH en la imagen de `identidad`) y f5 (áreas táctiles de `packages/ui`) siguen rojos y son ajenos a este PR.

## Desvíos del plan
- Ninguno.

## Riesgos residuales
- La fila describe la foto de perfil con el número 07 porque así lo rotula el código; si luego se renombra la clase o se crea un CU propio, hay que actualizar la fila.

## Decisiones y ambigüedades
- **Numeración de CU:** en la bóveda CU-07 es «Ejercer derechos sobre datos personales» (`docs/CasosDeUso/CU-07 …md`, implementado por `CU07EjercerDerechos` en `auditoria`); `identidad` reutilizó el 07 para la foto de perfil (commit 509dad69). Supuesto: la fila documenta lo que el código hace y lo declara. A confirmar con **Pablo** si se crea un CU propio para la foto y se renombra `CU07FotoDePerfil`.
