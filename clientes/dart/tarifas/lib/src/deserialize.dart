import 'package:aportaya_cliente_tarifas/src/model/aceptar_cotizacion200_response.dart';
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_cobro.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_cotizacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_devengo.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_devolucion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_factura.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_liquidacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_publicacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_segmento.dart';
import 'package:aportaya_cliente_tarifas/src/model/error.dart';
import 'package:aportaya_cliente_tarifas/src/model/linea_desglose.dart';
import 'package:aportaya_cliente_tarifas/src/model/poner_tarifario_vigente200_response.dart';
import 'package:aportaya_cliente_tarifas/src/model/resolver_precio_request.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_cobro.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_cotizacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_devengo.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_devolucion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_factura.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_liquidacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_publicacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_segmento.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_segmento_aplicado.dart';
import 'package:aportaya_cliente_tarifas/src/model/tarifario_vigente.dart';

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
        case 'AceptarCotizacion200Response':
          return AceptarCotizacion200Response.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCobro':
          return EntradaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCotizacion':
          return EntradaCotizacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDevengo':
          return EntradaDevengo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDevolucion':
          return EntradaDevolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaFactura':
          return EntradaFactura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaLiquidacion':
          return EntradaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPublicacion':
          return EntradaPublicacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaSegmento':
          return EntradaSegmento.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LineaDesglose':
          return LineaDesglose.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PonerTarifarioVigente200Response':
          return PonerTarifarioVigente200Response.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResolverPrecioRequest':
          return ResolverPrecioRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCobro':
          return SalidaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCotizacion':
          return SalidaCotizacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDevengo':
          return SalidaDevengo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDevolucion':
          return SalidaDevolucion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaFactura':
          return SalidaFactura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaLiquidacion':
          return SalidaLiquidacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPublicacion':
          return SalidaPublicacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSegmento':
          return SalidaSegmento.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSegmentoAplicado':
          return SalidaSegmentoAplicado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'TarifarioVigente':
          return TarifarioVigente.fromJson(value as Map<String, dynamic>) as ReturnType;
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