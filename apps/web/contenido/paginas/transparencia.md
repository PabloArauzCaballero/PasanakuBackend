---
ruta: /transparencia
titulo: "Transparencia: cómo se sella y se verifica"
bajada: "El sorteo de turnos y la historia de cada grupo se pueden comprobar por tu cuenta, sin creernos."
descripcion: "Cómo AportaYa hace verificable el sorteo de turnos y la historia de cada grupo: compromiso, revelación y bloques encadenados por hash."
indexable: true
actualizado: 2026-09-09
---

## El sorteo, en dos actos

El orden de turnos de un grupo se decide con un sorteo en dos fases:

1. **Compromiso.** Antes de sortear, se publica el hash de una semilla secreta (más las entropías que aportó cada participante). Nadie puede probar semillas hasta que salga el orden que le convenga, porque el hash ya quedó publicado.
2. **Revelación.** Se publica la semilla real. Cualquiera puede recomputar `SHA-256(semilla + entropías)` y comprobar que coincide con el hash comprometido, y recomputar el barajado (Fisher-Yates determinista) para comprobar que el orden publicado es el que sale de esa semilla.

Cada grupo tiene su página pública de verificación del sorteo, en `/publico/sorteos/:id`, con el paquete completo en JSON para verificarlo con tu propio código.

## La historia del grupo, en bloques encadenados

Cada período cerrado, cada entrega y cada hito del grupo se sella en un bloque que incluye el hash del bloque anterior. Alterar un dato antiguo obliga a rehacer todos los bloques posteriores — y eso se nota, porque la cadena deja de cerrar.

La página pública `/publico/grupos/:codigo` muestra el veredicto de esa verificación: si la cadena está íntegra, o desde qué bloque se rompió.

## Por qué no alcanza con que lo digamos nosotros

Un algoritmo que solo corre en nuestros servidores es una promesa. Un algoritmo publicado, con el paquete de datos descargable, es algo que cualquiera comprueba por su cuenta.
