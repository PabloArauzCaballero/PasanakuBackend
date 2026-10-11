---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
codigo: CU-129
criticidad: media
actores: [Usuario]
normas: [Protección del inversor, Separación de patrimonios]
---

# CU-129 — Simular la caja de una inversión sin tocar saldos

> **Objetivo.** Poder **comparar** qué pasaría con un importe hipotético si se dejara en la
> billetera, en un DPF o en un fondo, **sin leer ni mover ningún saldo**. La inversión de dinero de
> grupos sigue **desactivada**: esta simulación es una cuenta separada, no una puerta de atrás.

> [!warning] Parámetros SINTÉTICOS y sin promesa de rentabilidad
> Las condiciones de los productos son sintéticas (`DR-INV-02`, `DR-INV-03`). Un fondo no tiene tasa
> fija: los escenarios de variación del valor de cuota los pone quien simula y **no son una
> predicción**.

## Actores y disparador

- **Actor principal:** el usuario (`BILLETERA_VER`).
- **Disparador:** `POST /inversiones/simulaciones/caja` con un importe, un horizonte en días y,
  opcionalmente, los productos a comparar y los escenarios de fondo.

## Precondiciones

1. Importe mayor que cero, en bolivianos.
2. Horizonte de 1 a 3650 días.

## Flujo principal

1. Se leen las condiciones **vigentes** del catálogo (en producción, solo las aptas para producción).
2. **Billetera:** el importe se conserva y está disponible hoy.
3. **DPF:** si el horizonte llega al plazo, paga interés simple (`importe × tasa × días / base`, a
   centavos) menos la retención y está disponible al vencimiento. Si no llega: con cancelación
   anticipada se calcula la cancelación con la **penalización** del contrato; sin ella, el DPF
   figura como **no disponible en el horizonte** (con lo que pagaría al vencimiento).
4. **Fondo:** por cada escenario (por omisión −10 %, 0 % y +10 %) se calcula el valor, la comisión
   por resultado **solo sobre el beneficio** y el neto; una caída muestra la pérdida, sin comisión.
5. Cada alternativa dice cuántos días faltan para disponer, si cae dentro del horizonte, lo que
   llegaría a la billetera y la diferencia contra dejarlo ahí.
6. La respuesta lleva `usaSaldoReal: false` y la advertencia de que invertir puede generar pérdidas.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 3a | DPF sin cancelación anticipada y horizonte menor al plazo | `DPF_NO_DISPONIBLE` |
| 3b | DPF con cancelación anticipada | `DPF_ANTICIPADO` con penalización |
| 4a | Escenario de caída | Pérdida visible, sin comisión |
| — | Importe cero o en otra moneda | `AP-CU129-01` |
| — | Horizonte fuera de rango | `AP-CU129-02` |
| — | Un producto que no existe o no se ofrece | `AP-CU129-03` |
| — | Más de 9 escenarios, o uno por debajo de −100 % | `AP-CU129-04` |

## Postcondiciones

- No se leyó el saldo de nadie, no se retuvo ni se ordenó nada, no se escribió nada.
- Un saldo afectado a un pozo sigue sin poder invertirse (`AP-CU121-05`); simular no lo reactiva.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU129 = z.object({
  monto: MontoSchema,
  horizonteDias: z.number().int().min(1).max(3650),
  productos: z.array(z.string().max(40)).max(20).optional(),
  variacionesDeFondo: z.array(z.string().regex(/^-?\d{1,2}\.\d{2}$/)).max(9).optional(),
}).strict()

export const SalidaCU129 = z.object({
  monto: MontoSchema,
  horizonteDias: z.number().int(),
  alternativas: z.array(z.object({
    tipo: z.enum(['BILLETERA', 'DPF_AL_VENCIMIENTO', 'DPF_ANTICIPADO', 'DPF_NO_DISPONIBLE', 'FONDO_ESCENARIO']),
    productoCodigo: z.string().optional(),
    descripcion: z.string(),
    diasParaDisponer: z.number().int(),
    disponibleEnElHorizonte: z.boolean(),
    interes: MontoSchema,
    retencion: MontoSchema,
    comision: MontoSchema,
    netoCuandoDisponible: MontoSchema,
    diferenciaContraBilletera: MontoSchema,
    origenDatos: z.enum(['SINTETICO', 'VERIFICADO']),
  }).strict()),
  usaSaldoReal: z.literal(false),
  advertencia: z.string(),
}).strict()

export const ErroresCU129 = {
  IMPORTE_INVALIDO: 'AP-CU129-01',
  HORIZONTE_INVALIDO: 'AP-CU129-02',
  PRODUCTO_NO_DISPONIBLE: 'AP-CU129-03',
  ESCENARIOS_INVALIDOS: 'AP-CU129-04',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `IMPORTE_INVALIDO` | El importe es cero, negativo o no está en bolivianos |
| `HORIZONTE_INVALIDO` | El horizonte no va de 1 a 3650 días |
| `PRODUCTO_NO_DISPONIBLE` | Un código pedido no existe o no se ofrece en este entorno |
| `ESCENARIOS_INVALIDOS` | Más de 9 escenarios, o alguno menor o igual a −100 % o mayor a +1000 % |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `DevengoDeDpf` | Interés simple y retención con un único redondeo |
| Átomo | `SimulacionDeCaja` | Las alternativas de un importe hipotético; cálculo puro |
| Organismo | `CU129SimularCaja` | Valida, lee las condiciones vigentes y arma la comparación; no tiene el puerto del libro |
| Página | `POST /inversiones/simulaciones/caja` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| — | — | `BILLETERA_VER` |

## Interfaz

- **App:** comparador de caja en la pantalla de inversiones, con la advertencia visible y las
  alternativas no disponibles en el horizonte señaladas. Pendiente de especificar.

## Restricciones aplicables

Ninguna propia: no escribe en la base. La separación del dinero de grupos la sostiene
[[CU-121 Aceptar las condiciones y ordenar una inversión]] (`AP-CU121-05`).

## Evidencia que deja

Ninguna: no persiste nada. Lee `producto_inversion` y `version_condiciones`.

## Criterios de aceptación

```gherkin
Dado un importe hipotetico y un horizonte
Cuando se simula
Entonces compara billetera, DPF y fondo sin consultar ni mover ningun saldo

Dado un DPF que no admite cancelacion anticipada y un horizonte menor al plazo
Cuando se simula
Entonces el DPF figura como no disponible en el horizonte

Dado un DPF que si admite la cancelacion y un horizonte menor al plazo
Cuando se simula
Entonces se calcula la cancelacion con la penalizacion del contrato

Dado un fondo y escenarios de caida y de subida
Cuando se simula
Entonces la caida muestra perdida sin comision y la subida cobra exito solo sobre el beneficio

Dado un saldo afectado a un pozo
Cuando se simula la inversion de ese importe
Entonces la simulacion no lo lee ni reactiva la inversion de grupos, y la orden real sigue rechazada con AP-CU121-05
```

Además: la prueba de cuadre de cada alternativa (neto = importe + interés − retención − comisión) y el
rechazo de las entradas inválidas con su código.

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-02 | Tasas, plazos, penalización y costos reales de cada producto | Aliado + producto |
| DR-INV-03 | Retención fiscal real de los intereses | Fiscal |

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-124 Solicitar el rescate de una inversión]]
