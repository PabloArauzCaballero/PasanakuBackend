import 'package:aportaya_cliente_cumplimiento/src/model/contrato_vigente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/dinero.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_aceptacion.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_diligencia.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_diligencia_documentos_inner.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_incidente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_pep.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_pep_beneficiarios_finales_inner.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_riesgo_operativo.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/error.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_aceptacion.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_alcance.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_alcance_limites_sandbox.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_diligencia.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_diligencia_limites_nuevos_inner.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_incidente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_pep.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_riesgo_operativo.dart';

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
        case 'ContratoVigente':
          return ContratoVigente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAceptacion':
          return EntradaAceptacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDiligencia':
          return EntradaDiligencia.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDiligenciaDocumentosInner':
          return EntradaDiligenciaDocumentosInner.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaIncidente':
          return EntradaIncidente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPep':
          return EntradaPep.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPepBeneficiariosFinalesInner':
          return EntradaPepBeneficiariosFinalesInner.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRiesgoOperativo':
          return EntradaRiesgoOperativo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAceptacion':
          return SalidaAceptacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAlcance':
          return SalidaAlcance.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAlcanceLimitesSandbox':
          return SalidaAlcanceLimitesSandbox.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDiligencia':
          return SalidaDiligencia.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDiligenciaLimitesNuevosInner':
          return SalidaDiligenciaLimitesNuevosInner.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaIncidente':
          return SalidaIncidente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPep':
          return SalidaPep.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRiesgoOperativo':
          return SalidaRiesgoOperativo.fromJson(value as Map<String, dynamic>) as ReturnType;
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