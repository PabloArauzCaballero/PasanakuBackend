import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
import 'package:aportaya_cliente_aportes/src/model/entrada_cobro.dart';
import 'package:aportaya_cliente_aportes/src/model/entrada_disputa.dart';
import 'package:aportaya_cliente_aportes/src/model/entrada_enrutamiento.dart';
import 'package:aportaya_cliente_aportes/src/model/entrada_proveedor.dart';
import 'package:aportaya_cliente_aportes/src/model/entrada_reembolso.dart';
import 'package:aportaya_cliente_aportes/src/model/error.dart';
import 'package:aportaya_cliente_aportes/src/model/estado_del_participante.dart';
import 'package:aportaya_cliente_aportes/src/model/morosos.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_cobro.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_disputa.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_ejecucion_reembolso.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_enrutamiento.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_proveedor.dart';
import 'package:aportaya_cliente_aportes/src/model/salida_reembolso.dart';

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
        case 'EntradaCobro':
          return EntradaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDisputa':
          return EntradaDisputa.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEnrutamiento':
          return EntradaEnrutamiento.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaProveedor':
          return EntradaProveedor.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaReembolso':
          return EntradaReembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EstadoDelParticipante':
          return EstadoDelParticipante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Morosos':
          return Morosos.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCobro':
          return SalidaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDisputa':
          return SalidaDisputa.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEjecucionReembolso':
          return SalidaEjecucionReembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEnrutamiento':
          return SalidaEnrutamiento.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaProveedor':
          return SalidaProveedor.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaReembolso':
          return SalidaReembolso.fromJson(value as Map<String, dynamic>) as ReturnType;
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