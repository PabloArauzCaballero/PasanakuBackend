import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';
import '../textos_del_alta.dart';

/// Paso 2 de 9 — la contraseña con la que se va a entrar (CU-01).
///
/// **Este paso no existía.** El alta pedía ocho cosas y ninguna era una contraseña, así
/// que la cuenta nacía sin credencial: al terminar, la pantalla de ingreso pedía una
/// clave que nunca se había elegido. Se completaban ocho pasos para quedar afuera.
///
/// Va segundo, apenas después de los datos personales y antes de las fotos: quien
/// abandona el alta a mitad —y mucha gente lo hace en el paso del documento— ya eligió
/// con qué va a entrar cuando vuelva. Al final del formulario nadie lee una regla de
/// contraseña; acá todavía sí.
///
/// Las reglas que se muestran son las mismas que aplica el servidor (`PoliticaDeClave`):
/// ocho caracteres y que no contenga el celular ni el documento. No se inventan acá —si
/// el servidor rechaza, es porque esta pantalla dejó pasar algo.
class PasoContrasena extends ConsumerStatefulWidget {
  const PasoContrasena({super.key});

  @override
  ConsumerState<PasoContrasena> createState() => _PasoContrasenaState();
}

class _PasoContrasenaState extends ConsumerState<PasoContrasena> {
  late final TextEditingController _clave;
  late final TextEditingController _repetida;
  var _intentado = false;

  /// El mínimo del servidor. Vive acá como constante y no como número suelto en el
  /// texto: el mensaje de ayuda lo lee de este valor, así que no pueden divergir.
  static const largoMinimo = 8;

  @override
  void initState() {
    super.initState();
    final estado = ref.read(altaProvider);
    _clave = TextEditingController(text: estado.contrasena);
    _repetida = TextEditingController(text: estado.contrasena);
  }

  @override
  void dispose() {
    _clave.dispose();
    _repetida.dispose();
    super.dispose();
  }

  String? get _errorClave {
    final clave = _clave.text;
    if (clave.length < largoMinimo) return TextosDelAlta.claveCorta;
    final datos = ref.read(altaProvider).datos;
    // Las mismas dos comprobaciones que hace el servidor, para no gastar un viaje ni
    // dejar a alguien mirando un error genérico al final del alta.
    final celular = datos.telefono.replaceAll(RegExp(r'\D'), '');
    final documento = datos.numeroDocumento.replaceAll(RegExp(r'\D'), '');
    final sinEspacios = clave.replaceAll(RegExp(r'\s'), '');
    if (celular.length >= 4 && sinEspacios.contains(celular.substring(3))) {
      return TextosDelAlta.claveConTelefono;
    }
    if (documento.length >= 4 && sinEspacios.contains(documento)) {
      return TextosDelAlta.claveConDocumento;
    }
    return null;
  }

  String? get _errorRepetida =>
      _repetida.text == _clave.text ? null : TextosDelAlta.clavesNoCoinciden;

  String? _visible(String? error) => _intentado ? error : null;

  void _continuar() {
    setState(() => _intentado = true);
    if (_errorClave != null || _errorRepetida != null) return;
    ref.read(altaProvider.notifier).elegirContrasena(_clave.text);
    ref.read(altaProvider.notifier).siguiente();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosDelAlta.claveIntro,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s4),
          CampoContrasena(
            etiqueta: TextosDelAlta.claveNueva,
            controlador: _clave,
            ayuda: TextosDelAlta.claveAyuda,
            error: _visible(_errorClave),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: Espacio.s4),
          CampoContrasena(
            etiqueta: TextosDelAlta.claveRepetir,
            controlador: _repetida,
            error: _visible(_errorRepetida),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: Espacio.s4),
          // No es decoración: es la única advertencia del alta sobre algo que AportaYa
          // nunca va a pedir, y el momento en que se lee es este.
          const Alerta(
            tono: Tono.info,
            titulo: TextosDelAlta.claveAvisoTitulo,
            detalle: TextosDelAlta.claveAvisoDetalle,
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: _continuar,
          ),
        ],
      ),
    );
  }
}
