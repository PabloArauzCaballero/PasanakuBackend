//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_acuerdo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAcuerdo {
  /// Returns a new [SalidaAcuerdo] instance.
  SalidaAcuerdo({

    required  this.acuerdoId,

    required  this.estado,
  });

  @JsonKey(
    
    name: r'acuerdoId',
    required: true,
    includeIfNull: false,
  )


  final String acuerdoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAcuerdo &&
      other.acuerdoId == acuerdoId &&
      other.estado == estado;

    @override
    int get hashCode =>
        acuerdoId.hashCode +
        estado.hashCode;

  factory SalidaAcuerdo.fromJson(Map<String, dynamic> json) => _$SalidaAcuerdoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAcuerdoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

