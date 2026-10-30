# Plan — README de `identidad` nombra el CU-07 que el código ya implementa

- Fecha: 2026-10-30 · Repos afectados: PasanakuBackend (solo `servicios/identidad/README.md`) · Predecesor: `2026-10-25-bandeja-de-admisiones` (PR #266, donde el CI mostró el rojo)
- Resultado observable: `DocumentacionVivaContratoTest › La tabla de CU del README coincide con los CU implementados` pasa, y con él el paso `15 · contractTest` deja de cortar `16 · sagaTest` y `16b · cobertura`.
- Kill-test: la prueba falla hoy en `dev` con `identidad: implementa [07] y su README no los nombra` (CI run 40894824052, paso 15). Reproducido en local con una réplica de la prueba sobre `dev` ae38c444.

## Alcance
- IN: una fila en la tabla «Casos de uso» de `servicios/identidad/README.md`.
- OUT: renombrar `CU07FotoDePerfil`, tocar `auditoria`, la bóveda (`docs/CasosDeUso/`) o la numeración de CU; Trivy y f5 (otros PR).
- Hechos (con ruta): la prueba busca `\bCU-(\d{2})\b` en el README y `CU\d{2}[A-Z]` en los nombres de archivo de `src/main/java` (`plataforma/comun-pruebas/.../DocumentacionVivaContratoTest.java:29-84`). `identidad` tiene `aplicacion/CU07FotoDePerfil.java` (commit 509dad69, foto de perfil propia) y su README lista CU-01, 02, 04, 08, 09 y 05. Es la única divergencia entre los servicios.
- **Ambigüedad registrada:** en la bóveda, CU-07 es «Ejercer derechos sobre datos personales» (`docs/CasosDeUso/CU-07 Ejercer derechos sobre datos personales.md`) y lo implementa `auditoria` (`CU07EjercerDerechos`). El commit de la foto de perfil reutilizó el número 07 en `identidad` sin que la bóveda tenga un CU de foto. Supuesto tomado: la fila describe lo que el código hace y deja escrito que el número es el del código, no el de la bóveda. A confirmar con **Pablo** (quien definió la numeración): o se crea un CU propio para la foto y se renombra la clase, o se acepta la fila.

## H1 — La prueba de documentación viva pasa
**CA:** Dado `dev`, cuando corre `DocumentacionVivaContratoTest`, entonces no hay divergencias entre los CU del código y los de cada README.
**DoD:** réplica local de la prueba sin divergencias · `contractTest` verde en el CI del PR.
**Estado:** A MEDIAS (réplica local en verde; falta el CI del PR)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M1 | Agregar la fila CU-07 al README de `identidad`, con la aclaración de numeración | el README contiene `CU-07` y dice que el número es el del código | `python3 -I replica_cu_readme.py .` → `DIVERGENCIAS: []` ([evidencia](./evidencia/01-replica.txt)) | HECHO (contra la réplica) |
| H1.M2 | Abrir el PR hacia `dev` (no draft) y comprobar que es mergeable | `mergeable` = true y `draft` = false | `pull_request_read get` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otro CU del código sin README en otro servicio | la prueba sigue roja | la réplica corre sobre todos los servicios: solo `identidad` diverge |
| El rojo de Trivy y f5 siguen en el PR | el PR no queda 100 % verde | son ajenos y están comentados en #266; se dice en el PR |
