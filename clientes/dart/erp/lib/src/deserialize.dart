import 'package:aportaya_cliente_erp/src/model/dinero.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_cierre.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_cobro.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_cuenta_por_cobrar.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_depreciacion.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_ejercicio.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_estado_financiero.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_factura.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_orden.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_pago.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_presupuesto.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_presupuesto_partidas_inner.dart';
import 'package:aportaya_cliente_erp/src/model/entrada_tercero.dart';
import 'package:aportaya_cliente_erp/src/model/error.dart';
import 'package:aportaya_cliente_erp/src/model/salida_cierre.dart';
import 'package:aportaya_cliente_erp/src/model/salida_cobro.dart';
import 'package:aportaya_cliente_erp/src/model/salida_corrida.dart';
import 'package:aportaya_cliente_erp/src/model/salida_cuenta_por_cobrar.dart';
import 'package:aportaya_cliente_erp/src/model/salida_depreciacion.dart';
import 'package:aportaya_cliente_erp/src/model/salida_ejercicio.dart';
import 'package:aportaya_cliente_erp/src/model/salida_estado_financiero.dart';
import 'package:aportaya_cliente_erp/src/model/salida_factura.dart';
import 'package:aportaya_cliente_erp/src/model/salida_orden.dart';
import 'package:aportaya_cliente_erp/src/model/salida_plantilla.dart';
import 'package:aportaya_cliente_erp/src/model/salida_presupuesto.dart';
import 'package:aportaya_cliente_erp/src/model/salida_tercero.dart';

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
        case 'EntradaCierre':
          return EntradaCierre.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCobro':
          return EntradaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCuentaPorCobrar':
          return EntradaCuentaPorCobrar.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaDepreciacion':
          return EntradaDepreciacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEjercicio':
          return EntradaEjercicio.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEstadoFinanciero':
          return EntradaEstadoFinanciero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaFactura':
          return EntradaFactura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaOrden':
          return EntradaOrden.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPago':
          return EntradaPago.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPresupuesto':
          return EntradaPresupuesto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPresupuestoPartidasInner':
          return EntradaPresupuestoPartidasInner.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTercero':
          return EntradaTercero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCierre':
          return SalidaCierre.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCobro':
          return SalidaCobro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCorrida':
          return SalidaCorrida.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCuentaPorCobrar':
          return SalidaCuentaPorCobrar.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaDepreciacion':
          return SalidaDepreciacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEjercicio':
          return SalidaEjercicio.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEstadoFinanciero':
          return SalidaEstadoFinanciero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaFactura':
          return SalidaFactura.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaOrden':
          return SalidaOrden.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPlantilla':
          return SalidaPlantilla.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPresupuesto':
          return SalidaPresupuesto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaTercero':
          return SalidaTercero.fromJson(value as Map<String, dynamic>) as ReturnType;
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