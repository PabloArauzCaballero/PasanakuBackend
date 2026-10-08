//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_titularidad.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTitularidad {
  /// Returns a new [EntradaTitularidad] instance.
  EntradaTitularidad({

    required  this.nombreCompleto,

    required  this.documento,
  });

  @JsonKey(
    
    name: r'nombreCompleto',
    required: true,
    includeIfNull: false,
  )


  final String nombreCompleto;



  @JsonKey(
    
    name: r'documento',
    required: true,
    includeIfNull: false,
  )


  final String documento;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTitularidad &&
      other.nombreCompleto == nombreCompleto &&
      other.documento == documento;

    @override
    int get hashCode =>
        nombreCompleto.hashCode +
        documento.hashCode;

  factory EntradaTitularidad.fromJson(Map<String, dynamic> json) => _$EntradaTitularidadFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTitularidadToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

