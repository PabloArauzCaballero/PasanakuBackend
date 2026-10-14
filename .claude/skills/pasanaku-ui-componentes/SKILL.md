---
name: pasanaku-ui-componentes
description: Cómo se organizan y se reutilizan los componentes en la app Flutter de AportaYa — qué va en packages/diseno_flutter (átomos, moléculas, organismos, móviles) y qué en una pantalla de apps/movil, buscar antes de crear, el contrato de un componente (parámetros semánticos, estados, accesibilidad, 48 dp), partir lo que trae datos de lo que pinta, y el límite de 200 líneas. Usar antes de escribir cualquier widget nuevo, al revisar un PR que agrega uno, o cuando dos widgets hacen casi lo mismo. Traduce al stack real la skill de Atlas `atlas-ui-componentes`.
---

# Componentes en AportaYa (Flutter)

## 1. Primero: buscar. Casi siempre ya existe

`packages/diseno_flutter/lib/` tiene el inventario (ver la lista completa en `pasanaku-diseno` §3) y
un catálogo vivo en `lib/catalogo/` (widgetbook). Antes de crear:

```bash
git grep -n "class .* extends \(StatelessWidget\|StatefulWidget\)" packages/diseno_flutter/lib | grep -i "<lo que buscás>"
```

**Si algo «casi» sirve, se parametriza; no se copia.** Copiar una pantalla y tocarle dos estilos es
cómo se termina con veinte cabeceras distintas y pantallas sin botón de volver.

## 2. Qué va en el sistema de diseño y qué en la pantalla

- **El sistema de diseño decide cómo se ve y cómo se comporta un control**: color, espaciado, estados
  (presionado, foco, deshabilitado con motivo, cargando, error, éxito), movimiento, semántica.
- **La pantalla decide qué datos entran y qué pasa al tocar.** Una pantalla que escribe `color:`,
  `BorderRadius.circular(12)` o `TextStyle(fontSize:)` es un átomo que falta.
- **Traer datos y pintar son dos trabajos.** El provider (Riverpod) trae y transforma; el widget pinta
  lo que recibe. Un widget que lee `dioProvider` está haciendo los dos (`smart-dumb-components`).
- `packages/diseno_flutter` **no conoce** el dominio de AportaYa (no importa `apps/movil`): recibe
  textos, valores y callbacks.

## 3. El contrato de un componente

- **Parámetros semánticos, no de estilo.** `variante: BotonVariante.peligro`, `tono: Tono.aviso`,
  `expandido: true`; nada de `color:` ni `margin:` como parámetro. Si hace falta, lo que falta es una
  variante.
- **Los estados no son un anexo.** Todo control interactivo trae `habilitado`/`cargando` y, si se puede
  apagar, el **motivo** (`motivoDeBloqueo`). Toda vista con datos trae vacío, cargando, error y sin
  permiso.
- **Accesibilidad es parte de la firma**: `Semantics(button:, label:, value:, hint:)`, foco visible,
  área táctil ≥ `Tactil.minimo` (48). Un ícono sin texto lleva `tooltip` y etiqueta.
- **Una opción única no es una elección**: un selector, segmentado o grupo de radio con una sola opción
  se dibuja como texto (assert en debug).
- **Cambiar la firma de un átomo compartido es un barrido**: `git grep -n "NombreDelAtomo("` en
  `apps/` y `packages/`, ajustar **todos** los usos y correr las pruebas del paquete y de la app (en
  Atlas, cambiar `Select` dejó 21 selects rotos con el analizador casi verde).
- **200 líneas por archivo** en `apps/movil/lib` (gate): textos, dominio, widget y pintor en archivos
  separados.

## 4. Un componente nuevo, paso a paso

1. Buscarlo (y en el catálogo). Si «casi» existe, parametrizar.
2. Decidir dónde va: genérico ⇒ `diseno_flutter` (átomo/molécula/organismo/móvil); propio de un
   dominio ⇒ `apps/movil/lib/pantallas/<dominio>/pasos_*` o similar.
3. Escribir la **prueba de widget primero**: sus estados y su semántica.
4. Escribirlo con tokens (`pasanaku-diseno`) y movimiento de `Movimiento` (`pasanaku-movimiento`).
5. Agregarlo al catálogo (`catalogo/muestras.dart`) si es del sistema de diseño, y al golden si
   corresponde.
6. Capturas por ancho y tema (`visual-proof`) y rúbrica (`ui-quality-review`).

## 5. Piezas de identidad que existen en `apps/movil/lib/pantallas/identidad/`

`pasos_alta/`: `campos_del_alta`, `paso_datos`, `paso_contrasena`, `paso_celular` (confirma el
correo), `paso_capturas`, `riel_de_capturas`, `tarjeta_de_captura`, `pie_de_capturas`,
`sacar_captura`, `guia_de_encuadre`, `consejos_para_la_foto`, `hoja_antes_de_escanear`,
`aviso_de_permiso_camara`, `cabecera_de_camara`, `pie_de_prueba_de_vida`, `canal_de_verificacion`,
`catalogo_del_perfil`, `campo_de_actividad`, `campo_de_detalle`, `departamentos`, `paso_cotejo`,
`paso_perfil_transaccional`. Dominio: `estado_alta`, `datos_del_alta`, `capturas_del_expediente`,
`juez_de_pose`, `calidad_de_captura`, `envio_del_alta`, `subida_del_expediente`,
`seguimiento_del_alta`, `validaciones`. Reusalas antes de escribir otra.

## Checklist

- [ ] Busqué antes de crear; no hay otro widget que haga casi lo mismo.
- [ ] Sus parámetros son semánticos; no acepta colores ni márgenes desde afuera.
- [ ] Tiene vacío, cargando, error y deshabilitado con motivo; no solo el caso feliz.
- [ ] Semántica, foco y 48 dp.
- [ ] Si cambié una firma compartida, revisé todos los usos y corrí las dos suites.
- [ ] Ningún archivo pasa de 200 líneas.
