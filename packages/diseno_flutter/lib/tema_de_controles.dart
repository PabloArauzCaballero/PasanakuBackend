import 'package:flutter/material.dart';

import 'tokens/tokens.dart';

/// Los tres botones de Material, con el mismo alto de toque y la forma de píldora del
/// rediseño bancario (ADR-053). Separado de `tema.dart` por el barrido de 200 líneas.
ButtonStyle _base(Tokens t) => const ButtonStyle(
  minimumSize: WidgetStatePropertyAll(Size(0, Tactil.minimo)),
  padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: Espacio.s4)),
  textStyle: WidgetStatePropertyAll(Tipo.boton),
  shape: WidgetStatePropertyAll(StadiumBorder()),
  elevation: WidgetStatePropertyAll(0),
  visualDensity: VisualDensity.standard,
);

/// La acción principal es verde (ADR-053, A2); el naranja queda para la urgencia de
/// pagar, que la pide `Boton` con su variante. Apagado conserva el contorno: un botón
/// sin forma se lee como texto suelto (hallazgo T3 de la revisión del antes).
ButtonStyle botonRellenoDesde(Tokens t) => _base(t).copyWith(
  backgroundColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.disabled) ? t.surface2 : t.verdeSolido,
  ),
  foregroundColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.disabled) ? t.text3 : t.sobreVerdeSolido,
  ),
  side: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.disabled)
        ? BorderSide(color: t.fieldBorder, width: Borde.fino)
        : BorderSide.none,
  ),
  overlayColor: WidgetStatePropertyAll(
    t.sobreVerdeSolido.withValues(alpha: 0.08),
  ),
);

ButtonStyle botonBordeDesde(Tokens t) => _base(t).copyWith(
  foregroundColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.disabled) ? t.text3 : t.text,
  ),
  side: WidgetStateProperty.resolveWith(
    (e) => BorderSide(
      color: e.contains(WidgetState.disabled) ? t.border : t.fieldBorder,
      width: Borde.fino,
    ),
  ),
  overlayColor: WidgetStatePropertyAll(t.brand.withValues(alpha: 0.06)),
);

ButtonStyle botonTextoDesde(Tokens t) => _base(t).copyWith(
  foregroundColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.disabled) ? t.text3 : t.brandTexto,
  ),
  padding: const WidgetStatePropertyAll(
    EdgeInsets.symmetric(horizontal: Espacio.s2),
  ),
  overlayColor: WidgetStatePropertyAll(t.brand.withValues(alpha: 0.08)),
);

CheckboxThemeData casillaDesde(Tokens t) => CheckboxThemeData(
  fillColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.selected) ? t.brand : Colors.transparent,
  ),
  checkColor: WidgetStatePropertyAll(t.sobreVerdeSolido),
  side: BorderSide(color: t.fieldBorder, width: Borde.desfase),
  shape: RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(Radios.sm / 2),
  ),
);

RadioThemeData radioDesde(Tokens t) => RadioThemeData(
  fillColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.selected) ? t.brand : t.fieldBorder,
  ),
);

SwitchThemeData interruptorDesde(Tokens t) => SwitchThemeData(
  thumbColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.selected) ? t.sobreVerdeSolido : t.surface,
  ),
  trackColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.selected) ? t.brand : t.surface2,
  ),
  trackOutlineColor: WidgetStateProperty.resolveWith(
    (e) => e.contains(WidgetState.selected) ? t.brand : t.fieldBorder,
  ),
);

SliderThemeData deslizadorDesde(Tokens t) => SliderThemeData(
  activeTrackColor: t.brand,
  inactiveTrackColor: t.surface2,
  thumbColor: t.brand,
  overlayColor: t.brand.withValues(alpha: 0.12),
);
