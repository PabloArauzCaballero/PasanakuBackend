---
tags:
  - arquitectura
  - adr
  - frontend
  - diseño
titulo: "ADR-053 — Rediseño bancario de la app"
estado: aceptada
fecha: 2026-10-21
enmienda: [ADR-044]
---

# ADR-053 — Rediseño bancario de la app

> Cambia **cómo se ve y cómo se mueve** la app del participante (Flutter), no cómo funciona.
> El stack de [[ADR-044 Frontend en Angular y Flutter]] sigue igual. Agrega **una**
> dependencia de interfaz (íconos Lucide) y cambia el valor de varios roles de color
> compartidos con la web. Plan y evidencia: `docs/trabajo/2026-10-21-rediseno-bancario/`.

## Contexto

La revisión del «antes» (`evidencia/revision-antes.md`: 1.344 capturas de 46 pantallas en
3 anchos y 2 temas) encontró que la app es correcta pero se ve genérica:

- Nada es protagonista: fondo gris plano, tarjetas blancas con borde y sombras casi nulas.
- El saldo es texto suelto.
- El naranja es el color de toda acción.
- Hay ~185 íconos de Material mezclados (rellenos, contorneados y redondeados).
- Las cabeceras son el `AppBar` de fábrica y los botones apagados se leen como texto.

La persona usuaria pidió que la app se vea como sus apps bancarias hermanas (Meru, Nubank,
Revolut) y delegó las cuatro decisiones abiertas en «lo más recomendado» (2026-10-21).

## Decisión

1. **Dirección «Banca Verde Profunda»** (lámina `evidencia/direccion/lamina.html`):
   - Tarjeta héroe de saldo con degradado g600 → g900.
   - Acciones circulares.
   - Superficies sin borde con sombra en dos capas.
   - Barra de pestañas flotante de vidrio.
   - Cabeceras grandes.
2. **Color por rol**:
   - El **verde de marca es la acción principal**; el **naranja queda solo para la urgencia de
     pago** (aportar, vencimientos).
   - Roles nuevos: `heroInicio`, `heroFin`, `heroBrillo`, `heroTexto`, `heroTexto2`, `halo`,
     `vidrio`, `vidrioBorde` y `sombraHeroe`. La urgencia **reusa** los roles `accent`,
     `accentInk` y `accentTexto` (ya naranja y medidos); no se crea un juego paralelo.
   - Fondo claro `#F0F4F1`: el `#EEF2EF` de la lámina dejaba el texto bajo el velo del
     tutorial en 4,47:1 (test `tutoriales_a11y`). `accentTexto` pasa a `#9C5412` (5,13:1).
   - Cada par se mide en claro y oscuro (≥ 4,5:1 para texto). Los 15 pares de la lámina pasan.
3. **Tipografía**: Bricolage (ya empaquetada y sin uso) para cifras y títulos de pantalla;
   Instrument para el texto; cifras tabulares.
4. **Íconos: Lucide** con `lucide_icons_flutter` **fijado en `3.1.20`**:
   - Publicado el 2026-09-15, licencia ISC, unas 290 mil descargas por mes.
   - API `IconData`, compatible con `Icon`, así que los átomos no cambian de firma.
   - Un mapa único `IconosDeMarca` en `diseno_flutter` nombra cada ícono por su uso; las
     pantallas no importan el paquete directo.
5. **Navegación**: pestaña central «QR» (Inicio · Grupos · QR · Movimientos · Perfil). Ayuda
   pasa al Perfil y a la cabecera.
6. **Movimiento**:
   - Resortes críticamente amortiguados por defecto, con rebote solo tras un gesto con
     impulso (`apple-design`).
   - Nada disparado por la persona supera 320 ms (`pasanaku-movimiento`).
   - Todo respeta «reducir movimiento».

## Motivo

La persona usuaria pidió que la app se vea como sus apps bancarias hermanas y delegó las cuatro
decisiones abiertas en «lo más recomendado» (2026-10-21). La revisión del «antes» mostró una app
correcta pero genérica, así que el cambio es de **forma**, no de función: se separa lo que se puede
hacer (verde) de lo que hay que hacer ya (naranja de urgencia de pago) y se da protagonismo al saldo.

Lucide se eligió porque conserva la API `IconData` (los átomos no cambian de firma) y porque su
mantenimiento cumple la regla 90.4.2, que descartó `phosphor_flutter`. Los pares de color se
**midieron** en claro y oscuro en vez de copiarse de la lámina: por eso el fondo claro y
`accentTexto` difieren de ella.

> Las secciones «Motivo» y «Cómo se verifica» se redactaron el 2026-10-29, después de aceptada la
> decisión, solo con lo que este ADR y los reportes citados ya dicen. No cambian la decisión.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Mantener `Icons.*` de Material y solo unificar variantes | El trazo fino se pierde a 360 dp y el set no tiene el aire «fintech» pedido |
| `phosphor_flutter` | Sin publicar desde 2024-05: riesgo de mantenimiento (regla 90.4.2) |
| `hugeicons` | API que no es `IconData`: obligaba a cambiar la firma de `BotonIcono` y de los átomos con ícono |
| Naranja como color de toda acción (estado previo) | No distingue la urgencia de pagar de una acción común |
| Roles nuevos solo para la app, sin tocar la web | La persona usuaria permitió que el cambio alcance a la web; dos sistemas de color divergentes es deuda |

## Consecuencias

- **Web y backoffice**: comparten `tokens.css`. El cambio de valor de roles existentes se
  captura antes y después (H2.S5.M1, H8.S3.M2).
- **Goldens**: se regeneran todas las bases (Linux en CI; macOS con `goldens-macos.yml`).
- **Dependencia nueva**: `lucide_icons_flutter 3.1.20` en `packages/diseno_flutter`; el
  lockfile se versiona. Sube de versión solo por decisión explícita.
- **Sin cambios** en dominio, providers, contratos, sesión ni cálculos de dinero.

## Cómo se verifica

- [x] Análisis estático de la app y del paquete de diseño: `dart analyze --fatal-infos` →
      «No issues found» en `movil` y `diseno`
      (`docs/trabajo/2026-10-29-rediseno-bancario-al-repo-desplegado/REPORTE.md`, H1.M2).
- [x] Pruebas, con accesibilidad incluida (`tutoriales_a11y`): `flutter test` → `diseno` +128 ·
      `movil` +828 (mismo reporte, H1.M3).
- [x] Goldens regenerados con Flutter 3.44.8 y mirados: `flutter test test/goldens` → `diseno` +18 ·
      `movil` +274 (mismo reporte, H1.M4).
- [x] Tanda 2 (pantallas de trámite): `verificar_frontend.py movil/diseno` → TODO OK; goldens
      `diseno` +149 · `movil` +274
      (`docs/trabajo/2026-10-29-rediseno-bancario-tanda-2/REPORTE.md`, T2.M4 y T2.M5).
- [ ] Dispositivo real: no se ejercitó (tanda 2, «No cubierto»).
- [ ] Web y backoffice con los valores nuevos de los roles compartidos: la captura antes/después
      (H2.S5.M1, H8.S3.M2 de `docs/trabajo/2026-10-21-rediseno-bancario/`) no se volvió a
      ejecutar al redactar esta sección.
