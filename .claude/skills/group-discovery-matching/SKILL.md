---
name: group-discovery-matching
description: Descubrimiento de grupos y conformación automática — qué se expone públicamente de un grupo y de una persona, postulaciones y criterios de emparejamiento como política configurable, formación de propuestas y su aceptación, reemplazo de un cupo vacante con candidatos, equidad y no discriminación del motor, y las reglas de privacidad y anti-abuso de cualquier superficie pública. Usar al construir la búsqueda o el listado público de grupos, el flujo de postulación, el motor de emparejamiento, la propuesta de grupo, o la búsqueda de un reemplazo.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Descubrimiento de grupos y emparejamiento

Que un desconocido pueda entrar a un grupo es lo que hace escalable a Pasanaku, y también lo que
lo vuelve atacable: cada superficie pública es un lugar donde se filtran datos, entran cuentas
falsas y se arman grupos que nadie va a pagar.

Servicio dueño: `grupos` (M2, RF-19). Entidades: `postulacion_emparejamiento`,
`criterio_emparejamiento`, `propuesta_grupo`, `candidato_reemplazo`.

## 1. Qué es público y qué no

| Del grupo | Público | Solo miembros |
|---|---|---|
| Nombre, monto, frecuencia, cupos, fecha de inicio | ✅ | |
| Reglamento vigente | ✅ (para decidir si entrar) | |
| Verificación del sorteo, una vez revelado | ✅ | |
| Totales y movimientos | | ✅ |
| Quiénes son los participantes | | ✅ (y con alias, si el reglamento lo dice) |
| Estado de cumplimiento individual | | Ver `transparency-reputation` §2 |

| De la persona | Público | Con consentimiento |
|---|---|---|
| Nada por defecto | ✅ | |
| Alias y antigüedad en la plataforma | | ✅ |
| Cantidad de grupos completados / certificado | | ✅ (`consent-management`) |
| Documento, teléfono, dirección, deuda, score exacto | **Nunca** | |

**Por defecto, nada de una persona es público.** Cada campo que se publica requiere
consentimiento específico, revocable, y la revocación tiene **efecto inmediato** — incluido
invalidar cachés y reindexar el buscador (`caching-strategy`).

## 2. Postulación y criterios

- `criterio_emparejamiento` es **política configurable y versionada**: monto, frecuencia, zona,
  nivel de verificación mínimo, antigüedad, tolerancia de riesgo.
- La postulación registra **qué busca** la persona, no la habilita a ver a los demás
  postulantes.
- Requisito mínimo no negociable: **nivel de verificación 2** para postularse a tomar un cupo
  (`kyc-identity-verification` §1). Un motor que arma grupos con cuentas sin verificar arma
  grupos que no van a pagar.
- El criterio de riesgo usa el score **como orientación**, y su uso está declarado: qué pesa,
  cuánto, y cómo entra alguien sin historial (`transparency-reputation` §4).

## 3. El motor propone; las personas deciden

```
postulaciones → motor → propuesta_grupo (con sus postulaciones vinculadas)
    → cada postulante acepta o rechaza, con plazo
        → completas las aceptaciones → grupo CONFORMADO
        → vencido el plazo → la propuesta cae y las postulaciones vuelven al pool
```

- **Nadie queda dentro de un grupo sin aceptar explícitamente**, y aceptar incluye aceptar el
  reglamento de esa propuesta (`rosca-group-lifecycle` §2).
- La propuesta muestra lo necesario para decidir: monto, frecuencia, cantidad de participantes,
  reglamento, y el perfil agregado del grupo. **No** la identidad completa de los demás antes de
  que el grupo exista.
- El motor es **determinista y reproducible**: mismas postulaciones y mismo criterio ⇒ misma
  propuesta. Si no, no podés explicar por qué a alguien no lo emparejaron.
- Toda propuesta queda registrada con el criterio y su versión.

## 4. Equidad del motor

Un motor de emparejamiento decide quién accede a crédito informal. Eso obliga a:

- **Criterios declarados y auditables.** Nada de heurísticas implícitas en el orden del `SELECT`.
- **Prohibido usar atributos protegidos** (origen, género, religión, edad fuera de lo legalmente
  necesario) ni sus proxies obvios.
- Revisar la **distribución de resultados**: si los postulantes nuevos nunca entran a ningún
  grupo, el motor los excluyó de hecho aunque ninguna regla lo diga.
- La persona puede saber **por qué no fue emparejada** en términos accionables ("falta
  verificación nivel 2", "no hay grupos de ese monto en tu zona"), sin exponer el detalle del
  antifraude.

## 5. Reemplazo de un cupo vacante

- `candidato_reemplazo` para un cupo que quedó libre (retiro, expulsión firme).
- El entrante **toma el cupo con su posición y su historia de obligaciones**, no un lugar nuevo
  (`rosca-group-lifecycle` §1): el calendario del grupo no se mueve.
- Requiere aceptación del reglamento vigente y, según el reglamento, acuerdo del grupo.
- Se le muestra al candidato **el estado real del cupo** (obligaciones pendientes, deuda si la
  hereda, turno que le toca). Ocultarlo es venderle un problema.

## 6. Anti-abuso de la superficie pública

Todo listado público es un objetivo (`content-moderation-abuse`, `security-guardrails`):

- **Rate limiting** en búsqueda, listado y postulación (`api-gateway-bff` §5).
- **Sin enumeración**: los identificadores públicos no permiten recorrer todos los grupos ni
  todos los usuarios; paginación por cursor sin ids secuenciales adivinables.
- **Anti-scraping**: límites por IP y por cuenta, y detección de patrones de recorrido masivo.
- Texto libre (nombre y descripción del grupo) **moderado**: es contenido de usuario visible
  para desconocidos, y es el lugar donde aparecen estafas con números de contacto.
- **Prohibido exponer contacto directo** entre desconocidos antes de que el grupo exista: es la
  vía por la que se sacan a la gente de la plataforma y arman el pasanaku por fuera, sin fondo
  de garantía y sin recurso ante un fraude.

## 7. SEO y páginas públicas

Si hay páginas públicas de grupos (`seo-public-pages`): sin datos personales en el HTML, sin
datos personales en la URL, `noindex` para todo lo que tenga información de participantes, y
revisión de qué queda en la caché de los buscadores. Lo que se indexa es **muy difícil de
despublicar**.

## Anti-patrones

- Publicar datos de una persona por defecto.
- Consentimiento revocado que sigue visible por caché o índice.
- Emparejar cuentas sin verificación nivel 2.
- Motor no determinista o con criterios implícitos.
- Usar atributos protegidos o sus proxies.
- Meter a alguien en un grupo sin aceptación explícita.
- Mostrar la identidad completa de los demás antes de que el grupo exista.
- Reemplazo que reordena el calendario.
- Candidato que entra sin saber la deuda del cupo.
- Listado público sin rate limiting ni anti-enumeración.
- Nombre de grupo libre y sin moderar.
- Exponer teléfonos entre desconocidos.

## Checklist

- [ ] Lo público está declarado campo por campo; por defecto nada de la persona.
- [ ] Consentimiento por campo, revocable, con efecto inmediato en caché e índices.
- [ ] Criterios de emparejamiento versionados y auditables.
- [ ] Nivel de verificación mínimo exigido para postularse.
- [ ] Motor determinista; propuesta registrada con criterio y versión.
- [ ] Aceptación explícita de cada postulante, con plazo y con reglamento.
- [ ] Sin atributos protegidos ni proxies; distribución de resultados revisada.
- [ ] Motivo accionable de no emparejamiento.
- [ ] Reemplazo conserva posición e informa el estado real del cupo.
- [ ] Rate limiting, anti-enumeración, anti-scraping y moderación de texto libre.
- [ ] Sin contacto directo entre desconocidos antes de conformar.

## Evidencia / DoD

1. Respuesta del endpoint público, pegada, mostrando que no expone datos personales.
2. Prueba de revocación de consentimiento: el dato desaparece del listado y del índice.
3. Motor ejecutado dos veces con las mismas entradas: misma propuesta.
4. Salida del intento de postularse sin verificación: rechazado con motivo accionable.
5. Salida de la prueba de rate limiting y de enumeración sobre el listado público.
