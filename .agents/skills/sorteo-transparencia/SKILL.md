---
name: sorteo-transparencia
description: "Construir lo verificable de AportaYa: sorteo de turnos con compromiso y revelación, cadena de bloques de transparencia por grupo, verificación pública y reputación reproducible. Úsala al tocar el sorteo, el orden de turnos, los hashes encadenados o el puntaje de un participante, y cada vez que haya que demostrarle algo a alguien que no confía en nosotros."
---

# Sorteo y transparencia verificable

Un pasanaku funciona con confianza. Digitalizarlo la concentra en nosotros, y eso
es un problema: **"confiá en que el sorteo fue justo" no es una respuesta**. Este
módulo existe para que el participante pueda comprobarlo sin creernos.

## Sorteo: compromiso y revelación

Un sorteo del que solo publicamos el resultado es indistinguible de uno arreglado.
El protocolo separa el momento en que se fija el azar del momento en que se conoce:

```
FASE 1 · compromiso    se genera la semilla y se publica SOLO su hash
                       (append-only: el hash no admite UPDATE)
                       los participantes pueden aportar su propia entropía,
                       cada uno a ciegas de los demás
FASE 2 · revelación    se revela la semilla; cualquiera verifica que
                       SHA256(semilla ‖ entropías) = el hash comprometido
                       y recomputa el orden con el método declarado
```

| Regla | Por qué |
| --- | --- |
| **Un solo sorteo por grupo** (`UNIQUE (grupo_id)`, `R-GRP-05`) | Repetir el sorteo hasta que salga bien es el fraude que esto previene |
| **El compromiso es inmutable** | Si el hash se puede cambiar, el compromiso no compromete nada |
| **El método se declara antes** | "Fisher-Yates con generador determinista sembrado por la semilla", escrito, no implícito |
| **Revelación y creación de turnos en la misma transacción** | Un sorteo revelado sin turnos deja el grupo en un estado que nadie sabe resolver |
| **La entropía de cada participante se guarda por su hash hasta el cierre** | Ver el aporte ajeno permite calcular el propio para forzar un resultado |
| **Se publica la semilla, no solo el resultado** | Sin la semilla, la verificación es imposible |

Verificar es recomputar: mismo algoritmo, misma semilla, mismo orden. Si no
coincide, el sistema lo dice —no lo esconde.

## Hash canónico: la parte que todos subestiman

**Un hash solo sirve si dos implementaciones producen el mismo.** Por eso la
estructura que se hashea es canónica y está escrita:

- Orden de campos fijo, declarado. No el orden en que los devolvió la consulta.
- Importes como **cadena** con escala fija (`"500.00"`), nunca como flotante.
- Fechas en UTC, formato único.
- Sin espacios ni saltos que dependan del serializador.

Si la app y el backoffice calculan hashes distintos para el mismo contenido, la
verificación pública es ruido. Esta parte se prueba con un vector fijo: contenido
conocido → hash conocido.

## Cadena de bloques por grupo

```
bloque_transparencia
  numero_bloque    correlativo por grupo, UNIQUE (grupo_id, numero_bloque)  (R-REP-04)
  hash_anterior    el del bloque previo — génesis si es el primero
  hash_contenido   sobre la estructura canónica del período
  hash_bloque      SHA256(numero ‖ hash_anterior ‖ hash_contenido)
registro_sellado   qué hechos entraron: del bloque al hecho y del hecho al bloque
verificacion_publica  cada verificación, con su resultado, guardada
```

Reglas duras:

1. **No se sella un período con excepciones de conciliación abiertas.** Un bloque
   con datos provisorios miente con firma.
2. **Si el hash anterior no coincide, se detiene el sellado** y se abre incidente.
   La cadena rota es grave: continuar la oculta.
3. **Un bloque no se reescribe nunca.** La corrección de un hecho ya sellado entra
   al bloque siguiente como movimiento compensatorio, y ambos quedan visibles. Es
   la misma regla que en el libro de dinero.
4. **La verificación se guarda** con su resultado, incluso cuando falla.
   Especialmente cuando falla.

## Reputación reproducible

```
modelo_scoring (versionado) + peso_factor + regla_impacto_evento
  → evento_reputacion  → puntaje_reputacion + componente_score
```

| Regla | Por qué |
| --- | --- |
| El puntaje se **descompone**: cada componente con su contribución, que suma el total | Un número sin explicación no se puede reclamar |
| El puntaje guarda **qué versión del modelo** lo produjo | Recalcular con el modelo de hoy da otro número y rompe la evidencia |
| Vigencias que no se solapan (`EXCLUDE`, `R-REP-02`) | Dos puntajes vigentes es una ambigüedad que alguien va a resolver mal |
| Los eventos son *append-only*; el puntaje se recalcula, no se corrige a mano | Editar un puntaje es editar una reputación |
| El participante puede ver por qué bajó y reclamarlo | Skill `reclamos-consumidor` |

Cambiar el modelo de scoring es **una versión nueva**, con su fecha de vigencia. Los
puntajes viejos conservan la suya.

## Qué se publica y qué no

| Se publica | No se publica |
| --- | --- |
| Semilla, método y resultado del sorteo | Datos personales de los participantes |
| Hashes de la cadena y el procedimiento para recomputarlos | Montos individuales fuera del grupo |
| Métricas agregadas del grupo | Puntaje de un participante a terceros sin su consentimiento |

Transparencia no es exposición: es que **lo verificable sea verificable**.

## Checklist

- [ ] El compromiso se publica antes y es inmutable, con prueba de que el `UPDATE`
      se rechaza.
- [ ] La verificación recomputa de verdad, con el algoritmo declarado.
- [ ] Hay vector de prueba fijo para el hash canónico.
- [ ] No se sella con excepciones abiertas; hay prueba de ese rechazo.
- [ ] Cadena rota ⇒ se detiene y se abre incidente.
- [ ] `UNIQUE (grupo_id, numero_bloque)` probado contra sellado concurrente.
- [ ] El puntaje descompone sus componentes y suman el total.
- [ ] Cambiar el modelo de scoring no altera puntajes ya emitidos.

## Ver también

`contabilidad-partida-doble` · `observabilidad` · `restriccion` ·
CU-60, CU-61, CU-70 a CU-73 · familias `R-GRP` y `R-REP`
