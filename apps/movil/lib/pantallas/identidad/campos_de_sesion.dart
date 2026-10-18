import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/validaciones.dart';
import 'textos.dart';
import 'textos_del_alta.dart';

/// Con qué dice la persona quién es al entrar: su celular —por omisión, con el teclado
/// numérico y el `+591` ya puesto— o su correo, si toca «Entrar con mi correo».
/// El contrato acepta uno de los dos (`telefonoE164` o `correo`).
class IdentificadorDeIngreso {
  final telefono = TextEditingController();
  final correo = TextEditingController();
  bool porCorreo = false;

  /// Lo que va al servidor: `+591` y los ocho dígitos, o el correo sin espacios.
  String get valor {
    if (porCorreo) return correo.text.trim();
    final digitos = telefono.text.trim();
    return digitos.isEmpty ? '' : '+591$digitos';
  }

  bool get valido =>
      porCorreo ? errorCorreo(valor) == null : errorTelefono(valor) == null;

  void dispose() {
    telefono.dispose();
    correo.dispose();
  }
}

/// Campos del paso de credenciales del ingreso.
class CamposDeSesion extends StatelessWidget {
  const CamposDeSesion({
    super.key,
    required this.identificador,
    required this.contrasena,
    required this.onChanged,
  });

  final IdentificadorDeIngreso identificador;
  final TextEditingController contrasena;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      if (identificador.porCorreo)
        Campo(
          key: const Key('campo-correo-de-ingreso'),
          etiqueta: TextosIdentidad.correo,
          icono: Icons.mail_outline,
          ayuda: TextosIdentidad.sesionCorreoAyuda,
          ejemplo: TextosDelAlta.ejemploCorreo,
          controlador: identificador.correo,
          tipoDeTeclado: TextInputType.emailAddress,
          onChanged: (_) => onChanged(),
        )
      else
        Campo(
          etiqueta: TextosIdentidad.telefono,
          icono: Icons.phone_iphone,
          prefijo: '+591',
          ayuda: TextosIdentidad.sesionCelularAyuda,
          ejemplo: TextosDelAlta.ejemploTelefono,
          controlador: identificador.telefono,
          tipoDeTeclado: TextInputType.phone,
          onChanged: (_) => onChanged(),
        ),
      Align(
        alignment: Alignment.centerLeft,
        child: TextButton(
          onPressed: () {
            identificador.porCorreo = !identificador.porCorreo;
            onChanged();
          },
          child: Text(
            identificador.porCorreo
                ? TextosIdentidad.sesionUsarCelular
                : TextosIdentidad.sesionUsarCorreo,
          ),
        ),
      ),
      const SizedBox(height: Espacio.s2),
      CampoContrasena(
        etiqueta: TextosIdentidad.contrasena,
        ejemplo: TextosDelAlta.ejemploContrasena,
        controlador: contrasena,
        onChanged: (_) => onChanged(),
      ),
    ],
  );
}
