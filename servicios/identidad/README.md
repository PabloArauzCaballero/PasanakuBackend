# Servicio `identidad`

Modulo 01 de la boveda — Identidad, Usuarios y Seguridad.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `identidad` |
| **Rol de base** | `svc_identidad` |
| **Prefijos de ruta** | `/identidad` · `/usuarios` · `/sesion` · `/roles` |
| **Contrato** | [`openapi/identidad.yaml`](src/main/resources/openapi/identidad.yaml) |
| **Paquete** | `bo.aportaya.identidad` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-01 | Registro y apertura de billetera | **Implementado** |
| CU-02 | Revisar el expediente de identidad | **Implementado** |
| CU-04 | Autenticar con MFA y registrar dispositivo | **Implementado** |
| CU-07 | Foto de perfil propia: subir y ver. *El código la rotula CU-07; en la bóveda el CU-07 es «Ejercer derechos sobre datos personales» y lo implementa `auditoria`* | **Implementado** |
| CU-08 | Asignar y revocar roles de operador | **Implementado** |
| CU-09 | Cambiar credenciales y solicitar la baja | **Implementado** |
| CU-05 | Aceptar contrato de adhesión y tarifario | ⬜ sin implementar |

## Eventos que emite

| Tema | Cuando |
| --- | --- |
| | |

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| | | |

## Operaciones de servicio a servicio

| Ruta | Identidad exigida | Lo pide | Que hace |
| --- | --- | --- | --- |
| `POST /usuarios/{id}/titularidad/verificacion` | `DATOS_SENSIBLES_LEER` **o** el rol `SERVICIO_ENTREGAS` (`@PermisoAlternativo`) | `entregas` (CU-18, R-SEG-02) | Contesta SOLO `{coincide: true|false}`, nunca el nombre ni el documento. La lectura es de sistema, acotada a ese booleano. **Pendiente de Seguridad:** el modelo de roles (que otro rol de operacion puede verificar) y un limite de consultas por servicio |

## Trabajos programados

| Bloqueo | Cron | Que hace |
| --- | --- | --- |
| | | |

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:identidad:generateJooq
./gradlew :servicios:identidad:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.
