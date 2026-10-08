//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_sancion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaSancion {
  /// Returns a new [EntradaSancion] instance.
  EntradaSancion({

     this.evaluacionId,

    required  this.tipo,

    required  this.motivo,

     this.duracionDias,
  });

  @JsonKey(
    
    name: r'evaluacionId',
    required: false,
    includeIfNull: false,
  )


  final String? evaluacionId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaSancionTipoEnum tipo;



      /// obligatorio y escrito. «Bajo desempeno» a secas no se puede apelar porque no dice contra que defenderse
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



          // minimum: 1
  @JsonKey(
    
    name: r'duracionDias',
    required: false,
    includeIfNull: false,
  )


  final int? duracionDias;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaSancion &&
      other.evaluacionId == evaluacionId &&
      other.tipo == tipo &&
      other.motivo == motivo &&
      other.duracionDias == duracionDias;

    @override
    int get hashCode =>
        (evaluacionId == null ? 0 : evaluacionId.hashCode) +
        tipo.hashCode +
        motivo.hashCode +
        (duracionDias == null ? 0 : duracionDias.hashCode);

  factory EntradaSancion.fromJson(Map<String, dynamic> json) => _$EntradaSancionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaSancionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaSancionTipoEnum {
@JsonValue(r'ADVERTENCIA')
ADVERTENCIA(r'ADVERTENCIA'),
@JsonValue(r'REDUCCION_LIMITE')
REDUCCION_LIMITE(r'REDUCCION_LIMITE'),
@JsonValue(r'SUSPENSION')
SUSPENSION(r'SUSPENSION'),
@JsonValue(r'INHABILITACION')
INHABILITACION(r'INHABILITACION');

const EntradaSancionTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


