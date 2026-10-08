//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_segmento_aplicado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSegmentoAplicado {
  /// Returns a new [SalidaSegmentoAplicado] instance.
  SalidaSegmentoAplicado({

     this.codigo,

    required  this.motivo,

    required  this.evaluable,
  });

      /// null cuando no califica o cuando falta el dato para evaluar
  @JsonKey(
    
    name: r'codigo',
    required: false,
    includeIfNull: false,
  )


  final String? codigo;



      /// por que califica o que dato falto; en texto para el usuario
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



      /// false cuando falto un hecho. No se adivina en ninguna direccion
  @JsonKey(
    
    name: r'evaluable',
    required: true,
    includeIfNull: false,
  )


  final bool evaluable;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSegmentoAplicado &&
      other.codigo == codigo &&
      other.motivo == motivo &&
      other.evaluable == evaluable;

    @override
    int get hashCode =>
        (codigo == null ? 0 : codigo.hashCode) +
        motivo.hashCode +
        evaluable.hashCode;

  factory SalidaSegmentoAplicado.fromJson(Map<String, dynamic> json) => _$SalidaSegmentoAplicadoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSegmentoAplicadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

