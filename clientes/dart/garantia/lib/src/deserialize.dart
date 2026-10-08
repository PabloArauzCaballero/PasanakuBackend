import 'package:aportaya_cliente_garantia/src/model/aprobar_reemplazo200_response.dart';
import 'package:aportaya_cliente_garantia/src/model/devolucion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_cobertura.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_declaracion.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_descargo.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_devolucion_fondo.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_disolucion.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_reemplazo.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_resolucion_descargo.dart';
import 'package:aportaya_cliente_garantia/src/model/entrada_restriccion.dart';
import 'package:aportaya_cliente_garantia/src/model/error.dart';
import 'package:aportaya_cliente_garantia/src/model/levantar_restriccion200_response.dart';
import 'package:aportaya_cliente_garantia/src/model/levantar_restriccion_request.dart';
import 'package:aportaya_cliente_garantia/src/model/liquidacion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/posicion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/restriccion_vigente.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_cierre_disolucion.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_cobertura.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_declaracion.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_descargo.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_devolucion_fondo.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_disolucion.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_ejecucion_aval.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_reemplazo.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_resolucion_descargo.dart';
import 'package:aportaya_cliente_garantia/src/model/salida_restriccion.dart';

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
        case 'AprobarReemplazo200Response':
          return AprobarReemplazo200Response.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DevolucionDeParticipante':
          return DevolucionDeParticipante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCobertura':
          return EntradaCobertura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDeclaracion':
          return EntradaDeclaracion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDescargo':
          return EntradaDescargo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDevolucionFondo':
          return EntradaDevolucionFondo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDisolucion':
          return EntradaDisolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaReemplazo':
          return EntradaReemplazo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaResolucionDescargo':
          return EntradaResolucionDescargo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRestriccion':
          return EntradaRestriccion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LevantarRestriccion200Response':
          return LevantarRestriccion200Response.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LevantarRestriccionRequest':
          return LevantarRestriccionRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LiquidacionDeParticipante':
          return LiquidacionDeParticipante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PosicionDeParticipante':
          return PosicionDeParticipante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RestriccionVigente':
          return RestriccionVigente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCierreDisolucion':
          return SalidaCierreDisolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCobertura':
          return SalidaCobertura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDeclaracion':
          return SalidaDeclaracion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDescargo':
          return SalidaDescargo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDevolucionFondo':
          return SalidaDevolucionFondo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDisolucion':
          return SalidaDisolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEjecucionAval':
          return SalidaEjecucionAval.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaReemplazo':
          return SalidaReemplazo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaResolucionDescargo':
          return SalidaResolucionDescargo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRestriccion':
          return SalidaRestriccion.fromJson(value as Map<String, dynamic>) as ReturnType;
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