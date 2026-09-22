---
name: web-backoffice
description: "El comportamiento del backoffice web de AportaYa, en Angular: tablas densas paginadas del servidor con estado en la URL y exportación auditada, pantallas de cumplimiento con plazos y evidencia, segregación de funciones visible, permisos que ocultan pero no protegen, y estados obligatorios. Úsala al crear o modificar cualquier pantalla de apps/backoffice, junto con web-angular, que manda la forma del código."
---

# Backoffice web

Sus usuarios son el **oficial de cumplimiento, soporte y contabilidad**: gente
experta, jornada completa, pantallas densas, decisiones con consecuencia legal. No es
la app estirada.

El diseño visual lo manda `disenar-frontend`; la **forma del código Angular** la manda
`web-angular`; esta skill manda **el comportamiento del producto**: qué tiene que poder
hacer un operador y qué no puede pasar nunca. Stack: [[ADR-044 Frontend en Angular y Flutter]].

## Estructura

```
apps/backoffice/src/app/
├── nucleo/                  interceptores, sesión con rol y permisos, registro de acceso — del shell (F6)
├── layout/                  shell financiero y shell de sistemas — del shell (F6)
├── app.routes.ts            un `loadChildren` por dominio — del shell, congelado
└── rutas/<dominio>/         operacion · cumplimiento · sistemas · contabilidad · publicidad
    ├── <dominio>.routes.ts  las rutas del carril, con guardias `canMatch` por permiso
    ├── dominio/             un archivo por caso de uso sobre clientes/angular (httpResource / mutaciones)
    ├── textos.ts            textos del dominio, en voz de marca
    └── <pagina>/            un componente por página: compone organismos de @aportaya/ui, sin lógica
```

Angular Router con carga perezosa por dominio; `httpResource` y señales para el estado
de servidor; `TablaDeDatos` de `@aportaya/ui` sobre el CDK. Ningún componente inyecta
`HttpClient`.

## Tablas: el organismo central

Casi todo el backoffice es una tabla con filtros. Reglas fijas:

| Aspecto | Regla |
| --- | --- |
| Paginación | Del servidor, siempre. Nada de traer todo y filtrar en el cliente |
| Orden y filtros | Por lista blanca acordada con la API; el estado va en la URL |
| Virtualización | Para listas largas; el DOM no crece sin límite |
| Columnas de dinero | Alineadas a la derecha, con el átomo `Monto`, moneda visible |
| Fechas | Con zona horaria explícita; nada de fechas ambiguas en un expediente |
| Exportación | Del servidor, con los mismos filtros aplicados, y **queda registrada**: quién exportó qué y cuándo |
| Selección múltiple | Solo si existe una acción masiva real, y con confirmación que enumera lo afectado |

El estado de la tabla vive en la URL: un oficial tiene que poder pegar el enlace de lo
que está mirando en un expediente.

## Pantallas de cumplimiento

Lo que distingue a este producto de un CRUD:

- **El plazo se muestra siempre.** Un reclamo (CU-52) o un reporte (CU-43) exhibe
  cuánto falta, y el vencimiento se destaca visualmente antes de vencer, no después.
- **La evidencia se ve.** Toda pantalla de expediente muestra la bitácora: quién hizo
  qué, cuándo, con qué resultado. Es lo que se responde ante una inspección.
- **Nada se edita.** Corregir es registrar un movimiento o una decisión nueva, con
  motivo obligatorio. Si la interfaz sugiere "editar", está mintiendo sobre el modelo.
- **Motivo obligatorio** en toda acción con consecuencia: bloquear saldo, rechazar,
  elevar, devolver comisión.
- **Doble confirmación** en lo irreversible, enumerando exactamente qué va a pasar.

## Permisos

- La interfaz oculta o deshabilita por comodidad; **la protección real es del
  servidor** y de las políticas de fila.
- Una pantalla vacía por permiso lo dice explícitamente: "no tenés acceso a esto", no
  una tabla vacía que parece un error de datos.
- La segregación de funciones del modelo se respeta en la interfaz: quien registra no
  aprueba. Si la interfaz permite ambas cosas al mismo usuario, es un defecto.

## Estados obligatorios

Cargando, vacío, error y éxito en toda vista con datos; **enviando** en toda acción.
En un backoffice el estado vacío importa el doble: "no hay alertas" y "el filtro no
devolvió nada" son mensajes distintos y se distinguen.

## Formularios

- Signal Forms con los **tipos generados desde el OpenAPI del servicio** y validadores
  derivados del contrato; nunca reglas reescritas a mano.
- Errores por campo, en el campo, diciendo cómo corregir.
- Clave de idempotencia en toda operación con efecto.
- Los formularios largos (reportes, expedientes) guardan borrador local para no
  perder trabajo por una sesión caída.

## Rendimiento

- Consultas con caché e invalidación **explícita** por acción; nada de recargar todo
  tras cada cambio.
- Las pantallas de conciliación y reportes leen de la réplica: son consultas pesadas.
- Se optimiza con medición, no por intuición.

## Antipatrones

- Traer diez mil filas para filtrar en el navegador.
- Un botón "editar" sobre algo append-only.
- Acción masiva sin confirmación que enumere lo afectado.
- Exportar sin registrar quién exportó.
- Ocultar un permiso solo en el cliente y asumir que eso protege.
- Reutilizar el layout de la app móvil para pantallas densas.

## Ver también

`web-angular` · `errores-api` · `observabilidad` · `glosario-dominio` · `disenar-frontend` · `arquitectura-atomica` · `contratos-api` · `revision-codigo` ·
`seguridad-aplicacion` · [[Seguridad]] ·
[[ADR-038 Acceso administrativo · segundo factor y recuperación asistida]] — el acceso al backoffice
(ingreso, desafío TOTP, enrolamiento, recuperación, aprobaciones) está en
[[Flujo de pantallas · backoffice administrador]] §2.0 ·
[[ADR-044 Frontend en Angular y Flutter]] · `docs/Arquitectura/Prompts/Prompt de frontend.md`
