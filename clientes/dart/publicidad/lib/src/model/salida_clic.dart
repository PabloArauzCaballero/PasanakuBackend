//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_clic.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaClic {
  /// Returns a new [SalidaClic] instance.
  SalidaClic({

    required  this.clicId,

    required  this.costo,
  });

  @JsonKey(
    
    name: r'clicId',
    required: true,
    includeIfNull: false,
  )


  final String clicId;



      /// El costo de una impresion o un clic, con cuatro decimales. Una impresion suelta cuesta fracciones de centavo; el redondeo se hace una sola vez, al facturar.
  @JsonKey(
    
    name: r'costo',
    required: true,
    includeIfNull: false,
  )


  final String costo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaClic &&
      other.clicId == clicId &&
      other.costo == costo;

    @override
    int get hashCode =>
        clicId.hashCode +
        costo.hashCode;

  factory SalidaClic.fromJson(Map<String, dynamic> json) => _$SalidaClicFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaClicToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

