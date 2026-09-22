# Mutation testing (PIT) — H6.S1 del carril PR4-seguridad

> **Estado: TODO / BLOQUEADO por presupuesto de tiempo y entorno**, declarado sin
> rodeos. No se llegó a instalar el plugin PIT ni a correr `pitest` en esta corrida.

## Por qué no se llegó

El orden del encargo es H1 → H6 (regla del propio documento: nunca saltear al final
antes de cerrar lo anterior con evidencia). H1 (idempotencia + webhook) y H2
(inventario + contratos) quedaron con evidencia real de ejecución de scripts
(`verificar_seguridad.py`, `inventario_endpoints.py`, `verificar_contratos_limites.py`,
los tres con salida literal pegada), pero la ejecución de la suite Java de `aportes`
(`webTest`/`integrationTest`) contra un entorno Docker Desktop compartido con otros
cuatro carriles en la misma máquina fue severamente inestable durante buena parte
del turno (`java.io.IOException: Input/output error` reproducible del propio
Gradle al escribir su caché de locks, y una copia simple del repositorio a través
del bind mount de Windows tardando de 25 a más de 30 minutos). Abrir además PIT
—que requiere compilar, instrumentar bytecode y correr la suite completa varias
veces— sobre ese mismo entorno inestable, antes de H3/H4/H5, hubiera significado
gastar el resto del presupuesto del turno sin ninguna de las seis hitos con
evidencia sólida.

## Lo que se dejó listo para retomar

1. **Plugin verificado en el catálogo, no instalado todavía**: `info.solidsoft.pitest`
   no está en `gradle/libs.versions.toml` ni en `buildSrc/`. Falta el micro-PR que
   el encargo pide (H6.S1.M1): agregar el plugin a una convención opcional
   (`aportaya.mutacion`) en `buildSrc/`, coordinada con Leo (dueño de `buildSrc`) y
   Pablo (`libs.versions.toml`) — como en este turno soy la única persona activa,
   la nota queda para que el próximo turno la aplique o para que yo la retome.
2. **Clases objetivo confirmadas por nombre real** (H10.S4.M1 del plan madre): de
   las siete que el plan madre nombra —`Dinero`, `CostoDeOperacion`,
   `CondicionesDeRetiro`, `EstadoDeRetiro`, `Idempotencia`, `SegundoFactorStepUp`,
   `EmisorDeEvidencia`— **ninguna vive en `aportes`** (todas son de
   `nucleo-financiero`, `comun-web` o `identidad`, fuera del alcance de escritura de
   este carril). Lo que SÍ es candidato de mutation testing en `aportes` de este
   carril: `VerificadorDeFirmaWebhook` (dominio puro, sin Spring, ya con 8 tests
   unitarios dirigidos — candidato ideal para medir cobertura de mutantes de una
   pieza de seguridad crítica) y `CU100RecibirWebhookPasarela` (con 8 tests de
   integración en tres niveles).

## Siguiente paso concreto para quien retome

```kotlin
// buildSrc/src/main/kotlin/aportaya.mutacion.gradle.kts (NUEVO, convención opcional)
plugins {
    id("info.solidsoft.pitest") version "<verificar version compatible con Gradle 9.7 y JUnit 5>"
}
pitest {
    targetClasses.set(setOf("bo.aportaya.aportes.dominio.*"))
    junit5PluginVersion.set("1.2.1")
}
```

```bash
./gradlew :servicios:aportes:pitest
```

Score por clase y mutantes sobrevivientes: pendiente de la primera corrida real.
