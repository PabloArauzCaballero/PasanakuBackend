import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Los tonos semánticos, separados del acento: un estado nunca se parece a un botón.
enum Tono { ok, aviso, error, info, neutro, marca }

extension TonoColores on Tono {
  (Color fondo, Color texto) coloresDe(Tokens t) => switch (this) {
    Tono.ok => (t.okBg, t.okTexto),
    Tono.aviso => (t.warnBg, t.avisoTexto),
    Tono.error => (t.errBg, t.errTexto),
    Tono.info => (t.infoBg, t.infoTexto),
    Tono.neutro => (t.surface2, t.text2),
    Tono.marca => (t.verdeSolido, t.sobreVerdeSolido),
  };
}
