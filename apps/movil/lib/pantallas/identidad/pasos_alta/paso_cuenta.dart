import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/errores.dart';
import '../dominio/canales_de_verificacion.dart';
import '../dominio/estado_alta.dart';
import '../dominio/validaciones.dart';
import '../textos_del_alta.dart';
import 'bloque_clave_de_cuenta.dart';
import 'bloque_contacto_de_cuenta.dart';
import '../continuar_alta.dart';
import 'tarjeta_de_confianza.dart';

/// Fase 1 — contacto y contraseña en el paso «cuenta» (CU-01).
///
/// La contraseña se elige antes de confirmar el correo y del documento. Esto facilita
/// retomar el alta si se abandona durante identidad; el servidor sigue siendo quien
/// valida el conjunto completo cuando se envía el contrato.
///
/// Las reglas visibles reflejan las que aplica el servidor (`PoliticaDeClave`). En el
/// primer paso ya se puede contrastar la clave con el teléfono; si el CI aún no se
/// ingresó, su relación con la clave se valida al final en el servidor.
class PasoCuenta extends ConsumerStatefulWidget {
  const PasoCuenta({super.key, this.continuar = false});
  final bool continuar;

  @override
  ConsumerState<PasoCuenta> createState() => _PasoCuentaState();
}

class _PasoCuentaState extends ConsumerState<PasoCuenta> {
  late final _telefono = TextEditingController(
    text: ref
        .read(altaProvider)
        .datos
        .telefono
        .replaceFirst(BloqueContactoDeCuenta.prefijoBolivia, ''),
  );
  late final _correo = TextEditingController(
    text: ref.read(altaProvider).datos.correo,
  );
  final _focoTelefono = FocusNode();
  final _focoCorreo = FocusNode();
  final _contactoTocado = <String>{};
  var _intentoContacto = false;
  late final TextEditingController _clave;
  final _focoClave = FocusNode();
  var _intentado = false;

  /// El mínimo del servidor. Vive acá como constante y no como número suelto en el
  /// texto: el mensaje de ayuda lo lee de este valor, así que no pueden divergir.
  static const largoMinimo = 8;
  final _sugerencia =
      sugerirContrasena(); // fija mientras la pantalla está abierta

  @override
  void initState() {
    super.initState();
    final estado = ref.read(altaProvider);
    _clave = TextEditingController(text: estado.contrasena);
    if (estado.campoError == CampoDeErrorApi.contrasena) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _focoClave.requestFocus();
      });
    }
  }

  @override
  void dispose() {
    _telefono.dispose();
    _correo.dispose();
    _focoTelefono.dispose();
    _focoCorreo.dispose();
    _clave.dispose();
    _focoClave.dispose();
    super.dispose();
  }

  String? get _errorClave {
    final clave = _clave.text;
    final espacios = errorEspaciosEnClave(clave, sugerencia: _sugerencia);
    if (espacios != null) return espacios;
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

  String? _visible(String? error) => _intentado ? error : null;

  String get _telefonoCompleto => _telefono.text.isEmpty
      ? ''
      : '${BloqueContactoDeCuenta.prefijoBolivia}${_telefono.text}';

  void _sincronizarContacto(String campo) {
    _contactoTocado.add(campo);
    final estado = ref.read(altaProvider);
    ref
        .read(altaProvider.notifier)
        .actualizarDatos(
          estado.datos.copiarCon(
            telefono: _telefonoCompleto,
            correo: _correo.text,
            canalVerificacion: canalesDeVerificacionDelAlta.single,
          ),
        );
    setState(() {});
  }

  String? _errorContacto(String campo) {
    if (!_intentoContacto && !_contactoTocado.contains(campo)) return null;
    return campo == 'telefono'
        ? errorTelefono(_telefonoCompleto)
        : errorCorreo(_correo.text);
  }

  String? _errorRemoto(EstadoAlta alta) =>
      alta.campoError == CampoDeErrorApi.contrasena ? alta.error : null;

  Future<void> _continuar() async {
    setState(() => _intentoContacto = true);
    _sincronizarContacto('telefono');
    if (errorTelefono(_telefonoCompleto) != null ||
        errorCorreo(_correo.text) != null) {
      if (errorTelefono(_telefonoCompleto) != null) {
        _focoTelefono.requestFocus();
      } else {
        _focoCorreo.requestFocus();
      }
      return;
    }
    setState(() => _intentado = true);
    if (_errorClave != null) return;
    final notifier = ref.read(altaProvider.notifier);
    notifier.elegirContrasena(_clave.text);
    // Continuar siempre pide un código: confirmarlo es lo que trae el borrador.
    final enviado = await notifier.solicitarCodigoCorreo(
      nuevo: widget.continuar,
    );
    if (enviado) notifier.siguiente();
  }

  @override
  Widget build(BuildContext context) {
    final alta = ref.watch(altaProvider);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (widget.continuar) const AvisoDeContinuarAlta(),
          BloqueContactoDeCuenta(
            telefono: _telefono,
            correo: _correo,
            focoTelefono: _focoTelefono,
            focoCorreo: _focoCorreo,
            errorTelefono: _errorContacto('telefono'),
            errorCorreo: _errorContacto('correo'),
            onCambioTelefono: (_) => _sincronizarContacto('telefono'),
            onCambioCorreo: (_) => _sincronizarContacto('correo'),
          ),
          const SizedBox(height: Espacio.s3),
          BloqueClaveDeCuenta(
            clave: _clave,
            focoClave: _focoClave,
            // Los espacios se avisan al instante, sin esperar a «Continuar».
            errorClave: tieneEspacios(_clave.text)
                ? _errorClave
                : _visible(_errorClave),
            errorRemoto: _errorRemoto(alta),
            trazaId: alta.trazaId,
            errorCodigo: alta.errorCodigo,
            cargando: alta.enviandoCodigo,
            onCambioClave: (valor) {
              ref.read(altaProvider.notifier).elegirContrasena(valor);
              setState(() {});
            },
            onContinuar: _continuar,
          ),
          const SizedBox(height: Espacio.s4),
          const TarjetaDeConfianza(),
        ],
      ),
    );
  }
}
