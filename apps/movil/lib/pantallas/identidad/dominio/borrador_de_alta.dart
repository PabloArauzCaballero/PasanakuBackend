import 'dart:convert';

import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';

import 'estado_alta.dart';

const _claveBorradorAlta = 'alta.borrador.v1';
const _vigenciaBorrador = Duration(hours: 24);

/// Persistencia cifrada del progreso de activación. Nunca guarda contraseñas,
/// códigos OTP, consentimiento contractual ni rutas/fotos del documento.
///
/// Los datos personales que sí se conservan viven solo en Keychain/Keystore y
/// caducan al cabo de 24 horas. Una reanudación desde el paso de verificación
/// vuelve a pedir OTP y fotos: esos pasos no se consideran confirmados offline.
class BorradorDeAlta {
  BorradorDeAlta(this._almacen, {DateTime Function()? ahora})
    : _ahora = ahora ?? DateTime.now;

  final AlmacenSeguro _almacen;
  final DateTime Function() _ahora;

  Future<void> guardar(EstadoAlta estado) async {
    final datos = estado.datos;
    final documento = <String, Object?>{
      'version': 1,
      'guardadoEn': _ahora().toUtc().toIso8601String(),
      'paso': estado.paso.name,
      'datos': {
        'nombres': datos.nombres,
        'apellidos': datos.apellidos,
        'fechaNacimiento': datos.fechaNacimiento?.toIso8601String(),
        'telefono': datos.telefono,
        'tipoDocumento': datos.tipoDocumento,
        'numeroDocumento': datos.numeroDocumento,
        'lugarExpedicion': datos.lugarExpedicion,
        'correo': datos.correo,
        'canalVerificacion': datos.canalVerificacion,
      },
      'perfil': {
        'origenDeFondos': estado.origenDeFondos,
        'detalleDelOrigen': estado.detalleDelOrigen,
        'actividadEconomica': estado.actividadEconomica,
        'montoMensualEstimado': estado.montoMensualEstimado,
      },
    };
    await _almacen.guardar(_claveBorradorAlta, jsonEncode(documento));
  }

  Future<EstadoAlta?> restaurar() async {
    final serializado = await _almacen.leer(_claveBorradorAlta);
    if (serializado == null) return null;

    try {
      final raiz = jsonDecode(serializado);
      if (raiz is! Map<String, dynamic> || raiz['version'] != 1) {
        await borrar();
        return null;
      }

      final guardadoEn = DateTime.tryParse(raiz['guardadoEn'] as String? ?? '');
      final edad = guardadoEn == null
          ? _vigenciaBorrador + const Duration(seconds: 1)
          : _ahora().toUtc().difference(guardadoEn.toUtc());
      if (edad.isNegative || edad > _vigenciaBorrador) {
        await borrar();
        return null;
      }

      final datos = raiz['datos'];
      final perfil = raiz['perfil'];
      if (datos is! Map<String, dynamic> || perfil is! Map<String, dynamic>) {
        await borrar();
        return null;
      }

      final pasoGuardado = PasoAlta.values.firstWhere(
        (paso) => paso.name == raiz['paso'],
        orElse: () => PasoAlta.datos,
      );
      // Desde celular en adelante siempre hay que repetir verificaciones y
      // adjuntar de nuevo las fotos, que por diseño no salen de memoria.
      final pasoSeguro = pasoGuardado.index < PasoAlta.celular.index
          ? pasoGuardado
          : PasoAlta.celular;

      return EstadoAlta(
        paso: pasoSeguro,
        datos: DatosPersonales(
          nombres: _texto(datos, 'nombres'),
          apellidos: _texto(datos, 'apellidos'),
          fechaNacimiento: DateTime.tryParse(
            datos['fechaNacimiento'] as String? ?? '',
          ),
          telefono: _texto(datos, 'telefono'),
          tipoDocumento: _texto(datos, 'tipoDocumento', siVacio: 'CI'),
          numeroDocumento: _texto(datos, 'numeroDocumento'),
          lugarExpedicion: datos['lugarExpedicion'] as String?,
          correo: _texto(datos, 'correo'),
          canalVerificacion: _texto(datos, 'canalVerificacion', siVacio: 'SMS'),
        ),
        origenDeFondos: _texto(perfil, 'origenDeFondos'),
        detalleDelOrigen: _texto(perfil, 'detalleDelOrigen'),
        actividadEconomica: _texto(perfil, 'actividadEconomica'),
        // La versión previa guardaba un JSON number. No convertirlo de nuevo a
        // double: se conserva el resto del borrador y se pide reingresar el monto.
        montoMensualEstimado: _texto(perfil, 'montoMensualEstimado'),
        resumido: true,
      );
    } on Object {
      await borrar();
      return null;
    }
  }

  Future<void> borrar() => _almacen.borrar(_claveBorradorAlta);

  static String _texto(
    Map<String, dynamic> datos,
    String clave, {
    String siVacio = '',
  }) {
    final valor = datos[clave];
    if (valor is! String || valor.isEmpty) return siVacio;
    return valor;
  }
}
