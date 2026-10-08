import 'package:aportaya_cliente_transparencia/src/model/alerta_de_riesgo.dart';
import 'package:aportaya_cliente_transparencia/src/model/bloque_publicado.dart';
import 'package:aportaya_cliente_transparencia/src/model/componente.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_certificado.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_cierre_alerta.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_compensacion.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_evaluacion_insignias.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_evaluacion_riesgo.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_evento_reputacion.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_moderacion.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_recalculo.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_resena.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_revocacion_certificado.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_revocacion_insignia.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_sellado.dart';
import 'package:aportaya_cliente_transparencia/src/model/entrada_snapshot.dart';
import 'package:aportaya_cliente_transparencia/src/model/hecho_a_sellar.dart';
import 'package:aportaya_cliente_transparencia/src/model/hechos_del_usuario.dart';
import 'package:aportaya_cliente_transparencia/src/model/indicadores.dart';
import 'package:aportaya_cliente_transparencia/src/model/medicion.dart';
import 'package:aportaya_cliente_transparencia/src/model/metrica_de_riesgo.dart';
import 'package:aportaya_cliente_transparencia/src/model/problema.dart';
import 'package:aportaya_cliente_transparencia/src/model/puntaje_del_usuario.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_certificado.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_evaluacion_riesgo.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_evento_reputacion.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_insignia.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_puntaje.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_resena.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_sellado.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_snapshot.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_verificacion_cadena.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_verificacion_certificado.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_verificacion_sorteo.dart';
import 'package:aportaya_cliente_transparencia/src/model/salida_verificacion_sorteo_paquete.dart';

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
        case 'AlertaDeRiesgo':
          return AlertaDeRiesgo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'BloquePublicado':
          return BloquePublicado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Componente':
          return Componente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCertificado':
          return EntradaCertificado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCierreAlerta':
          return EntradaCierreAlerta.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaCompensacion':
          return EntradaCompensacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEvaluacionInsignias':
          return EntradaEvaluacionInsignias.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEvaluacionRiesgo':
          return EntradaEvaluacionRiesgo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaEventoReputacion':
          return EntradaEventoReputacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaModeracion':
          return EntradaModeracion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRecalculo':
          return EntradaRecalculo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaResena':
          return EntradaResena.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRevocacionCertificado':
          return EntradaRevocacionCertificado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRevocacionInsignia':
          return EntradaRevocacionInsignia.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaSellado':
          return EntradaSellado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaSnapshot':
          return EntradaSnapshot.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'HechoASellar':
          return HechoASellar.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'HechosDelUsuario':
          return HechosDelUsuario.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Indicadores':
          return Indicadores.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Medicion':
          return Medicion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'MetricaDeRiesgo':
          return MetricaDeRiesgo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Problema':
          return Problema.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PuntajeDelUsuario':
          return PuntajeDelUsuario.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaCertificado':
          return SalidaCertificado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEvaluacionRiesgo':
          return SalidaEvaluacionRiesgo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaEventoReputacion':
          return SalidaEventoReputacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaInsignia':
          return SalidaInsignia.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaPuntaje':
          return SalidaPuntaje.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaResena':
          return SalidaResena.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSellado':
          return SalidaSellado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaSnapshot':
          return SalidaSnapshot.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaVerificacionCadena':
          return SalidaVerificacionCadena.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaVerificacionCertificado':
          return SalidaVerificacionCertificado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaVerificacionSorteo':
          return SalidaVerificacionSorteo.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaVerificacionSorteoPaquete':
          return SalidaVerificacionSorteoPaquete.fromJson(value as Map<String, dynamic>) as ReturnType;
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