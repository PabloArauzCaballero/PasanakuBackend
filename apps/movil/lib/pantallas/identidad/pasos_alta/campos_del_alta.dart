import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../textos.dart';
import 'canal_de_verificacion.dart';
import 'departamentos.dart';

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
    required this.focoVence,
    required this.errores,
    required this.tocados,
    required this.prefijo,
    required this.fecha,
    required this.lugar,
    required this.vence,
    required this.onCambio,
    required this.onFecha,
    required this.onLugar,
    required this.onVence,
    required this.canal,
    required this.onCanal,
  });

  /// En orden: nombres, apellidos, celular, documento.
  final List<TextEditingController> controladores;
  final List<FocusNode> focos;
  final FocusNode focoFecha;
  final FocusNode focoLugar;
  final FocusNode focoVence;

  /// Por clave: `nombres`, `apellidos`, `telefono`, `documento`, `lugar`, `vence`, `fecha`.
  final Map<String, String?> errores;
  final Set<String> tocados;

  final String prefijo;
  final DateTime? fecha;
  final String? lugar;

  /// El vencimiento del carnet.
  final DateTime? vence;
  final ValueChanged<String> onCambio;
  final ValueChanged<DateTime?> onFecha;
  final ValueChanged<String> onLugar;
  final ValueChanged<DateTime?> onVence;

  /// `SMS` o `CORREO`.
  final String canal;
  final ValueChanged<String> onCanal;

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
        Campo(
          etiqueta: TextosIdentidad.correo,
          controlador: controladores[4],
          foco: focos[4],
          icono: Icons.alternate_email,
          ayuda: TextosIdentidad.correoAyuda,
          error: errores['correo'],
          exito: _listo('correo'),
          tipoDeTeclado: TextInputType.emailAddress,
          onChanged: (_) => onCambio('correo'),
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
                  for (final d in departamentosDelCarnet)
                    (valor: d.sigla, texto: d.nombre),
                ],
                onElegida: onLugar,
              ),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s3),
        // Se escribe leyendo el carnet. El calendario arranca hoy: un documento que
        // vencio ayer no sirve, y el servidor lo vuelve a comprobar con su reloj.
        CampoDeFecha(
          etiqueta: TextosIdentidad.fechaExpiracion,
          valor: vence,
          foco: focoVence,
          icono: Icons.event_available_outlined,
          primera: DateTime(hoy.year, hoy.month, hoy.day),
          ultima: DateTime(hoy.year + 30, hoy.month, hoy.day),
          inicial: DateTime(hoy.year + 1, hoy.month, hoy.day),
          ayuda: TextosIdentidad.fechaExpiracionAyuda,
          error: errores['vence'],
          onElegida: onVence,
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
        const SizedBox(height: Espacio.s4),
        CanalDeVerificacion(valor: canal, onElegido: onCanal),
      ],
    );
  }
}
