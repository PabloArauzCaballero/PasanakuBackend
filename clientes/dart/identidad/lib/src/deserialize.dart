import 'package:aportaya_cliente_identidad/src/model/archivo_del_expediente.dart';
import 'package:aportaya_cliente_identidad/src/model/decision_de_verificacion.dart';
import 'package:aportaya_cliente_identidad/src/model/dinero.dart';
import 'package:aportaya_cliente_identidad/src/model/documento.dart';
import 'package:aportaya_cliente_identidad/src/model/enlace_de_foto.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_autenticacion.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_registro.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_titularidad.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_token_de_invitacion.dart';
import 'package:aportaya_cliente_identidad/src/model/error.dart';
import 'package:aportaya_cliente_identidad/src/model/expediente_en_revision.dart';
import 'package:aportaya_cliente_identidad/src/model/factor_presentado.dart';
import 'package:aportaya_cliente_identidad/src/model/limite_aplicable.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_autenticacion.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_registro.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_titularidad.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_token_de_invitacion.dart';
import 'package:aportaya_cliente_identidad/src/model/usuario_encontrado.dart';

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
        case 'ArchivoDelExpediente':
          return ArchivoDelExpediente.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DecisionDeVerificacion':
          return DecisionDeVerificacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Dinero':
          return Dinero.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Documento':
          return Documento.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EnlaceDeFoto':
          return EnlaceDeFoto.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaAutenticacion':
          return EntradaAutenticacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaRegistro':
          return EntradaRegistro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTitularidad':
          return EntradaTitularidad.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'EntradaTokenDeInvitacion':
          return EntradaTokenDeInvitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Error':
          return Error.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ExpedienteEnRevision':
          return ExpedienteEnRevision.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'FactorPresentado':
          return FactorPresentado.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LimiteAplicable':
          return LimiteAplicable.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaAutenticacion':
          return SalidaAutenticacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaRegistro':
          return SalidaRegistro.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaTitularidad':
          return SalidaTitularidad.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SalidaTokenDeInvitacion':
          return SalidaTokenDeInvitacion.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UsuarioEncontrado':
          return UsuarioEncontrado.fromJson(value as Map<String, dynamic>) as ReturnType;
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