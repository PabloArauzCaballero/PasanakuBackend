//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_publicidad/src/model/conjunto.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_campana.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCampana {
  /// Returns a new [EntradaCampana] instance.
  EntradaCampana({

    required  this.cuentaPublicitariaId,

    required  this.nombre,

    required  this.objetivo,

    required  this.presupuestoTotal,

    required  this.moneda,

    required  this.fechaInicio,

     this.fechaFin,

    required  this.conjuntos,
  });

  @JsonKey(
    
    name: r'cuentaPublicitariaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaPublicitariaId;



  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(
    
    name: r'objetivo',
    required: true,
    includeIfNull: false,
  )


  final EntradaCampanaObjetivoEnum objetivo;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'presupuestoTotal',
    required: true,
    includeIfNull: false,
  )


  final String presupuestoTotal;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaCampanaMonedaEnum moneda;



  @JsonKey(
    
    name: r'fechaInicio',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaInicio;



  @JsonKey(
    
    name: r'fechaFin',
    required: false,
    includeIfNull: false,
  )


  final DateTime? fechaFin;



  @JsonKey(
    
    name: r'conjuntos',
    required: true,
    includeIfNull: false,
  )


  final List<Conjunto> conjuntos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCampana &&
      other.cuentaPublicitariaId == cuentaPublicitariaId &&
      other.nombre == nombre &&
      other.objetivo == objetivo &&
      other.presupuestoTotal == presupuestoTotal &&
      other.moneda == moneda &&
      other.fechaInicio == fechaInicio &&
      other.fechaFin == fechaFin &&
      other.conjuntos == conjuntos;

    @override
    int get hashCode =>
        cuentaPublicitariaId.hashCode +
        nombre.hashCode +
        objetivo.hashCode +
        presupuestoTotal.hashCode +
        moneda.hashCode +
        fechaInicio.hashCode +
        (fechaFin == null ? 0 : fechaFin.hashCode) +
        conjuntos.hashCode;

  factory EntradaCampana.fromJson(Map<String, dynamic> json) => _$EntradaCampanaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCampanaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCampanaObjetivoEnum {
@JsonValue(r'VISIBILIDAD_MARCA')
VISIBILIDAD_MARCA(r'VISIBILIDAD_MARCA'),
@JsonValue(r'TRAFICO')
TRAFICO(r'TRAFICO'),
@JsonValue(r'POSTULACION_GRUPO')
POSTULACION_GRUPO(r'POSTULACION_GRUPO'),
@JsonValue(r'DESCARGA_APP')
DESCARGA_APP(r'DESCARGA_APP'),
@JsonValue(r'CONVERSION')
CONVERSION(r'CONVERSION');

const EntradaCampanaObjetivoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaCampanaMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaCampanaMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


