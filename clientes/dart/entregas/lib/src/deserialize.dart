import 'package:aportaya_cliente_entregas/src/model/deduccion_entrega.dart';
import 'package:aportaya_cliente_entregas/src/model/designar_cuenta_principal200_response.dart';
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
import 'package:aportaya_cliente_entregas/src/model/disponibilidad.dart';
import 'package:aportaya_cliente_entregas/src/model/ejecutar_entrega_request.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_liquidacion.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_orden_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_registro_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_respuesta_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/error.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_autorizacion.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_ejecucion_entrega.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_intento_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_liquidacion.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_orden_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_registro_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_verificacion_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/verificar_cuenta_destino_request.dart';

final _regList = RegExp(r'^List<(.*)>$');
final _regSet = RegExp(r'^Set<(.*)>$');
final _regMap = RegExp(r'^Map<String,(.*)>$');

  ReturnType deserialize<ReturnType, BaseType>(dynamic value, String targetType, {bool growable= true}) {
      switch (targetType) {
        case 'String':
          return '$value' as ReturnType;
        case 'int':
          return (value is int ? value : int.parse('$value')) as ReturnType;
        case 'bool':
          if (value is bool) {
            return value as ReturnType;
          }
          final valueString = '$value'.toLowerCase();
          return (valueString == 'true' || valueString == '1') as ReturnType;
        case 'double':
          return (value is double ? value : double.parse('$value')) as ReturnType;
        case 'DeduccionEntrega':
          return DeduccionEntrega.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DesignarCuentaPrincipal200Response':
          return DesignarCuentaPrincipal200Response.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Disponibilidad':
          return Disponibilidad.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EjecutarEntregaRequest':
          return EjecutarEntregaRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaLiquidacion':
          return EntradaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaOrdenDesembolso':
          return EntradaOrdenDesembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRegistroCuenta':
          return EntradaRegistroCuenta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRespuestaDesembolso':
          return EntradaRespuestaDesembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAutorizacion':
          return SalidaAutorizacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEjecucionEntrega':
          return SalidaEjecucionEntrega.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaIntentoDesembolso':
          return SalidaIntentoDesembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaLiquidacion':
          return SalidaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaOrdenDesembolso':
          return SalidaOrdenDesembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRegistroCuenta':
          return SalidaRegistroCuenta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaVerificacionCuenta':
          return SalidaVerificacionCuenta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'VerificarCuentaDestinoRequest':
          return VerificarCuentaDestinoRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        default:
          RegExpMatch? match;

          if (value is List && (match = _regList.firstMatch(targetType)) != null) {
            targetType = match![1]!; // ignore: parameter_assignments
            return value
              .map<BaseType>((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable))
              .toList(growable: growable) as ReturnType;
          }
          if (value is Set && (match = _regSet.firstMatch(targetType)) != null) {
            targetType = match![1]!; // ignore: parameter_assignments
            return value
              .map<BaseType>((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable))
              .toSet() as ReturnType;
          }
          if (value is Map && (match = _regMap.firstMatch(targetType)) != null) {
            targetType = match![1]!.trim(); // ignore: parameter_assignments
            return Map<String, BaseType>.fromIterables(
              value.keys as Iterable<String>,
              value.values.map((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable)),
            ) as ReturnType;
          }
          break;
    }
    throw Exception('Cannot deserialize');
  }