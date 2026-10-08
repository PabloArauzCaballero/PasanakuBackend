//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_apelacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaApelacion {
  /// Returns a new [EntradaApelacion] instance.
  EntradaApelacion({

    required  this.argumento,

    required  this.evidenciasJson,
  });

  @JsonKey(
    
    name: r'argumento',
    required: true,
    includeIfNull: false,
  )


  final String argumento;



  @JsonKey(
    
    name: r'evidenciasJson',
    required: true,
    includeIfNull: false,
  )


  final String evidenciasJson;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaApelacion &&
      other.argumento == argumento &&
      other.evidenciasJson == evidenciasJson;

    @override
    int get hashCode =>
        argumento.hashCode +
        evidenciasJson.hashCode;

  factory EntradaApelacion.fromJson(Map<String, dynamic> json) => _$EntradaApelacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaApelacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

