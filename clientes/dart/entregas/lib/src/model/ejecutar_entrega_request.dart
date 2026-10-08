//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'ejecutar_entrega_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EjecutarEntregaRequest {
  /// Returns a new [EjecutarEntregaRequest] instance.
  EjecutarEntregaRequest({

    required  this.montoEntregado,
  });

  @JsonKey(
    
    name: r'montoEntregado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoEntregado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EjecutarEntregaRequest &&
      other.montoEntregado == montoEntregado;

    @override
    int get hashCode =>
        montoEntregado.hashCode;

  factory EjecutarEntregaRequest.fromJson(Map<String, dynamic> json) => _$EjecutarEntregaRequestFromJson(json);

  Map<String, dynamic> toJson() => _$EjecutarEntregaRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

