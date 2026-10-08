//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_entrega.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEntrega {
  /// Returns a new [SalidaEntrega] instance.
  SalidaEntrega({

    required  this.anuncioId,

    required  this.impresionId,

    required  this.costo,

     this.motivo,
  });

  @JsonKey(
    
    name: r'anuncioId',
    required: true,
    includeIfNull: true,
  )


  final String? anuncioId;



  @JsonKey(
    
    name: r'impresionId',
    required: true,
    includeIfNull: true,
  )


  final String? impresionId;



      /// El costo de una impresion o un clic, con cuatro decimales. Una impresion suelta cuesta fracciones de centavo; el redondeo se hace una sola vez, al facturar.
  @JsonKey(
    
    name: r'costo',
    required: true,
    includeIfNull: false,
  )


  final String costo;



      /// Por que el espacio no muestra nada. Nulo cuando si entrega.
  @JsonKey(
    
    name: r'motivo',
    required: false,
    includeIfNull: false,
  )


  final SalidaEntregaMotivoEnum? motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEntrega &&
      other.anuncioId == anuncioId &&
      other.impresionId == impresionId &&
      other.costo == costo &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        (anuncioId == null ? 0 : anuncioId.hashCode) +
        (impresionId == null ? 0 : impresionId.hashCode) +
        costo.hashCode +
        (motivo == null ? 0 : motivo.hashCode);

  factory SalidaEntrega.fromJson(Map<String, dynamic> json) => _$SalidaEntregaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEntregaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Por que el espacio no muestra nada. Nulo cuando si entrega.
enum SalidaEntregaMotivoEnum {
    /// Por que el espacio no muestra nada. Nulo cuando si entrega.
@JsonValue(r'SIN_ANUNCIO_ELEGIBLE')
SIN_ANUNCIO_ELEGIBLE(r'SIN_ANUNCIO_ELEGIBLE'),
    /// Por que el espacio no muestra nada. Nulo cuando si entrega.
@JsonValue(r'ESPACIO_INACTIVO')
ESPACIO_INACTIVO(r'ESPACIO_INACTIVO');

const SalidaEntregaMotivoEnum(this.value);

final String value;

@override
String toString() => value;
}


