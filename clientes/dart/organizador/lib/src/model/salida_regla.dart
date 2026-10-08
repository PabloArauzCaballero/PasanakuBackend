//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_regla.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRegla {
  /// Returns a new [SalidaRegla] instance.
  SalidaRegla({

    required  this.reglaId,

    required  this.codigo,

    required  this.activa,

    required  this.requiereConfirmacionHumana,
  });

  @JsonKey(
    
    name: r'reglaId',
    required: true,
    includeIfNull: false,
  )


  final String reglaId;



  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'activa',
    required: true,
    includeIfNull: false,
  )


  final bool activa;



  @JsonKey(
    
    name: r'requiereConfirmacionHumana',
    required: true,
    includeIfNull: false,
  )


  final bool requiereConfirmacionHumana;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRegla &&
      other.reglaId == reglaId &&
      other.codigo == codigo &&
      other.activa == activa &&
      other.requiereConfirmacionHumana == requiereConfirmacionHumana;

    @override
    int get hashCode =>
        reglaId.hashCode +
        codigo.hashCode +
        activa.hashCode +
        requiereConfirmacionHumana.hashCode;

  factory SalidaRegla.fromJson(Map<String, dynamic> json) => _$SalidaReglaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaReglaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

