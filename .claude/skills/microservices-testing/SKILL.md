---
name: microservices-testing
description: Cómo se prueba un sistema distribuido sin un E2E gigante e inútil — la pirámide distribuida (unitario, componente con dependencias dobladas, contrato, integración real, E2E acotado), tests de componente que levantan el servicio contra base real con los vecinos stubbeados, pruebas de duplicado y de fallo de dependido, pruebas de saga rota a la mitad, datos de prueba entre servicios y por qué un E2E verde no prueba la resiliencia. Usar al definir qué tests escribir para un servicio o un flujo que cruza servicios, cuando la suite E2E es lenta e intermitente, o al verificar un cambio que toca dos o más servicios.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Testing de microservicios

El error clásico al partir un monolito: mantener la misma estrategia de tests y compensar con un
E2E que levanta los diez servicios. Resultado: una suite lenta, intermitente, que nadie mira y
que igual **no prueba lo único nuevo que trajo el corte** — que las llamadas fallan, se duplican
y llegan fuera de orden.

## 1. La pirámide distribuida

| Nivel | Qué levanta | Qué prueba | Cuántos |
|---|---|---|---|
| **Unitario** | Nada | Reglas de negocio puras: cálculo de deducción, transición de estado | Muchos |
| **Componente** | **Un** servicio + su base real; vecinos stubbeados | El servicio cumple su contrato de punta a punta | Muchos |
| **Contrato** | Nada (pactos) | Que productor y consumidor sigan de acuerdo | Uno por par |
| **Integración real** | Dos o tres servicios reales + broker | Que el flujo cruce de verdad, con la cola en el medio | Pocos |
| **E2E** | Todo | Un puñado de recorridos críticos de usuario | Muy pocos |

La inversión va al **nivel de componente**: es donde se prueba casi todo, rápido y sin
intermitencia. El E2E cubre los caminos de dinero de punta a punta y nada más.

## 2. Test de componente: el caballo de batalla

```
[ tu servicio real ] ──▶ [ PostgreSQL real, efímero ]
        │
        ├──▶ stub HTTP de `identidad`  (contrato = el pacto, no lo que se te ocurra)
        └──▶ broker real, cola efímera
```

Reglas:

1. **Base real, no mockeada.** El ORM, los constraints y los índices únicos son parte de lo que
   estás probando — sobre todo los de idempotencia (regla 91.2.4).
2. **Los vecinos se stubbean contra su pacto**, no contra lo que imaginás. Un stub que devuelve
   algo que el servicio real nunca devolvería es un test que miente
   (`service-contracts-versioning`).
3. El stub puede **fallar y tardar**: la mitad del valor está en probar timeout, 500 y lentitud.
4. Datos preparados por la API del propio servicio cuando se puede; si no, por fábrica declarada
   (`test-data-management`).
5. Cada test deja la base como la encontró, o usa base efímera por corrida.

## 3. Los cuatro tests que solo existen en distribuido

Ninguno de estos existía en el monolito, y son los que atrapan los bugs caros:

### 3.1 Duplicado

```ts
await consumidor.procesar(mensaje);
await consumidor.procesar(mensaje);        // el mismo message_id
expect(await contarAsientos(obligacionId)).toBe(1);   // efecto único
```

Obligatorio para **todo** consumidor. Sin este test, la idempotencia es una intención.

### 3.2 Dependido caído y dependido lento

```ts
stubGarantia.down();                 // conexión rechazada
await expect(entregas.liquidar(id)).rejects.toMatchObject({ code: 'DEPENDENCIA_NO_DISPONIBLE' });

stubGarantia.delay(5_000);           // más lento que el timeout
const t0 = Date.now();
await expect(entregas.liquidar(id)).rejects.toThrow();
expect(Date.now() - t0).toBeLessThan(1_000);    // cortó en el timeout, no esperó 5 s
```

### 3.3 Fuera de orden

```ts
await consumidor.procesar(eventoVersion2);
await consumidor.procesar(eventoVersion1);   // llega tarde
expect(await estado(cupoId)).toBe('AL_DIA');  // el viejo no pisó al nuevo
```

### 3.4 Saga rota a la mitad

Un test por paso: falla el paso N, se verifica que compensó los N-1 anteriores y que el sistema
quedó consistente (`saga-distributed-transactions` §7). Incluye matar el orquestador entre
"guardar estado" y "disparar paso".

## 4. Qué NO probar con E2E

| Tentación | Dónde va realmente |
|---|---|
| Validación de cada campo de un formulario | Unitario / componente |
| Matriz de autorización completa | Test de API del servicio (`api-testing`) |
| Que el evento tenga los campos correctos | Test de contrato |
| Cada rama de cálculo de deducciones | Unitario |
| Que el panel muestre el score | Componente del BFF con stub |

Al E2E le quedan: registrarse y entrar a un grupo · pagar un aporte y verlo acreditado · liquidar
y recibir una entrega · declarar un incumplimiento y ver la cobertura. Pocos, estables, con datos
propios y ejecutados en serie (`e2e-playwright`, `agent-resource-control`).

## 5. Entorno de pruebas

- **Compose de desarrollo** con los servicios necesarios y sus dependencias
  (`docker-local-stack`). Levantar los diez para correr un test de `pagos` es una pérdida.
- Cada servicio arranca con **su** esquema migrado desde cero; nada de una base compartida
  precargada que nadie sabe cómo se generó.
- **Datos sintéticos siempre** (`synthetic-test-data-generation`). Prohibido copiar datos de
  producción (regla 90.2.5).
- Los identificadores cruzados (un `cupo_id` que `pagos` necesita y `grupos` es quien crea) se
  obtienen llamando a la API del dueño en el arranque del test, no hardcodeados: si el contrato
  cambia, el test tiene que enterarse.

## 6. Verde no alcanza

Regla 98.8: un cambio que cruza servicios **no se cierra con un E2E verde**. El peldaño
`VERIFIED` exige haber roto algo:

- [ ] Duplicado probado.
- [ ] Dependido caído probado.
- [ ] Dependido lento probado.
- [ ] Saga compensando probada (si hay saga).
- [ ] Contrato verificado de los dos lados.

Todo con salida pegada (`evidence-and-verification`).

## Anti-patrones

- E2E gigante como única red de seguridad.
- Mockear la base en el test de componente: probás tu código contra tu imaginación.
- Stub que no respeta el pacto del servicio real.
- Test que solo prueba el camino feliz de un flujo distribuido.
- Consumidor sin test de duplicado.
- `sleep(2000)` en vez de esperar una condición: intermitencia garantizada
  (`e2e-failure-triage`).
- Tests que dependen del orden entre ellos o de datos que dejó otro.
- Compartir una base de pruebas entre servicios y entre desarrolladores.
- Declarar "probado" cuando lo que se probó fue un mock del backend (regla 1.5 de AGENTS.md).

## Checklist

- [ ] Las reglas de negocio están cubiertas por unitarios, no por E2E.
- [ ] Hay tests de componente con base real y vecinos stubbeados contra el pacto.
- [ ] Todo consumidor tiene test de duplicado.
- [ ] Hay test de dependido caído y de dependido lento.
- [ ] Hay test de evento fuera de orden donde el orden importa.
- [ ] Cada paso de saga tiene su test de compensación.
- [ ] Pactos de contrato corriendo en CI de los dos lados.
- [ ] E2E limitado a los recorridos críticos, estable y con datos propios.
- [ ] Datos sintéticos; nada de producción.

## Evidencia / DoD

1. Salida del runner por nivel (unitario, componente, contrato, integración, E2E).
2. Salida del test de duplicado, mostrando el efecto único.
3. Salida de los tests con el dependido apagado y lento, con tiempos.
4. Salida de los tests de compensación de saga, uno por paso.
5. Lista de qué **no** quedó cubierto, en la sección "No cubierto" del reporte.
