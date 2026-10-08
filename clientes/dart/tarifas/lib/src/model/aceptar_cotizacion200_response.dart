//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'aceptar_cotizacion200_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AceptarCotizacion200Response {
  /// Returns a new [AceptarCotizacion200Response] instance.
  AceptarCotizacion200Response({

    required  this.aceptada,
  });

  @JsonKey(
    
    name: r'aceptada',
    required: true,
    includeIfNull: false,
  )


  final bool aceptada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AceptarCotizacion200Response &&
      other.aceptada == aceptada;

    @override
    int get hashCode =>
        aceptada.hashCode;

  factory AceptarCotizacion200Response.fromJson(Map<String, dynamic> json) => _$AceptarCotizacion200ResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AceptarCotizacion200ResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

