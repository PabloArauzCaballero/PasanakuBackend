//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'firmar_contrato_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class FirmarContratoRequest {
  /// Returns a new [FirmarContratoRequest] instance.
  FirmarContratoRequest({

    required  this.tokenFirmaId,
  });

  @JsonKey(
    
    name: r'tokenFirmaId',
    required: true,
    includeIfNull: false,
  )


  final String tokenFirmaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is FirmarContratoRequest &&
      other.tokenFirmaId == tokenFirmaId;

    @override
    int get hashCode =>
        tokenFirmaId.hashCode;

  factory FirmarContratoRequest.fromJson(Map<String, dynamic> json) => _$FirmarContratoRequestFromJson(json);

  Map<String, dynamic> toJson() => _$FirmarContratoRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

