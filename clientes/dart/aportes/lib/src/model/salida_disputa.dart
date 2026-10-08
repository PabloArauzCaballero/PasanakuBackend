//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_disputa.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDisputa {
  /// Returns a new [SalidaDisputa] instance.
  SalidaDisputa({

     this.disputaId,

    required  this.fechaLimiteRespuesta,

    required  this.esNueva,
  });

      /// null cuando el proveedor reenvio una disputa ya abierta
  @JsonKey(
    
    name: r'disputaId',
    required: false,
    includeIfNull: false,
  )


  final String? disputaId;



  @JsonKey(
    
    name: r'fechaLimiteRespuesta',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaLimiteRespuesta;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDisputa &&
      other.disputaId == disputaId &&
      other.fechaLimiteRespuesta == fechaLimiteRespuesta &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        (disputaId == null ? 0 : disputaId.hashCode) +
        fechaLimiteRespuesta.hashCode +
        esNueva.hashCode;

  factory SalidaDisputa.fromJson(Map<String, dynamic> json) => _$SalidaDisputaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDisputaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

