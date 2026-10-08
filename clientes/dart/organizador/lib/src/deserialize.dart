import 'package:aportaya_cliente_organizador/src/model/dinero.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_apelacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_aprobacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_contrato.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_ejecucion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_evaluacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_postulacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_programacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_regla.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_resolucion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_sancion.dart';
import 'package:aportaya_cliente_organizador/src/model/error.dart';
import 'package:aportaya_cliente_organizador/src/model/faltante.dart';
import 'package:aportaya_cliente_organizador/src/model/firmar_contrato_request.dart';
import 'package:aportaya_cliente_organizador/src/model/habilitacion.dart';
import 'package:aportaya_cliente_organizador/src/model/metrica.dart';
import 'package:aportaya_cliente_organizador/src/model/rescindir_contrato_request.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_apelacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_contrato.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_ejecucion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_evaluacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_firma.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_habilitacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_postulacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_programacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_regla.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_rescision.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_resolucion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_sancion.dart';

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
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaApelacion':
          return EntradaApelacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAprobacion':
          return EntradaAprobacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaContrato':
          return EntradaContrato.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEjecucion':
          return EntradaEjecucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEvaluacion':
          return EntradaEvaluacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPostulacion':
          return EntradaPostulacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaProgramacion':
          return EntradaProgramacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRegla':
          return EntradaRegla.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaResolucion':
          return EntradaResolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaSancion':
          return EntradaSancion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Faltante':
          return Faltante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'FirmarContratoRequest':
          return FirmarContratoRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Habilitacion':
          return Habilitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Metrica':
          return Metrica.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RescindirContratoRequest':
          return RescindirContratoRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaApelacion':
          return SalidaApelacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaContrato':
          return SalidaContrato.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEjecucion':
          return SalidaEjecucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEvaluacion':
          return SalidaEvaluacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaFirma':
          return SalidaFirma.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaHabilitacion':
          return SalidaHabilitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPostulacion':
          return SalidaPostulacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaProgramacion':
          return SalidaProgramacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRegla':
          return SalidaRegla.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRescision':
          return SalidaRescision.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaResolucion':
          return SalidaResolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSancion':
          return SalidaSancion.fromJson(value as Map<String, dynamic>) as ReturnType;
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