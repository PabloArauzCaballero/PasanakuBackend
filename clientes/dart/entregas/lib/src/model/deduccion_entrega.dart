//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'deduccion_entrega.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DeduccionEntrega {
  /// Returns a new [DeduccionEntrega] instance.
  DeduccionEntrega({

    required  this.tipo,

    required  this.descripcion,

    required  this.monto,

     this.referenciaOrigenId,

    required  this.esObligatoria,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final DeduccionEntregaTipoEnum tipo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



      /// la fila que justifica el descuento. Sin ella la deduccion no se puede auditar
  @JsonKey(
    
    name: r'referenciaOrigenId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaOrigenId;



  @JsonKey(
    
    name: r'esObligatoria',
    required: true,
    includeIfNull: false,
  )


  final bool esObligatoria;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DeduccionEntrega &&
      other.tipo == tipo &&
      other.descripcion == descripcion &&
      other.monto == monto &&
      other.referenciaOrigenId == referenciaOrigenId &&
      other.esObligatoria == esObligatoria;

    @override
    int get hashCode =>
        tipo.hashCode +
        descripcion.hashCode +
        monto.hashCode +
        (referenciaOrigenId == null ? 0 : referenciaOrigenId.hashCode) +
        esObligatoria.hashCode;

  factory DeduccionEntrega.fromJson(Map<String, dynamic> json) => _$DeduccionEntregaFromJson(json);

  Map<String, dynamic> toJson() => _$DeduccionEntregaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DeduccionEntregaTipoEnum {
@JsonValue(r'AJUSTE_ACORDADO')
AJUSTE_ACORDADO(r'AJUSTE_ACORDADO'),
@JsonValue(r'APORTE_PROPIO_DEL_PERIODO')
APORTE_PROPIO_DEL_PERIODO(r'APORTE_PROPIO_DEL_PERIODO'),
@JsonValue(r'COMISION_PLATAFORMA')
COMISION_PLATAFORMA(r'COMISION_PLATAFORMA'),
@JsonValue(r'COSTO_TRANSFERENCIA')
COSTO_TRANSFERENCIA(r'COSTO_TRANSFERENCIA'),
@JsonValue(r'DEUDA_VENCIDA_PROPIA')
DEUDA_VENCIDA_PROPIA(r'DEUDA_VENCIDA_PROPIA'),
@JsonValue(r'RECARGO_MORA_PROPIO')
RECARGO_MORA_PROPIO(r'RECARGO_MORA_PROPIO'),
@JsonValue(r'REPOSICION_FONDO_GARANTIA')
REPOSICION_FONDO_GARANTIA(r'REPOSICION_FONDO_GARANTIA'),
@JsonValue(r'RETENCION_IMPUESTO')
RETENCION_IMPUESTO(r'RETENCION_IMPUESTO');

const DeduccionEntregaTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


