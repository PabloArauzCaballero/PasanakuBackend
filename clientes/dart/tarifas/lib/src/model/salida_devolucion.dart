//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_devolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDevolucion {
  /// Returns a new [SalidaDevolucion] instance.
  SalidaDevolucion({

    required  this.devolucionId,

     this.notaCreditoId,

     this.cuf,

    required  this.devolucionTotal,

    required  this.disponibleRestante,
  });

  @JsonKey(
    
    name: r'devolucionId',
    required: true,
    includeIfNull: false,
  )


  final String devolucionId;



  @JsonKey(
    
    name: r'notaCreditoId',
    required: false,
    includeIfNull: false,
  )


  final String? notaCreditoId;



  @JsonKey(
    
    name: r'cuf',
    required: false,
    includeIfNull: false,
  )


  final String? cuf;



  @JsonKey(
    
    name: r'devolucionTotal',
    required: true,
    includeIfNull: false,
  )


  final bool devolucionTotal;



  @JsonKey(
    
    name: r'disponibleRestante',
    required: true,
    includeIfNull: false,
  )


  final Dinero disponibleRestante;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDevolucion &&
      other.devolucionId == devolucionId &&
      other.notaCreditoId == notaCreditoId &&
      other.cuf == cuf &&
      other.devolucionTotal == devolucionTotal &&
      other.disponibleRestante == disponibleRestante;

    @override
    int get hashCode =>
        devolucionId.hashCode +
        (notaCreditoId == null ? 0 : notaCreditoId.hashCode) +
        (cuf == null ? 0 : cuf.hashCode) +
        devolucionTotal.hashCode +
        disponibleRestante.hashCode;

  factory SalidaDevolucion.fromJson(Map<String, dynamic> json) => _$SalidaDevolucionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDevolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

