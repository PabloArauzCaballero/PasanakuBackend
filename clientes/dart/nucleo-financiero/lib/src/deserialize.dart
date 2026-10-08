import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_bloqueo.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_cierre.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_cierre_retencion.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_recarga.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_retencion.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_retiro.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_reverso.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_transferencia.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_transferencia_destino.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/error.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/saldo_billetera.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_acreditacion.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_bloqueo.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_cierre_billetera.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_cierre_retencion.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_extracto.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_recarga.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_retencion.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_retiro.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_reverso.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/salida_transferencia.dart';

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
        case 'EntradaBloqueo':
          return EntradaBloqueo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCierre':
          return EntradaCierre.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCierreRetencion':
          return EntradaCierreRetencion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRecarga':
          return EntradaRecarga.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRetencion':
          return EntradaRetencion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRetiro':
          return EntradaRetiro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaReverso':
          return EntradaReverso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTransferencia':
          return EntradaTransferencia.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTransferenciaDestino':
          return EntradaTransferenciaDestino.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SaldoBilletera':
          return SaldoBilletera.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAcreditacion':
          return SalidaAcreditacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaBloqueo':
          return SalidaBloqueo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCierreBilletera':
          return SalidaCierreBilletera.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCierreRetencion':
          return SalidaCierreRetencion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaExtracto':
          return SalidaExtracto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRecarga':
          return SalidaRecarga.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRetencion':
          return SalidaRetencion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRetiro':
          return SalidaRetiro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaReverso':
          return SalidaReverso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaTransferencia':
          return SalidaTransferencia.fromJson(value as Map<String, dynamic>) as ReturnType;
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