---
name: seguridad-sesion-rls
description: "Proteger datos en AportaYa: contexto de sesión con SET LOCAL, políticas de fila, roles de base, segregación de funciones, acceso a datos personales y cifrado. Úsala al abrir cualquier consulta a una tabla con RLS, al crear un endpoint o un trabajo del worker, al tocar sesiones o dispositivos, y cuando alguien proponga 'filtrar por usuario en el backend'."
---

# Sesión, políticas de fila y datos personales

## La regla de una línea

**El contexto se fija con `SET LOCAL` dentro de la misma transacción del caso de
uso, y fuera de una transacción no se consulta nada con RLS** 
([[ADR-007 Sesión, RLS y pooling]]).

```ts
await db.transaction().execute(async (trx) => {
  await sql`SET LOCAL app.usuario_id = ${usuarioId}`.execute(trx)
  await sql`SET LOCAL app.rol        = ${rol}`.execute(trx)
  return organismo.ejecutar(trx, entrada)     // recién ahora
})
```

`SET` normal —sin `LOCAL`— **persiste en la conexión del pool**: el siguiente
request hereda la identidad del anterior. Es la fuga de datos más silenciosa que
puede tener este sistema, y no deja rastro. PgBouncer corre en modo *transaction*
justamente porque el contexto muere en el `COMMIT`.

## Por qué RLS y no un `WHERE` en el servicio

Un filtro en el servicio es una promesa: hay que confiar en que ningún camino lo
olvidó. Una política de fila es un control **verificable con una consulta**, que es
lo que pide un supervisor. La aplicación filtra igual —para no traer de más—, pero
la garantía está en la base.

## Roles de base

| Rol | Puede | No puede |
| --- | --- | --- |
| `api` | Leer y escribir según políticas | `UPDATE`/`DELETE` en tablas *append-only*; editar catálogos regulatorios |
| `worker` | Ejecutar trabajos y escribir efectos | Suplantar a un usuario salvo que el evento traiga el actor original |
| `reportes` | **Solo lectura** | Escribir cualquier cosa |
| `migrador` | DDL | Correr en horario de servicio sin ventana |

Ninguno es superusuario. Si un proceso necesita más permisos de los que tiene, la
respuesta por defecto es que el diseño está mal, no que falta un `GRANT`.

## El worker no es anónimo

Un trabajo también fija su contexto: actúa **como sistema**, con su propio
identificador, y así queda en la bitácora. Un efecto sin actor identificable es
un agujero de auditoría. Cuando el evento trae el actor original (por ejemplo, el
usuario que disparó el pago), se propaga ese actor.

## Segregación de funciones

Reglas que la base impone, no el buen criterio:

| Regla | Dónde |
| --- | --- |
| Quien analiza una alerta no la revisa | `caso_investigacion_lft` |
| Quien registra un descuadre no lo aprueba | `descuadre_custodia` |
| Quien carga un catálogo regulatorio no es el rol de aplicación | `GRANT` |
| Quien atiende un reclamo no autoriza su propia reparación | `reclamo_cliente` |

## Sesiones y dispositivos

- `sesion` tiene vencimiento, y se revoca por: cierre voluntario, cambio de
  credencial, incidente de seguridad, o inactividad.
- `dispositivo` se vincula y se puede desvincular; una operación sensible desde un
  dispositivo nuevo exige verificación adicional.
- Rotación de credenciales tras incidente: se revocan **todas** las sesiones
  activas, no solo la sospechosa.
- El identificador de sesión y el de dispositivo se guardan en
  `transaccion_billetera`: es lo que permite responder "¿desde dónde se hizo este
  pago?" sin adivinar.

## Datos personales

| Qué | Cómo |
| --- | --- |
| Documento de identidad, número de cuenta | Cifrado en reposo; se guarda además una versión enmascarada para mostrar |
| Consulta de datos de un tercero por un operador | Deja fila en `registro_acceso_datos`, con motivo |
| Ejercicio de derechos (acceso, rectificación, supresión) | `solicitud_datos_personales`, con plazo guardado (CU-07) |
| Supresión | **No borra evidencia financiera ni regulatoria**: se anonimiza lo que la ley permite (`proceso_anonimizacion`) y se conserva lo que otra ley obliga a conservar |
| Retención | `politica_retencion` fija el plazo por tipo de dato; borrar antes también es incumplir |

El derecho a la supresión y el deber de conservación chocan a propósito: gana el
deber de conservación en los datos financieros, y se documenta por qué.

## Errores y mensajes

Un usuario sin permiso recibe `403` o un resultado vacío, **sin detalles
internos**. Nunca nombres de tabla, SQL ni trazas. Distinguir "no existe" de "no
podés verlo" ya es información filtrada: cuando la diferencia importa, se responde
igual en ambos casos.

## Checklist

- [ ] Toda consulta con RLS corre dentro de una transacción con `SET LOCAL`.
- [ ] No hay ni un `SET` sin `LOCAL` en el repositorio. Se busca antes de mergear.
- [ ] Cada trabajo del worker fija su contexto explícitamente.
- [ ] Hay prueba de que el usuario A **no** puede leer las filas del usuario B.
- [ ] Hay prueba de que el rol `reportes` no puede escribir.
- [ ] El acceso a datos de terceros deja fila con motivo.
- [ ] Ningún mensaje de error expone estructura interna.

## Ver también

`docs/Arquitectura/ADR-007 Sesión, RLS y pooling.md` · `restriccion` ·
`errores-api` · `observabilidad` · `cumplimiento-uif` · familia `R-SEG` de
`docs/Restricciones.md` · CU-05, CU-07, CU-55
