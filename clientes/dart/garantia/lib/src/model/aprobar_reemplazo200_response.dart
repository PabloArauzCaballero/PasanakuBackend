//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'aprobar_reemplazo200_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AprobarReemplazo200Response {
  /// Returns a new [AprobarReemplazo200Response] instance.
  AprobarReemplazo200Response({

    required  this.aprobado,
  });

  @JsonKey(
    
    name: r'aprobado',
    required: true,
    includeIfNull: false,
  )


  final bool aprobado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AprobarReemplazo200Response &&
      other.aprobado == aprobado;

    @override
    int get hashCode =>
        aprobado.hashCode;

  factory AprobarReemplazo200Response.fromJson(Map<String, dynamic> json) => _$AprobarReemplazo200ResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AprobarReemplazo200ResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

