import 'package:aportaya_cliente_publicidad/src/model/conjunto.dart';
import 'package:aportaya_cliente_publicidad/src/model/dinero.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_anunciante.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_anuncio.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_campana.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_conversion.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_entrega.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_liquidacion.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_pieza.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_rechazo.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_revision.dart';
import 'package:aportaya_cliente_publicidad/src/model/entrada_socio.dart';
import 'package:aportaya_cliente_publicidad/src/model/error.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_anunciante.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_anuncio.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_campana.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_clic.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_consumo.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_conversion.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_entrega.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_liquidacion.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_pieza.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_revision.dart';
import 'package:aportaya_cliente_publicidad/src/model/salida_socio.dart';

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
        case 'Conjunto':
          return Conjunto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAnunciante':
          return EntradaAnunciante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAnuncio':
          return EntradaAnuncio.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCampana':
          return EntradaCampana.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaConversion':
          return EntradaConversion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEntrega':
          return EntradaEntrega.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaLiquidacion':
          return EntradaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPieza':
          return EntradaPieza.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRechazo':
          return EntradaRechazo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRevision':
          return EntradaRevision.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaSocio':
          return EntradaSocio.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAnunciante':
          return SalidaAnunciante.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAnuncio':
          return SalidaAnuncio.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCampana':
          return SalidaCampana.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaClic':
          return SalidaClic.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaConsumo':
          return SalidaConsumo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaConversion':
          return SalidaConversion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEntrega':
          return SalidaEntrega.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaLiquidacion':
          return SalidaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPieza':
          return SalidaPieza.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRevision':
          return SalidaRevision.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSocio':
          return SalidaSocio.fromJson(value as Map<String, dynamic>) as ReturnType;
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