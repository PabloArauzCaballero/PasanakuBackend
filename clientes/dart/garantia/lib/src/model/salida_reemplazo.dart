//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_reemplazo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaReemplazo {
  /// Returns a new [SalidaReemplazo] instance.
  SalidaReemplazo({

    required  this.reemplazoId,

    required  this.estado,

     this.deudaAsumidaPorElEntrante,

     this.deudaRetenidaPorElSaliente,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'reemplazoId',
    required: true,
    includeIfNull: false,
  )


  final String reemplazoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaReemplazoEstadoEnum estado;



  @JsonKey(
    
    name: r'deudaAsumidaPorElEntrante',
    required: false,
    includeIfNull: false,
  )


  final Dinero? deudaAsumidaPorElEntrante;



  @JsonKey(
    
    name: r'deudaRetenidaPorElSaliente',
    required: false,
    includeIfNull: false,
  )


  final Dinero? deudaRetenidaPorElSaliente;



  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaReemplazo &&
      other.reemplazoId == reemplazoId &&
      other.estado == estado &&
      other.deudaAsumidaPorElEntrante == deudaAsumidaPorElEntrante &&
      other.deudaRetenidaPorElSaliente == deudaRetenidaPorElSaliente &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        reemplazoId.hashCode +
        estado.hashCode +
        deudaAsumidaPorElEntrante.hashCode +
        deudaRetenidaPorElSaliente.hashCode +
        esNuevo.hashCode;

  factory SalidaReemplazo.fromJson(Map<String, dynamic> json) => _$SalidaReemplazoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaReemplazoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaReemplazoEstadoEnum {
@JsonValue(r'BUSCANDO')
BUSCANDO(r'BUSCANDO'),
@JsonValue(r'PROPUESTO')
PROPUESTO(r'PROPUESTO'),
@JsonValue(r'APROBADO')
APROBADO(r'APROBADO'),
@JsonValue(r'EJECUTADO')
EJECUTADO(r'EJECUTADO'),
@JsonValue(r'FALLIDO')
FALLIDO(r'FALLIDO');

const SalidaReemplazoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


