//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'rescindir_contrato_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RescindirContratoRequest {
  /// Returns a new [RescindirContratoRequest] instance.
  RescindirContratoRequest({

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RescindirContratoRequest &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        motivo.hashCode;

  factory RescindirContratoRequest.fromJson(Map<String, dynamic> json) => _$RescindirContratoRequestFromJson(json);

  Map<String, dynamic> toJson() => _$RescindirContratoRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

