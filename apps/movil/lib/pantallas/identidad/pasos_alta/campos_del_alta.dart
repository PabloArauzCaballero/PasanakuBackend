import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../textos.dart';

/// Los cinco campos del primer paso del alta, con su ícono y su ayuda.
///
/// Van juntos y aparte del paso porque son la **forma** del formulario: qué se
/// pregunta, con qué ícono y con qué ayuda. Cuándo mostrar un error y qué pasa al
/// tocar «Continuar» es otra cosa, y vive en `PasoDatos`.
class CamposDelAlta extends StatelessWidget {
  const CamposDelAlta({
    super.key,
    required this.controladores,
    required this.focos,
    required this.focoFecha,
    required this.focoLugar,
    required this.errores,
    required this.tocados,
    required this.prefijo,
    required this.fecha,
    required this.lugar,
    required this.onCambio,
    required this.onFecha,
    required this.onLugar,
  });

  /// En orden: nombres, apellidos, celular, documento.
  final List<TextEditingController> controladores;
  final List<FocusNode> focos;
  final FocusNode focoFecha;
  final FocusNode focoLugar;

  /// Por clave: `nombres`, `apellidos`, `telefono`, `documento`, `fecha`.
  final Map<String, String?> errores;
  final Set<String> tocados;

  final String prefijo;
  final DateTime? fecha;
  final String? lugar;
  final ValueChanged<String> onCambio;
  final ValueChanged<DateTime> onFecha;
  final ValueChanged<String> onLugar;

  /// Los nueve departamentos, con la sigla que lleva el carnet.
  static const departamentos = <({String sigla, String nombre})>[
    (sigla: 'LP', nombre: 'La Paz'),
    (sigla: 'SC', nombre: 'Santa Cruz'),
    (sigla: 'CB', nombre: 'Cochabamba'),
    (sigla: 'OR', nombre: 'Oruro'),
    (sigla: 'PT', nombre: 'Potosí'),
    (sigla: 'CH', nombre: 'Chuquisaca'),
    (sigla: 'TJ', nombre: 'Tarija'),
    (sigla: 'BE', nombre: 'Beni'),
    (sigla: 'PD', nombre: 'Pando'),
  ];

  /// El tilde verde solo después de tocar el campo: un formulario recién abierto no
  /// tiene nada que festejar.
  bool _listo(String cual) => tocados.contains(cual) && errores[cual] == null;

  @override
  Widget build(BuildContext context) {
    final hoy = DateTime.now();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Campo(
          etiqueta: TextosIdentidad.nombres,
          controlador: controladores[0],
          foco: focos[0],
          icono: Icons.badge_outlined,
          error: errores['nombres'],
          exito: _listo('nombres'),
          tipoDeTeclado: TextInputType.name,
          onChanged: (_) => onCambio('nombres'),
        ),
        const SizedBox(height: Espacio.s3),
        Campo(
          etiqueta: TextosIdentidad.apellidos,
          controlador: controladores[1],
          foco: focos[1],
          icono: Icons.badge_outlined,
          error: errores['apellidos'],
          exito: _listo('apellidos'),
          tipoDeTeclado: TextInputType.name,
          onChanged: (_) => onCambio('apellidos'),
        ),
        const SizedBox(height: Espacio.s3),
        Campo(
          etiqueta: TextosIdentidad.telefono,
          controlador: controladores[2],
          foco: focos[2],
          icono: Icons.smartphone_outlined,
          // El prefijo del país va puesto: escribirlo a mano era la causa más común
          // de un formulario que no dejaba seguir.
          prefijo: prefijo,
          ayuda: TextosIdentidad.telefonoAyuda,
          error: errores['telefono'],
          exito: _listo('telefono'),
          tipoDeTeclado: TextInputType.phone,
          formateadores: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(8),
          ],
          onChanged: (_) => onCambio('telefono'),
        ),
        const SizedBox(height: Espacio.s3),
        // El número y su extensión van en **un renglón**, como están en el carnet:
        // `5551234 LP`. Separarlos sugiere que son dos datos sueltos, y no lo son —
        // el número solo no identifica a nadie.
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: Campo(
                etiqueta: TextosIdentidad.numeroDocumento,
                controlador: controladores[3],
                foco: focos[3],
                icono: Icons.credit_card_outlined,
                ayuda: TextosIdentidad.documentoAyuda,
                error: errores['documento'],
                exito: _listo('documento'),
                onChanged: (_) => onCambio('documento'),
              ),
            ),
            const SizedBox(width: Espacio.s3),
            Expanded(
              flex: 2,
              child: CampoDeSeleccion<String>(
                etiqueta: TextosIdentidad.lugarExpedicion,
                valor: lugar,
                textoVacio: TextosIdentidad.lugarExpedicionVacio,
                ayuda: TextosIdentidad.lugarExpedicionAyuda,
                error: errores['lugar'],
                exito: _listo('lugar'),
                foco: focoLugar,
                opciones: [
                  for (final d in departamentos)
                    (valor: d.sigla, texto: d.nombre),
                ],
                onElegida: onLugar,
              ),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s3),
        CampoDeFecha(
          etiqueta: TextosIdentidad.fechaNacimiento,
          valor: fecha,
          foco: focoFecha,
          ultima: DateTime(hoy.year - 18, hoy.month, hoy.day),
          inicial: DateTime(hoy.year - 25, hoy.month, hoy.day),
          ayuda: TextosIdentidad.fechaNacimientoAyuda,
          error: errores['fecha'],
          onElegida: onFecha,
        ),
      ],
    );
  }
}
