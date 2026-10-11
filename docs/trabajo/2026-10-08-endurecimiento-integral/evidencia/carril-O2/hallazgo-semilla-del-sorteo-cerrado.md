# Hallazgo de seguridad MEDIO · la semilla del sorteo salia en la respuesta de comprometer

**Amenaza**: quien dispara el sorteo recibia la semilla al comprometer, podia precalcular el orden (Fisher-Yates determinista) y
abandonar o reintentar si no le convenia. El servidor ya sellaba la semilla (`snapshot_sorteo.semilla_sellada`) y revelaba con la
sellada, asi que la respuesta HTTP era una fuga de hecho y la descripcion del contrato («el servidor no la guarda») ya era falsa.

**Control agregado** (donde vive):
- Contrato `servicios/grupos/src/main/resources/openapi/grupos.yaml`: `CompromisoDeSorteo` sin `semilla` (ni en `required` ni en `properties`), descripcion reescrita: solo el hash, la semilla queda sellada hasta revelar. `EntradaRevelacion.semilla` sigue declarada **obsoleta e ignorada**.
- `SorteoDelGrupo.comprometer` ya no copia la semilla a la respuesta; el record de dominio `CU60Sortear.Compromiso` la conserva solo para uso interno y pruebas (su `toString` la oculta).
- Consumidores: `apps/movil/.../cu60_sortear_turnos.dart` dejo de pedir y enviar `semilla` al revelar (no tenia llamadores); web y backoffice no la leian; `packages/simulado/ejemplos/grupos/comprometerSorteo.json` regenerado desde el contrato (`contratos-a-json.mjs` + `generar-ejemplos.mjs`); `scripts/generar_postman.py` ya afirmaba que la semilla no viaja.

**Pruebas** (`servicios/grupos/src/test/java/bo/aportaya/grupos/web/SorteoDelGrupoTest.java`):
- `comprometerNoDevuelveLaSemilla`: la respuesta serializada no contiene ni el texto de la semilla ni la clave `semilla`, y el modelo generado ya no tiene el campo.
- `semillasDeClientesDistintasNoCambianNada`: revelar con dos semillas distintas del cliente llega igual al caso de uso (semilla nula = la sellada).
- ya existentes y sin tocar: `laSemillaDelClienteNoLlega`, `CU60SnapshotTest.paqueteReconstruible` (el orden sale de la sellada) y `congelaAlComprometer`.

Riesgo residual: el cliente generado y la compilacion de web/backoffice/movil se verifican con `generarClientes` (ver `indice-y-resultados.md`).
