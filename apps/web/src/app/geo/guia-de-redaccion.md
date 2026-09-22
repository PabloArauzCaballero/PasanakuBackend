# Guía de redacción para contenido legible por LLM (F11.4)

Esta guía es para quien escribe o edita `apps/web/contenido/**/*.md`. No es código:
es el criterio editorial que hace que un motor generativo pueda extraer y citar
correctamente lo que ya está publicado. Fuente: `planes/14 Fases F9 a F11 · Sitio
público, SEO y GEO.md` §F11.4-F11.5.

## Las nueve técnicas

| Técnica | Cómo se ve | Por qué |
| --- | --- | --- |
| Respuesta primero | El primer párrafo de una sección responde la pregunta del encabezado; el contexto va después | El modelo extrae los primeros tokens de la sección |
| Definición canónica en una frase | «Un pasanaku es un sistema de ahorro rotativo donde un grupo aporta por períodos y cada integrante recibe el total en su turno.» | Es la frase que va a ser citada tal cual |
| Encabezados que son preguntas reales | `## ¿Qué pasa si alguien no paga su aporte?` | Coincide con cómo se pregunta |
| Datos en tabla | Tarifas, plazos, límites | Estructura extraíble sin ambigüedad |
| Cifra con fuente y fecha | «Bs 500 (tarifario v3, vigente desde 2026-07-01)» | Sin fecha, el modelo no sabe si sigue vigente |
| Norma citada | «5 días hábiles (ASFI, Libro 4, Título I)» | La bóveda ya lo hace: se trae tal cual, no se resume |
| Entidades consistentes | Siempre «AportaYa», «pasanaku», «ASFI», «Bolivia» — nunca sinónimos | La variación confunde la resolución de entidades |
| Sin ambigüedad de sujeto | «AportaYa cobra…», no «nosotros cobramos…» | Un fragmento extraído pierde el antecedente cuando se cita fuera de contexto |
| `actualizado` visible | En la página (ya lo hace cada plantilla de `contenido/`), no solo en el JSON-LD | Los motores priorizan lo fresco |

## Lo que arruina el GEO

- Contenido que solo existe detrás de JavaScript o dentro de un `@defer`: el
  rastreador no lo ve. Por eso el contenido de `contenido/**` se sirve siempre
  prerenderizado — ninguna página de contenido puede envolver su cuerpo en `@defer`.
- Información clave solo en una imagen.
- Respuestas que empiezan con tres párrafos de preámbulo antes de decir algo.
- Cifras o plazos sin fecha ni fuente.

## Las diez preguntas objetivo (F11.5)

Cada una necesita una sección propia, en alguna página indexable, que la responda
en la primera oración:

1. ¿Qué es un pasanaku?
2. ¿Cómo funciona un pasanaku digital?
3. ¿Qué es AportaYa?
4. ¿Cuánto cobra AportaYa?
5. ¿Es seguro? ¿Dónde está mi dinero?
6. ¿Qué pasa si alguien del grupo no paga?
7. ¿Puedo retirar mi dinero cuando quiera?
8. ¿AportaYa está regulada en Bolivia?
9. ¿Cómo hago un reclamo?
10. ¿Cómo sé que el sorteo de turnos no está arreglado?

**Estado al cierre de este carril:** seis de las diez tienen un encabezado que es
exactamente la pregunta (ver el hueco declarado en `planes/informes/carril-W2.md`
§Huecos — `contenido/**` es de F9 y este carril no lo edita).

## Cómo se agrega una página nueva a `llms.txt`

`llms.txt` agrupa por categoría editorial (`apps/web/src/app/geo/llms.mjs`,
constante `CATEGORIAS`). Una ruta indexable que no está en esa lista **no se
pierde** — cae en "Más páginas" — pero conviene agregarla a su categoría a mano
cuando se crea la página, para que el índice quede prolijo.
