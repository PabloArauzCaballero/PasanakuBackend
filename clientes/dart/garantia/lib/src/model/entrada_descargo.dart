//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_descargo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDescargo {
  /// Returns a new [EntradaDescargo] instance.
  EntradaDescargo({

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
    bool operator ==(Object other) => identical(this, other) || other is EntradaDescargo &&
      other.argumento == argumento &&
      other.evidenciasJson == evidenciasJson;

    @override
    int get hashCode =>
        argumento.hashCode +
        evidenciasJson.hashCode;

  factory EntradaDescargo.fromJson(Map<String, dynamic> json) => _$EntradaDescargoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDescargoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

