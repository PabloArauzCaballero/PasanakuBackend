//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'resolver_precio_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResolverPrecioRequest {
  /// Returns a new [ResolverPrecioRequest] instance.
  ResolverPrecioRequest({

    required  this.hechos,
  });

  @JsonKey(
    
    name: r'hechos',
    required: true,
    includeIfNull: false,
  )


  final Map<String, int> hechos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResolverPrecioRequest &&
      other.hechos == hechos;

    @override
    int get hashCode =>
        hechos.hashCode;

  factory ResolverPrecioRequest.fromJson(Map<String, dynamic> json) => _$ResolverPrecioRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ResolverPrecioRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

