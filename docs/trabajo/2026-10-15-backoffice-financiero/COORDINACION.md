# Coordinación de los dos carriles en computadoras independientes

## Punto de partida

Repositorio: `https://github.com/PabloArauzCaballero/PasanakuBackend`. Rama de integración inicial: `codex/alta-atlas-sesion-premium-2026-10-11`. Esa rama contiene el trabajo local conservado y el merge de `origin/dev` del 2026-10-15. Antes de empezar, cada computadora debe hacer `git fetch origin` y comprobar el commit publicado de esta guía. El commit exacto puede avanzar al registrar estas instrucciones; usar el `HEAD` de la rama base al crear las dos ramas, nunca un clon antiguo de `dev`.

Ramas de trabajo independientes:

- `codex/backoffice-dominio-20261015`: carril A, esquema, servicios, OpenAPI, permisos, motor, auditoría y controles.
- `codex/backoffice-ui-20261015`: carril B, Angular, componentes, rutas, estados y textos.

Ambas ramas nacen del mismo commit integrado. Cada computadora debe trabajar solo en su rama. Los cambios compartidos de contratos se publican primero en A y se llevan a B por merge o cherry-pick explícito; evitar editar simultáneamente el mismo archivo. Los cambios de `dev` se integran regularmente en cada rama después de revisar diferencias; no hacer `push --force` sobre ramas compartidas.

## Interfaz de coordinación obligatoria

Por cada módulo, A entrega antes de integrar: OpenAPI versionado, permisos, estados/transiciones, filtros y paginación, errores, idempotencia, correlación y ejemplos sintéticos. B puede adelantar la UI con adaptadores tipados y fixtures marcados para desarrollo, sin presentarlos como datos reales. Los gates G1–G4 del `PLAN.md` juntan ambos carriles por flujo. Registrar cambios incompatibles de contrato antes de incorporarlos a B.

## Decisiones ya fijadas

- Decisiones de casos automáticas bajo política aprobada; revisión humana por excepción.
- Límites y matriz aún no aprobados: parámetros cerrados para operaciones sensibles hasta validación de negocio/Legal.
- Aprobación humana autenticada con MFA y evidencia auditable; no denominarla firma digital certificada.
- OCR futuro como adaptador de evidencia; no es dependencia de la primera entrega.

## Inicio en la otra computadora

`git clone` del repositorio, `git fetch origin`, `git switch --track origin/codex/backoffice-dominio-20261015` o `git switch --track origin/codex/backoffice-ui-20261015`. Leer `AGENTS.md`, `PLAN.md`, los documentos funcionales citados y los contratos reales antes de modificar código. No copiar archivos del ZIP de Descargas sobre el repositorio: la copia publicada en Git es la fuente compartida.

## Verificación de la base integrada

Ejecutada en Windows el 2026-10-15 después del merge de `origin/dev`:

- `corepack yarn workspace @aportaya/backoffice typecheck`: pasó.
- `corepack yarn workspace @aportaya/backoffice test:front`: 72 archivos, 479 pruebas, pasó.
- `flutter test` sobre cliente/errores/sesión/acceso a rutas: 28 pruebas, pasó.
- `flutter test` sobre centro de ayuda/verificación de sesión/deep links: 14 pruebas, pasó.
- `:servicios:identidad:integrationTest` para `CU01Test` y `CU02RevisionTest`: pasó con Docker Desktop activo. `compileJava` también pasó.
- `flutter test test/widget/hoja_de_seleccion_test.dart` en `packages/diseno_flutter`: 7 pruebas, pasó tras incorporar el ajuste local del selector y el teclado.

Estas verificaciones cubren el merge relevante, no equivalen a la suite completa ni a un despliegue. Cada carril debe ejecutar sus gates antes de integrar nuevas funciones.
