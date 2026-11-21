# Actor autenticado y cuatro ojos reales

Componente único de la plataforma para saber **quién firma** un acto y para exigir que un acto
de dos firmas lo hagan **dos personas distintas, cada una con su propio token**
(F1.S2.M1 del plan de correcciones del 2026-11-21; decisión P-13 de Pablo).

Clases, en `bo.aportaya.plataforma.web.seguridad`:

| Clase | Qué es |
|---|---|
| `CuatroOjos` | `@Component`. `actor()` da el actor de la petición; `segundaFirma(primero, codigo)` valida la segunda firma; `exigir(primero, segundo, codigo)` es la regla pura. |
| `ActorAutenticado` | `usuarioId` (`sub` del token) + `sesionId` (reclamo `sesion_id`). |
| `SegundaFirma` | El par de actores ya validado. |

## Reglas

1. **Ningún actor sale del cuerpo, la query ni una cabecera.** El contrato del endpoint no
   declara `aprobadoPor`, `autorizadaPor`, `segundaRevisionPor`, `revisor`, `analistaId` ni nada
   parecido. Si un cliente lo manda igual, `CuerposEstrictos` lo rechaza con `400 AP-VAL-02`.
2. **La segunda firma es otra petición con otro token.** No se aceptan dos identidades en una
   misma petición.
3. **Misma persona = rechazo**, aunque llegue desde otra sesión. Lo mismo si dos usuarios
   compartieran sesión (sería un defecto previo) o si la firma es del rol `sistema`.
4. Un token sin `sesion_id` (step-up, servicio a servicio) **no firma**: `401`.
5. `CuatroOjos` no decide **quién puede** firmar: eso es el `@Permiso` del endpoint, y los pares
   de permisos incompatibles los controla identidad (segregación de funciones).

## Cómo se usa

```java
// Primer acto: solicitar el reverso. El actor se guarda con el registro.
@Permiso("REVERSO_SOLICITAR")
public ResponseEntity<Salida> solicitar(EntradaReverso entrada) {   // sin campo de aprobador
    ActorAutenticado solicitante = cuatroOjos.actor();
    UUID id = reversos.solicitar(entrada, solicitante.usuarioId(), solicitante.sesionId());
    ...
}

// Segundo acto, en otra petición: autorizarlo.
@Permiso("REVERSO_AUTORIZAR")
public Salida autorizar(UUID reversoId) {
    var pendiente = reversos.pendiente(reversoId);
    SegundaFirma firma = cuatroOjos.segundaFirma(
            ActorAutenticado.registrado(pendiente.solicitadoPor(), pendiente.sesionSolicitud()),
            CodigoError.de(14, 2));                       // el código del caso de uso → 422
    reversos.autorizar(reversoId, firma.segundo().usuarioId(), ...);
}
```

- El primer actor se **rehidrata de la tabla del propio servicio**, donde se guardó con
  `actor()`. Si la tabla histórica no guarda la sesión, `ActorAutenticado.registrado(usuario, null)`
  sigue siendo válido: decide la persona.
- La precondición de estado (que el acto siga pendiente) va en el `UPDATE` del servicio, no en un
  `if` previo (regla 96.3.2). `CuatroOjos` no reemplaza eso.
- En pruebas web, `Sesiones.enSesion(usuario, sesion, rol, permisos...)` fabrica un token con
  `sesion_id`; `ContextoWebDePrueba` ya registra `CuatroOjos`.

## Pruebas que lo respaldan

- `CuatroOjosTest` (regla pura, en `test`).
- `CuatroOjosWebTest` (capa web, en `webTest`): otra persona aprueba; misma sesión → 422; misma
  persona en otra sesión → 422; aprobador en el cuerpo → 400 `AP-VAL-02`; token sin `sesion_id`
  → 401; sin token → 401; sin permiso → 403.

## Lo que todavía no usa esto

La migración de los endpoints que hoy toman el aprobador del cuerpo es de la tanda siguiente.
La lista está en `docs/trabajo/2026-11-21-f1-identidad-token-acceso/REPORTE.md`.
