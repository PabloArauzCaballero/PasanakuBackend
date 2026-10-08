//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_declaracion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDeclaracion {
  /// Returns a new [EntradaDeclaracion] instance.
  EntradaDeclaracion({

    required  this.codigoExpediente,

    required  this.usuarioId,

    required  this.participanteId,

    required  this.grupoId,

     this.periodoId,

     this.cupoId,

    required  this.obligacionId,

    required  this.tipo,

    required  this.severidad,

    required  this.origenDeteccion,

    required  this.montoInvolucrado,

    required  this.diasMora,

     this.afectoALaEntrega = false,

    required  this.tipoDeEvidencia,

    required  this.descripcionDeLaEvidencia,

     this.urlDeLaEvidencia,

     this.hashDeLaEvidencia,
  });

  @JsonKey(
    
    name: r'codigoExpediente',
    required: true,
    includeIfNull: false,
  )


  final String codigoExpediente;



  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'participanteId',
    required: true,
    includeIfNull: false,
  )


  final String participanteId;



  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'periodoId',
    required: false,
    includeIfNull: false,
  )


  final String? periodoId;



  @JsonKey(
    
    name: r'cupoId',
    required: false,
    includeIfNull: false,
  )


  final String? cupoId;



  @JsonKey(
    
    name: r'obligacionId',
    required: true,
    includeIfNull: false,
  )


  final String obligacionId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaDeclaracionTipoEnum tipo;



  @JsonKey(
    
    name: r'severidad',
    required: true,
    includeIfNull: false,
  )


  final EntradaDeclaracionSeveridadEnum severidad;



  @JsonKey(
    
    name: r'origenDeteccion',
    required: true,
    includeIfNull: false,
  )


  final EntradaDeclaracionOrigenDeteccionEnum origenDeteccion;



  @JsonKey(
    
    name: r'montoInvolucrado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoInvolucrado;



          // minimum: 0
  @JsonKey(
    
    name: r'diasMora',
    required: true,
    includeIfNull: false,
  )


  final int diasMora;



  @JsonKey(
    defaultValue: false,
    name: r'afectoALaEntrega',
    required: false,
    includeIfNull: false,
  )


  final bool? afectoALaEntrega;



  @JsonKey(
    
    name: r'tipoDeEvidencia',
    required: true,
    includeIfNull: false,
  )


  final EntradaDeclaracionTipoDeEvidenciaEnum tipoDeEvidencia;



      /// obligatoria. Declarar sin con que probarlo deja al participante sin nada contra que defenderse
  @JsonKey(
    
    name: r'descripcionDeLaEvidencia',
    required: true,
    includeIfNull: false,
  )


  final String descripcionDeLaEvidencia;



  @JsonKey(
    
    name: r'urlDeLaEvidencia',
    required: false,
    includeIfNull: false,
  )


  final String? urlDeLaEvidencia;



  @JsonKey(
    
    name: r'hashDeLaEvidencia',
    required: false,
    includeIfNull: false,
  )


  final String? hashDeLaEvidencia;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDeclaracion &&
      other.codigoExpediente == codigoExpediente &&
      other.usuarioId == usuarioId &&
      other.participanteId == participanteId &&
      other.grupoId == grupoId &&
      other.periodoId == periodoId &&
      other.cupoId == cupoId &&
      other.obligacionId == obligacionId &&
      other.tipo == tipo &&
      other.severidad == severidad &&
      other.origenDeteccion == origenDeteccion &&
      other.montoInvolucrado == montoInvolucrado &&
      other.diasMora == diasMora &&
      other.afectoALaEntrega == afectoALaEntrega &&
      other.tipoDeEvidencia == tipoDeEvidencia &&
      other.descripcionDeLaEvidencia == descripcionDeLaEvidencia &&
      other.urlDeLaEvidencia == urlDeLaEvidencia &&
      other.hashDeLaEvidencia == hashDeLaEvidencia;

    @override
    int get hashCode =>
        codigoExpediente.hashCode +
        usuarioId.hashCode +
        participanteId.hashCode +
        grupoId.hashCode +
        (periodoId == null ? 0 : periodoId.hashCode) +
        (cupoId == null ? 0 : cupoId.hashCode) +
        obligacionId.hashCode +
        tipo.hashCode +
        severidad.hashCode +
        origenDeteccion.hashCode +
        montoInvolucrado.hashCode +
        diasMora.hashCode +
        afectoALaEntrega.hashCode +
        tipoDeEvidencia.hashCode +
        descripcionDeLaEvidencia.hashCode +
        (urlDeLaEvidencia == null ? 0 : urlDeLaEvidencia.hashCode) +
        (hashDeLaEvidencia == null ? 0 : hashDeLaEvidencia.hashCode);

  factory EntradaDeclaracion.fromJson(Map<String, dynamic> json) => _$EntradaDeclaracionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDeclaracionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDeclaracionTipoEnum {
@JsonValue(r'ABANDONO_DE_GRUPO')
ABANDONO_DE_GRUPO(r'ABANDONO_DE_GRUPO'),
@JsonValue(r'APORTE_ATRASADO')
APORTE_ATRASADO(r'APORTE_ATRASADO'),
@JsonValue(r'APORTE_IMPAGO')
APORTE_IMPAGO(r'APORTE_IMPAGO'),
@JsonValue(r'APORTE_PARCIAL')
APORTE_PARCIAL(r'APORTE_PARCIAL'),
@JsonValue(r'BENEFICIARIO_NO_CONTINUA_APORTANDO')
BENEFICIARIO_NO_CONTINUA_APORTANDO(r'BENEFICIARIO_NO_CONTINUA_APORTANDO'),
@JsonValue(r'COMPROBANTE_FALSO')
COMPROBANTE_FALSO(r'COMPROBANTE_FALSO'),
@JsonValue(r'FRAUDE_CONFIRMADO')
FRAUDE_CONFIRMADO(r'FRAUDE_CONFIRMADO'),
@JsonValue(r'INCUMPLIMIENTO_AVAL')
INCUMPLIMIENTO_AVAL(r'INCUMPLIMIENTO_AVAL'),
@JsonValue(r'INCUMPLIMIENTO_ORGANIZADOR')
INCUMPLIMIENTO_ORGANIZADOR(r'INCUMPLIMIENTO_ORGANIZADOR'),
@JsonValue(r'INCUMPLIMIENTO_PLAN_REGULARIZACION')
INCUMPLIMIENTO_PLAN_REGULARIZACION(r'INCUMPLIMIENTO_PLAN_REGULARIZACION'),
@JsonValue(r'PAGO_RECHAZADO_O_REVERSADO')
PAGO_RECHAZADO_O_REVERSADO(r'PAGO_RECHAZADO_O_REVERSADO'),
@JsonValue(r'USO_INDEBIDO_DE_LA_PLATAFORMA')
USO_INDEBIDO_DE_LA_PLATAFORMA(r'USO_INDEBIDO_DE_LA_PLATAFORMA');

const EntradaDeclaracionTipoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaDeclaracionSeveridadEnum {
@JsonValue(r'LEVE')
LEVE(r'LEVE'),
@JsonValue(r'MODERADA')
MODERADA(r'MODERADA'),
@JsonValue(r'GRAVE')
GRAVE(r'GRAVE'),
@JsonValue(r'CRITICA')
CRITICA(r'CRITICA');

const EntradaDeclaracionSeveridadEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaDeclaracionOrigenDeteccionEnum {
@JsonValue(r'ALERTA_DE_RIESGO')
ALERTA_DE_RIESGO(r'ALERTA_DE_RIESGO'),
@JsonValue(r'AUDITORIA_INTERNA')
AUDITORIA_INTERNA(r'AUDITORIA_INTERNA'),
@JsonValue(r'AUTOMATICO_CONCILIACION')
AUTOMATICO_CONCILIACION(r'AUTOMATICO_CONCILIACION'),
@JsonValue(r'AUTOMATICO_VENCIMIENTO')
AUTOMATICO_VENCIMIENTO(r'AUTOMATICO_VENCIMIENTO'),
@JsonValue(r'REPORTE_DE_ORGANIZADOR')
REPORTE_DE_ORGANIZADOR(r'REPORTE_DE_ORGANIZADOR'),
@JsonValue(r'REPORTE_DE_PARTICIPANTE')
REPORTE_DE_PARTICIPANTE(r'REPORTE_DE_PARTICIPANTE');

const EntradaDeclaracionOrigenDeteccionEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaDeclaracionTipoDeEvidenciaEnum {
@JsonValue(r'ACTA_ACUERDO')
ACTA_ACUERDO(r'ACTA_ACUERDO'),
@JsonValue(r'CAPTURA_ESTADO')
CAPTURA_ESTADO(r'CAPTURA_ESTADO'),
@JsonValue(r'COMUNICACION')
COMUNICACION(r'COMUNICACION'),
@JsonValue(r'DOCUMENTO')
DOCUMENTO(r'DOCUMENTO'),
@JsonValue(r'EXTRACTO')
EXTRACTO(r'EXTRACTO'),
@JsonValue(r'LOG_SISTEMA')
LOG_SISTEMA(r'LOG_SISTEMA');

const EntradaDeclaracionTipoDeEvidenciaEnum(this.value);

final String value;

@override
String toString() => value;
}


