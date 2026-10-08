//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'conjunto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Conjunto {
  /// Returns a new [Conjunto] instance.
  Conjunto({

    required  this.segmentoAudienciaId,

    required  this.espacioPublicitarioId,

    required  this.nombre,

    required  this.presupuestoDiario,

    required  this.pujaMaxima,

    required  this.modeloPuja,
  });

  @JsonKey(
    
    name: r'segmentoAudienciaId',
    required: true,
    includeIfNull: false,
  )


  final String segmentoAudienciaId;



  @JsonKey(
    
    name: r'espacioPublicitarioId',
    required: true,
    includeIfNull: false,
  )


  final String espacioPublicitarioId;



  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'presupuestoDiario',
    required: true,
    includeIfNull: false,
  )


  final String presupuestoDiario;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'pujaMaxima',
    required: true,
    includeIfNull: false,
  )


  final String pujaMaxima;



  @JsonKey(
    
    name: r'modeloPuja',
    required: true,
    includeIfNull: false,
  )


  final ConjuntoModeloPujaEnum modeloPuja;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Conjunto &&
      other.segmentoAudienciaId == segmentoAudienciaId &&
      other.espacioPublicitarioId == espacioPublicitarioId &&
      other.nombre == nombre &&
      other.presupuestoDiario == presupuestoDiario &&
      other.pujaMaxima == pujaMaxima &&
      other.modeloPuja == modeloPuja;

    @override
    int get hashCode =>
        segmentoAudienciaId.hashCode +
        espacioPublicitarioId.hashCode +
        nombre.hashCode +
        presupuestoDiario.hashCode +
        pujaMaxima.hashCode +
        modeloPuja.hashCode;

  factory Conjunto.fromJson(Map<String, dynamic> json) => _$ConjuntoFromJson(json);

  Map<String, dynamic> toJson() => _$ConjuntoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ConjuntoModeloPujaEnum {
@JsonValue(r'CPM')
CPM(r'CPM'),
@JsonValue(r'CPC')
CPC(r'CPC');

const ConjuntoModeloPujaEnum(this.value);

final String value;

@override
String toString() => value;
}


