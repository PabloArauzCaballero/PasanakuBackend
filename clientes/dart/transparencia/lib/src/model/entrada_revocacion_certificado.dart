//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_revocacion_certificado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRevocacionCertificado {
  /// Returns a new [EntradaRevocacionCertificado] instance.
  EntradaRevocacionCertificado({

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRevocacionCertificado &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        motivo.hashCode;

  factory EntradaRevocacionCertificado.fromJson(Map<String, dynamic> json) => _$EntradaRevocacionCertificadoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRevocacionCertificadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

