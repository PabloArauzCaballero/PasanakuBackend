---
name: motor-de-reglas
description: "Escribir reglas configurables en AportaYa —cumplimiento, antifraude y automatización— sin cablear números ni permitir código arbitrario: expresión compilada, umbral que apunta al catálogo, simulación obligatoria antes de activar, catálogo cerrado de acciones, confirmación humana para lo sensible, versionado y calibración contra desenlaces. Úsala al crear o cambiar cualquier regla, cuando una regla genere demasiadas alertas, o cuando alguien proponga automatizar un cobro."
---

# Motor de reglas

Tres motores, una misma disciplina:

| Tabla | Qué vigila | Caso de uso |
| --- | --- | --- |
| [[regla_cumplimiento]] | umbrales, límites, consumidor | [[CU-48 Calibrar reglas de cumplimiento y triar sus alertas]] |
| [[regla_antifraude]] | patrones de fraude sobre la billetera | [[CU-48 Calibrar reglas de cumplimiento y triar sus alertas]] |
| [[regla_automatizacion]] | tareas del organizador y del grupo | [[CU-95 Definir una regla de automatización]] |

## Las cinco reglas del motor

### 1. El umbral apunta al catálogo, nunca al número

```ts
// mal — una migración cada vez que la UIF cambia un monto
expresion: 'monto > 10000'

// bien — el valor vive en umbral_reporte_uif / umbral_operativo, con vigencia
expresion: 'monto > umbral("UIF_PCC01")'
```

`R-UIF-01` y el error `UMBRAL_CABLEADO` existen exactamente para esto. Ver la skill
`semillas-catalogos`.

### 2. La expresión se compila, no se evalúa como texto

Se parsea a un AST validado contra el esquema de campos disponibles. Nada de `eval`,
nada de SQL concatenado, nada de plantillas. Un campo inexistente falla **al
guardar**, no en producción a las tres de la mañana.

### 3. Simular sobre datos reales antes de activar

```
operaciones evaluadas · operaciones marcadas · % del tráfico · muestra de 20
```

Una regla que marca el 30 % del tráfico no se activa: se recalibra. `SIMULACION_REQUERIDA`
bloquea la activación. La simulación corre en la **réplica de lectura**.

### 4. La acción es proporcional a la severidad

| Acción | Efecto | Severidad mínima |
| --- | --- | --- |
| `SOLO_ALERTAR` | genera alerta | cualquiera |
| `RETENER` | congela la operación | media |
| `BLOQUEAR` | corta la operatoria del usuario | alta |
| `RECHAZAR` | deniega en el acto | alta |

`ACCION_DESPROPORCIONADA` rechaza el resto. Bloquear a un cliente es caro y tiene
que justificarse.

Para automatización, la lista es cerrada y la base la hace cumplir (`R-ORG-06`):

```
automáticas   RECORDAR · GENERAR_OBLIGACIONES · ABRIR_PERIODO · PUBLICAR_RESUMEN · MARCAR_MORA
confirmación  PROPONER_COBRO · PROPONER_ENTREGA · PROPONER_SANCION · PROPONER_COBERTURA
```

**Todo lo que mueve dinero o afecta derechos espera a una persona.** No existe la
opción de saltearlo, y el silencio no es consentimiento: la tarea caduca.

### 5. Toda regla se versiona y ninguna se borra

Hay que poder decir con qué regla se evaluó una operación de hace un año.
Desactivar escribe la fecha; la fila queda.

## La alerta se cierra con conclusión

`R-UIF-07`: no se cierra en blanco. Las tres conclusiones posibles son
`SIN_MERITO`, `MERITA_INVESTIGACION` y `AJUSTAR_REGLA`, y la tercera es la que
mantiene el motor vivo.

El tablero mensual por regla —generadas, cerradas sin mérito, convertidas en caso—
es lo que decide si se afina, se endurece o se retira. **Una regla con cero alertas
en seis meses también se revisa**: o el riesgo desapareció, o la regla está mal
escrita y da falsa tranquilidad.

## Prioridad y empate

`uq_regla_automatizacion_prioridad` impide dos reglas activas con la misma prioridad
y disparador. **El empate se resuelve al definir, no al ejecutar.** Cuando dos reglas
podrían disparar sobre el mismo hecho, corre la de mayor prioridad y la otra queda
registrada como no aplicada, con motivo.

## Ámbito

Una regla de organizador **no puede tocar grupos que no administra**, y eso lo
garantiza la política de fila (`R-SEG-03`), no la interfaz. Si la condición apunta a
un ámbito ajeno, el conjunto es vacío y la regla nunca dispara: se avisa al definirla
en vez de dejarla muerta en silencio.

## Qué no hacer

- No permitir expresiones que ejecuten código.
- No activar sin simular.
- No poner un número regulatorio dentro de una expresión.
- No cerrar alertas en lote sin fundamento.
- No automatizar una acción sensible "porque el organizador es de confianza".
- No cambiar la expresión de una regla cuando lo que cambió fue el umbral.

## Ver también

- [[CU-48 Calibrar reglas de cumplimiento y triar sus alertas]] ·
  [[CU-95 Definir una regla de automatización]] ·
  [[CU-96 Programar y ejecutar una tarea automatizada]] ·
  [[CU-97 Anticipar el riesgo con alertas tempranas]]
- `R-UIF-01` · `R-UIF-07` · `R-ORG-06` · `R-SEG-03` en [[Restricciones]]
- Skills: `cumplimiento-uif`, `semillas-catalogos`, `automatizacion-tareas`,
  `observabilidad`, `norma-nueva`
