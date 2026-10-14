import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

/// El idioma de la app: español de Bolivia, y español a secas como respaldo.
///
/// Sin esto, con el teléfono en inglés el selector de fecha del alta mostraba «Thu, Oct 11» y
/// «October 1990» (carril H2, D3): la app declaraba textos en español pero ningún idioma. Con una
/// sola lista soportada, el teléfono en inglés (o en cualquier otro idioma) cae en la primera:
/// `es_BO`.
const idiomasDeLaApp = <Locale>[Locale('es', 'BO'), Locale('es')];

/// Los textos propios de Material (selector de fecha, «Aceptar», «Cancelar», menús del campo de
/// texto) en esos idiomas.
const List<LocalizationsDelegate<dynamic>> delegadosDeIdioma =
    GlobalMaterialLocalizations.delegates;
