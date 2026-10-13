---
name: norma-nueva
description: "Incorporar una norma o cambio regulatorio al proyecto AportaYa: investigar la fuente real (ASFI, UIF, BCB, SIN), actualizar docs/Cumplimiento.md, decidir si es dato de catálogo o estructura nueva, y propagar a casos de uso y restricciones. Úsala cuando aparezca una resolución, circular, instructivo o umbral nuevo, o cuando haya que verificar si el modelo cumple algo."
---

# Incorporar una norma nueva

## Principio

**Ninguna cifra regulatoria se cablea.** Umbrales, límites, plazos, alícuotas y
periodicidades viven en tablas con vigencia y con la cita normativa al lado:

| Qué cambia | Dónde vive |
| --- | --- |
| Umbral de reporte a la unidad de inteligencia financiera | `umbral_reporte_uif` |
| Límite de saldo u operación por nivel de diligencia | `limite_operativo_billetera` |
| Periodicidad y plazo de un reporte regulatorio | `catalogo_reporte_regulatorio` |
| Alícuota de un impuesto | `impuesto` |
| Precio y comisiones | `tarifario` + `concepto_tarifa` |
| Tipología de monitoreo | `regla_monitoreo_lft` |
| Alcance autorizado de la licencia | `licencia_regulatoria` |

Cada una tiene `vigente_desde` / `vigente_hasta` y una columna `base_normativa`,
`fuente_normativa` o `base_legal`. **Las filas anteriores nunca se borran**: hay
que poder explicar qué regía el día de una operación pasada.

## Procedimiento

### 1. Buscar la fuente primaria

Orden de preferencia: texto oficial del organismo → resolución publicada →
análisis de estudio jurídico → nota de prensa. **Nunca** una sola fuente
secundaria para un número.

Sitios: `asfi.gob.bo`, `uif.gob.bo`, `bcb.gob.bo`, `impuestos.gob.bo`,
`lexivox.org`, `bolivia.infoleyes.com`.

Muchos PDF oficiales están escaneados o cifrados. Si `WebFetch` devuelve binario:

```bash
pip3 install --quiet pypdf
python3 - <<'EOF'
import pypdf, re
r = pypdf.PdfReader("archivo.pdf")
t = re.sub(r'\s+', ' ', "\n".join(p.extract_text() or '' for p in r.pages))
print(t[:8000])
EOF
```

Si el PDF es imagen (pocos caracteres extraídos), decirlo explícitamente y buscar
la resolución modificatoria, que suele venir en texto.

### 2. Decidir qué tipo de cambio es

| Situación | Qué hacer |
| --- | --- |
| Cambia un valor (umbral, plazo, alícuota, límite) | Fila nueva en `seeders/minimos/*.json` con vigencia. **Cero código.** Skill `semillas-catalogos` |
| Aparece un concepto nuevo con datos propios (formulario, registro, declaración) | Tabla nueva → skill `boveda-modelo` |
| Cambia un procedimiento | Caso de uso → skill `caso-de-uso` |
| La norma exige que algo sea imposible | Restricción → skill `restriccion` |
| Exige un reporte periódico | Fila en `catalogo_reporte_regulatorio` + caso de uso de remisión |

### 3. Actualizar `docs/Cumplimiento.md`

Agregar o modificar la fila en la sección del organismo, con:

- **Requisito** redactado como obligación, no como resumen.
- **Estado**: ✅ cubierto · 🟡 parcial · 🔵 fuera del modelo · ❌ brecha abierta.
- **Cómo lo soporta el modelo**: tabla y columna concretas, enlazadas.

Si el estado es ❌, agregar además la brecha a la sección §8 con tipo y acción
recomendada. **Una brecha sin fila en §8 es una brecha invisible.**

Agregar la fuente a §9 con el enlace y actualizar `fecha_revision` en el
frontmatter.

### 4. Propagar

- ¿Hay un caso de uso que ejecuta la obligación? Si no, escribirlo.
- ¿Hay una restricción que impide violarla? Si no, agregarla.
- ¿Hay un plazo de adecuación? Registrarlo como tarea con fecha, no como comentario.

### 5. Verificar

```bash
python3 scripts/generar_boveda.py     # sin_resolver debe ser []
python3 scripts/generar_ddl.py        # esquema + restricciones + semillas
```

Si la norma cambió un valor de catálogo, la verificación es que la fila vieja
**siga estando** con su `vigente_hasta` cerrado. Borrarla rompe la reproducción de
operaciones pasadas, que es exactamente lo que un supervisor viene a mirar.

## Cómo redactar el estado con honestidad

- No marcar ✅ porque "se podría guardar en un JSON". ✅ significa que existe la
  columna y el flujo.
- 🔵 no es una excusa: es la señal de que alguien fuera del equipo técnico tiene
  que hacer algo (licencia, política, capital, auditoría externa).
- Si una cifra no se pudo confirmar contra fuente primaria, **decirlo en el
  documento** con un aviso, como está hecho con los límites de dinero electrónico.

## Lo que nunca hay que hacer

- Afirmar que el sistema "cumple" una norma que exige licencia o proceso
  organizacional. Un esquema de datos aporta evidencia, no autorización.
- Inventar números de artículo o de resolución. Si no se verificó, se deja el
  campo para que legal lo complete.
- Sembrar un umbral en producción sin confirmación del área legal.

## Ver también

`docs/Cumplimiento.md` · skills `boveda-modelo`, `caso-de-uso`, `restriccion`,
`semillas-catalogos`, `cumplimiento-uif`, `facturacion-sin`,
`reclamos-consumidor`.
