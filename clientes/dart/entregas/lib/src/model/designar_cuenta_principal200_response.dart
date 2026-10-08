//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'designar_cuenta_principal200_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DesignarCuentaPrincipal200Response {
  /// Returns a new [DesignarCuentaPrincipal200Response] instance.
  DesignarCuentaPrincipal200Response({

    required  this.designada,
  });

  @JsonKey(
    
    name: r'designada',
    required: true,
    includeIfNull: false,
  )


  final bool designada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DesignarCuentaPrincipal200Response &&
      other.designada == designada;

    @override
    int get hashCode =>
        designada.hashCode;

  factory DesignarCuentaPrincipal200Response.fromJson(Map<String, dynamic> json) => _$DesignarCuentaPrincipal200ResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DesignarCuentaPrincipal200ResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

