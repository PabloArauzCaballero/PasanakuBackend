//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_bloqueo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaBloqueo {
  /// Returns a new [EntradaBloqueo] instance.
  EntradaBloqueo({

    required  this.cuentaBilleteraId,

    required  this.autoridad,

    required  this.tipoOrden,

    required  this.numeroOficio,

     this.montoBloqueado,

    required  this.alcance,

    required  this.documentoUrl,

    required  this.hashDocumento,
  });

  @JsonKey(
    
    name: r'cuentaBilleteraId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaBilleteraId;



  @JsonKey(
    
    name: r'autoridad',
    required: true,
    includeIfNull: false,
  )


  final EntradaBloqueoAutoridadEnum autoridad;



  @JsonKey(
    
    name: r'tipoOrden',
    required: true,
    includeIfNull: false,
  )


  final EntradaBloqueoTipoOrdenEnum tipoOrden;



  @JsonKey(
    
    name: r'numeroOficio',
    required: true,
    includeIfNull: false,
  )


  final String numeroOficio;



  @JsonKey(
    
    name: r'montoBloqueado',
    required: false,
    includeIfNull: false,
  )


  final Dinero? montoBloqueado;



  @JsonKey(
    
    name: r'alcance',
    required: true,
    includeIfNull: false,
  )


  final EntradaBloqueoAlcanceEnum alcance;



  @JsonKey(
    
    name: r'documentoUrl',
    required: true,
    includeIfNull: false,
  )


  final String documentoUrl;



  @JsonKey(
    
    name: r'hashDocumento',
    required: true,
    includeIfNull: false,
  )


  final String hashDocumento;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaBloqueo &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.autoridad == autoridad &&
      other.tipoOrden == tipoOrden &&
      other.numeroOficio == numeroOficio &&
      other.montoBloqueado == montoBloqueado &&
      other.alcance == alcance &&
      other.documentoUrl == documentoUrl &&
      other.hashDocumento == hashDocumento;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        autoridad.hashCode +
        tipoOrden.hashCode +
        numeroOficio.hashCode +
        montoBloqueado.hashCode +
        alcance.hashCode +
        documentoUrl.hashCode +
        hashDocumento.hashCode;

  factory EntradaBloqueo.fromJson(Map<String, dynamic> json) => _$EntradaBloqueoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaBloqueoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaBloqueoAutoridadEnum {
@JsonValue(r'ASFI')
ASFI(r'ASFI'),
@JsonValue(r'BCB')
BCB(r'BCB'),
@JsonValue(r'FISCALIA')
FISCALIA(r'FISCALIA'),
@JsonValue(r'JUZGADO')
JUZGADO(r'JUZGADO'),
@JsonValue(r'UIF')
UIF(r'UIF'),
@JsonValue(r'INTERNO')
INTERNO(r'INTERNO');

const EntradaBloqueoAutoridadEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaBloqueoTipoOrdenEnum {
@JsonValue(r'CONGELAMIENTO')
CONGELAMIENTO(r'CONGELAMIENTO'),
@JsonValue(r'EMBARGO')
EMBARGO(r'EMBARGO'),
@JsonValue(r'INFORMATIVO')
INFORMATIVO(r'INFORMATIVO'),
@JsonValue(r'INMOVILIZACION')
INMOVILIZACION(r'INMOVILIZACION'),
@JsonValue(r'RETENCION')
RETENCION(r'RETENCION');

const EntradaBloqueoTipoOrdenEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaBloqueoAlcanceEnum {
@JsonValue(r'PARCIAL')
PARCIAL(r'PARCIAL'),
@JsonValue(r'TOTAL')
TOTAL(r'TOTAL');

const EntradaBloqueoAlcanceEnum(this.value);

final String value;

@override
String toString() => value;
}


