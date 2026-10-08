//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_depreciacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDepreciacion {
  /// Returns a new [SalidaDepreciacion] instance.
  SalidaDepreciacion({

    required  this.activoFijoId,

    required  this.depreciacionId,

    required  this.monto,

    required  this.valorEnLibros,

    required  this.totalmenteDepreciado,
  });

  @JsonKey(
    
    name: r'activoFijoId',
    required: true,
    includeIfNull: false,
  )


  final String activoFijoId;



  @JsonKey(
    
    name: r'depreciacionId',
    required: true,
    includeIfNull: false,
  )


  final String depreciacionId;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'valorEnLibros',
    required: true,
    includeIfNull: false,
  )


  final String valorEnLibros;



  @JsonKey(
    
    name: r'totalmenteDepreciado',
    required: true,
    includeIfNull: false,
  )


  final bool totalmenteDepreciado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDepreciacion &&
      other.activoFijoId == activoFijoId &&
      other.depreciacionId == depreciacionId &&
      other.monto == monto &&
      other.valorEnLibros == valorEnLibros &&
      other.totalmenteDepreciado == totalmenteDepreciado;

    @override
    int get hashCode =>
        activoFijoId.hashCode +
        depreciacionId.hashCode +
        monto.hashCode +
        valorEnLibros.hashCode +
        totalmenteDepreciado.hashCode;

  factory SalidaDepreciacion.fromJson(Map<String, dynamic> json) => _$SalidaDepreciacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDepreciacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

