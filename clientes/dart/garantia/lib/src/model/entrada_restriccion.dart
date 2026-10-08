//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_restriccion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRestriccion {
  /// Returns a new [EntradaRestriccion] instance.
  EntradaRestriccion({

    required  this.nivel,

    required  this.motivo,

     this.duracionDias,
  });

  @JsonKey(
    
    name: r'nivel',
    required: true,
    includeIfNull: false,
  )


  final EntradaRestriccionNivelEnum nivel;



      /// obligatorio. «Deudor» a secas no le dice a la persona contra que puede apelar
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



      /// null la hace indefinida; solo se levanta con motivo escrito
          // minimum: 1
  @JsonKey(
    
    name: r'duracionDias',
    required: false,
    includeIfNull: false,
  )


  final int? duracionDias;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRestriccion &&
      other.nivel == nivel &&
      other.motivo == motivo &&
      other.duracionDias == duracionDias;

    @override
    int get hashCode =>
        nivel.hashCode +
        motivo.hashCode +
        (duracionDias == null ? 0 : duracionDias.hashCode);

  factory EntradaRestriccion.fromJson(Map<String, dynamic> json) => _$EntradaRestriccionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRestriccionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaRestriccionNivelEnum {
@JsonValue(r'OBSERVACION')
OBSERVACION(r'OBSERVACION'),
@JsonValue(r'LIMITADO')
LIMITADO(r'LIMITADO'),
@JsonValue(r'VETADO')
VETADO(r'VETADO');

const EntradaRestriccionNivelEnum(this.value);

final String value;

@override
String toString() => value;
}


