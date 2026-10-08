//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_devolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDevolucion {
  /// Returns a new [EntradaDevolucion] instance.
  EntradaDevolucion({

    required  this.motivo,

    required  this.detalle,

    required  this.monto,

    required  this.forma,

     this.reclamoId,

    required  this.autorizadaPor,
  });

  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final EntradaDevolucionMotivoEnum motivo;



  @JsonKey(
    
    name: r'detalle',
    required: true,
    includeIfNull: false,
  )


  final String detalle;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



  @JsonKey(
    
    name: r'forma',
    required: true,
    includeIfNull: false,
  )


  final EntradaDevolucionFormaEnum forma;



      /// obligatorio cuando nace de un reclamo favorable con monto (R-CON-04)
  @JsonKey(
    
    name: r'reclamoId',
    required: false,
    includeIfNull: false,
  )


  final String? reclamoId;



      /// quien autoriza no es quien ejecuta (R-SEG-04)
  @JsonKey(
    
    name: r'autorizadaPor',
    required: true,
    includeIfNull: false,
  )


  final String autorizadaPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDevolucion &&
      other.motivo == motivo &&
      other.detalle == detalle &&
      other.monto == monto &&
      other.forma == forma &&
      other.reclamoId == reclamoId &&
      other.autorizadaPor == autorizadaPor;

    @override
    int get hashCode =>
        motivo.hashCode +
        detalle.hashCode +
        monto.hashCode +
        forma.hashCode +
        (reclamoId == null ? 0 : reclamoId.hashCode) +
        autorizadaPor.hashCode;

  factory EntradaDevolucion.fromJson(Map<String, dynamic> json) => _$EntradaDevolucionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDevolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDevolucionMotivoEnum {
@JsonValue(r'ENTREGA_ANULADA')
ENTREGA_ANULADA(r'ENTREGA_ANULADA'),
@JsonValue(r'ERROR_DE_TARIFA')
ERROR_DE_TARIFA(r'ERROR_DE_TARIFA'),
@JsonValue(r'RECLAMO_PROCEDENTE')
RECLAMO_PROCEDENTE(r'RECLAMO_PROCEDENTE'),
@JsonValue(r'FALLA_DE_SERVICIO')
FALLA_DE_SERVICIO(r'FALLA_DE_SERVICIO');

const EntradaDevolucionMotivoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaDevolucionFormaEnum {
@JsonValue(r'ABONO_BILLETERA')
ABONO_BILLETERA(r'ABONO_BILLETERA'),
@JsonValue(r'NOTA_CREDITO')
NOTA_CREDITO(r'NOTA_CREDITO'),
@JsonValue(r'COMPENSACION')
COMPENSACION(r'COMPENSACION');

const EntradaDevolucionFormaEnum(this.value);

final String value;

@override
String toString() => value;
}


