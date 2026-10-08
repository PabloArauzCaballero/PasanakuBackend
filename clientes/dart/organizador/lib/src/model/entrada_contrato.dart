//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_contrato.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaContrato {
  /// Returns a new [EntradaContrato] instance.
  EntradaContrato({

    required  this.version,

    required  this.contenidoHash,

    required  this.obligaciones,

    required  this.causalesRescision,
  });

  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final String version;



  @JsonKey(
    
    name: r'contenidoHash',
    required: true,
    includeIfNull: false,
  )


  final String contenidoHash;



  @JsonKey(
    
    name: r'obligaciones',
    required: true,
    includeIfNull: false,
  )


  final String obligaciones;



      /// obligatorias. Un contrato que no dice por que se puede terminar deja al organizador sin saber que lo expone a perder el vinculo
  @JsonKey(
    
    name: r'causalesRescision',
    required: true,
    includeIfNull: false,
  )


  final String causalesRescision;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaContrato &&
      other.version == version &&
      other.contenidoHash == contenidoHash &&
      other.obligaciones == obligaciones &&
      other.causalesRescision == causalesRescision;

    @override
    int get hashCode =>
        version.hashCode +
        contenidoHash.hashCode +
        obligaciones.hashCode +
        causalesRescision.hashCode;

  factory EntradaContrato.fromJson(Map<String, dynamic> json) => _$EntradaContratoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaContratoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

