# H6 — gates locales
```text
$ python3 scripts/verificar_criterios.py  → EXIT 0
Sin divergencias entre la boveda y el codigo.
$ python3 scripts/verificar_boveda.py  → EXIT 0
TODO OK
$ python3 scripts/verificar_carriles.py  → EXIT 0
TODO OK (con avisos)
$ python3 scripts/verificar_maqueta.py  → EXIT 0
TODO OK
$ python3 scripts/verificar_contratos_limites.py  → EXIT 0
Los contratos de request en el alcance de este gate declaran limites y additionalProperties: false.
$ python3 scripts/verificar_seguridad.py  → EXIT 0 (antes del ajuste: KeyError "tabla" por el bloque `sql` de la semilla; ver REPORTE)
TODO OK · 5 aviso(s)
$ python3 scripts/verificar_pruebas_web.py → EXIT 0
La capa web esta cubierta segun ADR-043.
$ python3 scripts/inventario_endpoints.py --check → EXIT 0
El inventario esta alineado: todo lo implementado tiene contrato y viceversa.
$ python3 scripts/generar_ddl.py → EXIT 0, sin diff en sql/
$ python3 scripts/generar_postman.py → EXIT 0; diff solo en postman/*/identidad*.json (parámetros nuevos), commiteado
$ python3 scripts/generar_semillas.py → EXIT 0; diff solo en sql/60_semillas/10-roles-y-permisos.sql
$ java -jar openapi-generator-cli-7.10.0.jar validate -i servicios/identidad/src/main/resources/openapi/identidad.yaml
No validation issues detected.
$ ./gradlew :servicios:{identidad,publicidad,nucleo-financiero}:check -x test -x jacoco…  → BUILD SUCCESSFUL
$ yarn typecheck   (apps/backoffice) → EXIT 0
$ yarn ng test --include nucleo, sistemas, cumplimiento, publicidad (sin a11y) → Test Files 105 passed (105) · Tests 774 passed (774)
```
