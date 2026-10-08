//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'verificar_cuenta_destino_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class VerificarCuentaDestinoRequest {
  /// Returns a new [VerificarCuentaDestinoRequest] instance.
  VerificarCuentaDestinoRequest({

    required  this.metodo,
  });

  @JsonKey(
    
    name: r'metodo',
    required: true,
    includeIfNull: false,
  )


  final String metodo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is VerificarCuentaDestinoRequest &&
      other.metodo == metodo;

    @override
    int get hashCode =>
        metodo.hashCode;

  factory VerificarCuentaDestinoRequest.fromJson(Map<String, dynamic> json) => _$VerificarCuentaDestinoRequestFromJson(json);

  Map<String, dynamic> toJson() => _$VerificarCuentaDestinoRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

