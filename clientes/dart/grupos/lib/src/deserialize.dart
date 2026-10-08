import 'package:aportaya_cliente_grupos/src/model/actividad_en_grupos.dart';
import 'package:aportaya_cliente_grupos/src/model/alias_resuelto.dart';
import 'package:aportaya_cliente_grupos/src/model/compromiso_de_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/dia_salteado.dart';
import 'package:aportaya_cliente_grupos/src/model/dinero.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_acuerdo.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_compromiso.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_grupo.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_invitacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_permuta.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_postulacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_retiro.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_revelacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_traspaso.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_voto.dart';
import 'package:aportaya_cliente_grupos/src/model/error.dart';
import 'package:aportaya_cliente_grupos/src/model/paquete_del_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/revelacion_de_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_acuerdo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_grupo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_invitacion.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_permuta.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_plazo_habil.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_postulacion.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_retiro.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_traspaso.dart';

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
        case 'ActividadEnGrupos':
          return ActividadEnGrupos.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AliasResuelto':
          return AliasResuelto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'CompromisoDeSorteo':
          return CompromisoDeSorteo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DiaSalteado':
          return DiaSalteado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAcuerdo':
          return EntradaAcuerdo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCompromiso':
          return EntradaCompromiso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaGrupo':
          return EntradaGrupo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaInvitacion':
          return EntradaInvitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPermuta':
          return EntradaPermuta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaPostulacion':
          return EntradaPostulacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRetiro':
          return EntradaRetiro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRevelacion':
          return EntradaRevelacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTraspaso':
          return EntradaTraspaso.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaVoto':
          return EntradaVoto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PaqueteDelSorteo':
          return PaqueteDelSorteo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RevelacionDeSorteo':
          return RevelacionDeSorteo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAcuerdo':
          return SalidaAcuerdo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaGrupo':
          return SalidaGrupo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaInvitacion':
          return SalidaInvitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPermuta':
          return SalidaPermuta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPlazoHabil':
          return SalidaPlazoHabil.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPostulacion':
          return SalidaPostulacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRetiro':
          return SalidaRetiro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaTraspaso':
          return SalidaTraspaso.fromJson(value as Map<String, dynamic>) as ReturnType;
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