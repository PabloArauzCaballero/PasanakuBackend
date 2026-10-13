import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../dominio/canales_de_verificacion.dart';
import '../textos.dart';
import '../textos_del_alta.dart';
import 'bloque_de_formulario.dart';
import 'canal_de_verificacion.dart';

/// Contacto del alta y canal de confirmación del correo.
class BloqueContactoDeCuenta extends StatelessWidget {
  const BloqueContactoDeCuenta({
    super.key,
    required this.telefono,
    required this.correo,
    required this.focoTelefono,
    required this.focoCorreo,
    required this.errorTelefono,
    required this.errorCorreo,
    required this.onCambioTelefono,
    required this.onCambioCorreo,
  });

  static const prefijoBolivia = '+591';

  final TextEditingController telefono;
  final TextEditingController correo;
  final FocusNode focoTelefono;
  final FocusNode focoCorreo;
  final String? errorTelefono;
  final String? errorCorreo;
  final ValueChanged<String> onCambioTelefono;
  final ValueChanged<String> onCambioCorreo;

  @override
  Widget build(BuildContext context) => BloqueDeFormulario(
    icono: Icons.contact_mail_outlined,
    titulo: 'Cómo te contactamos',
    hijos: [
      Campo(
        etiqueta: TextosIdentidad.telefono,
        controlador: telefono,
        foco: focoTelefono,
        icono: Icons.smartphone_outlined,
        prefijo: prefijoBolivia,
        ejemplo: TextosDelAlta.ejemploTelefono,
        ayuda: TextosIdentidad.telefonoAyuda,
        error: errorTelefono,
        tipoDeTeclado: TextInputType.phone,
        formateadores: [
          FilteringTextInputFormatter.digitsOnly,
          LengthLimitingTextInputFormatter(8),
        ],
        onChanged: onCambioTelefono,
      ),
      Campo(
        etiqueta: TextosIdentidad.correo,
        controlador: correo,
        foco: focoCorreo,
        icono: Icons.mail_outline_rounded,
        ejemplo: TextosDelAlta.ejemploCorreo,
        ayuda: TextosIdentidad.correoAyuda,
        error: errorCorreo,
        tipoDeTeclado: TextInputType.emailAddress,
        onChanged: onCambioCorreo,
      ),
      CanalDeVerificacion(
        canales: canalesDeVerificacionDelAlta,
        valor: canalesDeVerificacionDelAlta.single,
        onElegido: (_) {},
      ),
    ],
  );
}
