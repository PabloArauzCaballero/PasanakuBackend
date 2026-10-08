//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_factura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaFactura {
  /// Returns a new [SalidaFactura] instance.
  SalidaFactura({

    required  this.facturaId,

    required  this.cuf,

    required  this.numeroFactura,

    required  this.estadoFiscal,

     this.eventoSignificativoId,
  });

  @JsonKey(
    
    name: r'facturaId',
    required: true,
    includeIfNull: false,
  )


  final String facturaId;



  @JsonKey(
    
    name: r'cuf',
    required: true,
    includeIfNull: false,
  )


  final String cuf;



  @JsonKey(
    
    name: r'numeroFactura',
    required: true,
    includeIfNull: false,
  )


  final int numeroFactura;



  @JsonKey(
    
    name: r'estadoFiscal',
    required: true,
    includeIfNull: false,
  )


  final SalidaFacturaEstadoFiscalEnum estadoFiscal;



      /// presente si se emitio bajo contingencia; lleva su plazo guardado
  @JsonKey(
    
    name: r'eventoSignificativoId',
    required: false,
    includeIfNull: false,
  )


  final String? eventoSignificativoId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaFactura &&
      other.facturaId == facturaId &&
      other.cuf == cuf &&
      other.numeroFactura == numeroFactura &&
      other.estadoFiscal == estadoFiscal &&
      other.eventoSignificativoId == eventoSignificativoId;

    @override
    int get hashCode =>
        facturaId.hashCode +
        cuf.hashCode +
        numeroFactura.hashCode +
        estadoFiscal.hashCode +
        (eventoSignificativoId == null ? 0 : eventoSignificativoId.hashCode);

  factory SalidaFactura.fromJson(Map<String, dynamic> json) => _$SalidaFacturaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaFacturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaFacturaEstadoFiscalEnum {
@JsonValue(r'VALIDADA')
VALIDADA(r'VALIDADA'),
@JsonValue(r'EMITIDA_OFFLINE')
EMITIDA_OFFLINE(r'EMITIDA_OFFLINE'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaFacturaEstadoFiscalEnum(this.value);

final String value;

@override
String toString() => value;
}


