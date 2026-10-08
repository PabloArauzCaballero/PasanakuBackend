//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_postulacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPostulacion {
  /// Returns a new [EntradaPostulacion] instance.
  EntradaPostulacion({

    required  this.cuposSolicitados,

     this.mensaje,
  });

          // minimum: 1
          // maximum: 3
  @JsonKey(
    
    name: r'cuposSolicitados',
    required: true,
    includeIfNull: false,
  )


  final int cuposSolicitados;



  @JsonKey(
    
    name: r'mensaje',
    required: false,
    includeIfNull: false,
  )


  final String? mensaje;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPostulacion &&
      other.cuposSolicitados == cuposSolicitados &&
      other.mensaje == mensaje;

    @override
    int get hashCode =>
        cuposSolicitados.hashCode +
        (mensaje == null ? 0 : mensaje.hashCode);

  factory EntradaPostulacion.fromJson(Map<String, dynamic> json) => _$EntradaPostulacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPostulacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

