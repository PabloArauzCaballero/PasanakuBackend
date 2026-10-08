import 'package:aportaya_cliente_notificaciones/src/model/dinero.dart';
import 'package:aportaya_cliente_notificaciones/src/model/entrada_notificacion.dart';
import 'package:aportaya_cliente_notificaciones/src/model/entrada_respuesta.dart';
import 'package:aportaya_cliente_notificaciones/src/model/error.dart';
import 'package:aportaya_cliente_notificaciones/src/model/salida_notificacion.dart';
import 'package:aportaya_cliente_notificaciones/src/model/salida_respuesta.dart';
import 'package:aportaya_cliente_notificaciones/src/model/supresion.dart';

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
        case 'EntradaNotificacion':
          return EntradaNotificacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRespuesta':
          return EntradaRespuesta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaNotificacion':
          return SalidaNotificacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRespuesta':
          return SalidaRespuesta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Supresion':
          return Supresion.fromJson(value as Map<String, dynamic>) as ReturnType;
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