---
name: respaldos-restauracion
description: "Respaldar y restaurar los datos de AportaYa: estrategia y variables de configuración, retención y cifrado, recuperación a un punto en el tiempo, ensayo de restauración obligatorio, RPO y RTO, y conservación de evidencia regulatoria. Úsala al configurar respaldos, al planificar continuidad (CU-56), o cuando alguien diga que ya hay backup."
---

# Respaldos y restauración

> **Un respaldo no existe hasta que se demuestra que restaura.** Mientras no haya un
> ensayo con resultado escrito, lo que hay es un archivo de origen desconocido.

Esto no es solo buena práctica: la continuidad operativa es exigible, y CU-56 obliga a
**ejercitarla con evidencia**.

## Configuración

```
RESPALDO_ACTIVO
RESPALDO_PROGRAMACION           cron
RESPALDO_ZONA_HORARIA
RESPALDO_ESTRATEGIA             pg_dump | pgbackrest
RESPALDO_RETENCION_DIAS
RESPALDO_DESTINO                almacenamiento de objetos con bloqueo
RESPALDO_CIFRADO_ACTIVO
RESPALDO_COMPRESION
RESPALDO_DURACION_MAXIMA_SEG
ENSAYO_RESTAURACION_PROGRAMACION
```

Sin valores por defecto silenciosos: si falta uno, el trabajo de respaldo no arranca y
avisa.

## Estrategia

| Estrategia | Cuándo | Qué da |
| --- | --- | --- |
| `pg_dump` lógico | Etapa temprana, base chica | Portabilidad; **no** da punto en el tiempo |
| **pgBackRest** | Operación real con dinero de terceros | Respaldo completo + WAL ⇒ recuperación a un punto en el tiempo |
| Instantánea del proveedor | Complemento | Rápida, pero **nunca** la única garantía |

Con custodia de fondos ajenos, perder cinco minutos de transacciones no es aceptable:
la meta es recuperación a un punto en el tiempo, no una copia de anoche.

## Dónde y cómo se guarda

- Destino **fuera** del servidor de base de datos, con cifrado en reposo y en
  tránsito, y clave gestionada aparte del respaldo.
- **Bloqueo de objeto** o inmutabilidad donde el proveedor lo permita: un respaldo que
  el atacante puede borrar no es un respaldo.
- Retención acorde a [[Cumplimiento]]: los registros de auditoría y la evidencia
  regulatoria tienen plazos legales y **no se purgan por conveniencia de espacio**.
- Acceso con credencial propia (`backup`), separada del runtime.

## Ejecución

- El respaldo corre como **trabajo separado**, nunca dentro del proceso de la API.
- Una ejecución a la vez: si la anterior no terminó, la nueva no arranca.
- Se registra: inicio, fin, duración, tamaño, resultado y suma de verificación.
- Métrica y alerta: respaldo fallido, respaldo que no corrió, duración fuera de rango.
  **Que no haya alerta no es lo mismo que haya respaldo.**

## Ensayo de restauración

Programado, no improvisado. El resultado se archiva como evidencia:

```
1. Entorno aislado, jamás sobre producción
2. Verificar suma de verificación y descifrar
3. Restaurar hasta un punto en el tiempo elegido
4. Aplicar el esquema esperado y verificar que la versión de la aplicación arranca
5. Prueba de humo de solo lectura + consultas de verificación de Restricciones
6. Medir RPO y RTO reales
7. Escribir el informe: qué se restauró, cuánto tardó, qué falló
```

Si el ensayo falla, es un **incidente**, no una tarea pendiente: se registra como
evento de riesgo operativo (`observabilidad`) y se corrige antes de seguir.

## RPO y RTO

Se declaran, se miden en el ensayo y se comparan:

| Objetivo | Cómo se lee |
| --- | --- |
| **RPO** | Cuántos datos se aceptan perder. Con dinero en custodia, minutos |
| **RTO** | Cuánto puede estar caído el servicio. Se mide en el ensayo, no se estima |

Un RPO declarado que el ensayo no alcanza es un RPO falso: se corrige la estrategia o
se corrige el número, pero no se deja la contradicción.

## Qué más se respalda

| Además de la base | Por qué |
| --- | --- |
| Evidencia y archivos adjuntos | Reportes, respaldos de reclamo, acuses |
| Configuración de catálogos (`seeders/`) | Está en git, pero se verifica que la versión desplegada coincide |
| Claves de cifrado | En el gestor de secretos, con su propio respaldo y rotación |

## Antipatrones

- "Ya hay backup" sin ensayo de restauración.
- Respaldo en el mismo servidor o en el mismo bucket que la aplicación escribe.
- Cifrar y guardar la clave junto al respaldo.
- Retención decidida por costo sin mirar los plazos legales.
- Ensayo hecho una vez, en el primer mes, y nunca más.
- Restaurar en producción "para probar".

## Ver también

`despliegue-contenedores` · `observabilidad` · `definicion-de-terminado` ·
`extraccion-de-datos` · `reportes-regulatorios` ·
`docs/Arquitectura/ADR-013 Respaldo y continuidad.md` · `docs/Cumplimiento.md`
