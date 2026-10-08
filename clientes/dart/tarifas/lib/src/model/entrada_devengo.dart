//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_devengo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDevengo {
  /// Returns a new [EntradaDevengo] instance.
  EntradaDevengo({

    required  this.cotizacionId,

    required  this.tarifarioId,

    required  this.referenciaTipo,

    required  this.referenciaId,

    required  this.usuarioObligadoId,

     this.grupoId,

     this.descuento,

     this.exentoTotal = false,
  });

  @JsonKey(
    
    name: r'cotizacionId',
    required: true,
    includeIfNull: false,
  )


  final String cotizacionId;



  @JsonKey(
    
    name: r'tarifarioId',
    required: true,
    includeIfNull: false,
  )


  final String tarifarioId;



  @JsonKey(
    
    name: r'referenciaTipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaDevengoReferenciaTipoEnum referenciaTipo;



  @JsonKey(
    
    name: r'referenciaId',
    required: true,
    includeIfNull: false,
  )


  final String referenciaId;



  @JsonKey(
    
    name: r'usuarioObligadoId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioObligadoId;



  @JsonKey(
    
    name: r'grupoId',
    required: false,
    includeIfNull: false,
  )


  final String? grupoId;



  @JsonKey(
    
    name: r'descuento',
    required: false,
    includeIfNull: false,
  )


  final Dinero? descuento;



      /// exencion del 100%; se devenga en cero y queda quien lo autorizo
  @JsonKey(
    defaultValue: false,
    name: r'exentoTotal',
    required: false,
    includeIfNull: false,
  )


  final bool? exentoTotal;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDevengo &&
      other.cotizacionId == cotizacionId &&
      other.tarifarioId == tarifarioId &&
      other.referenciaTipo == referenciaTipo &&
      other.referenciaId == referenciaId &&
      other.usuarioObligadoId == usuarioObligadoId &&
      other.grupoId == grupoId &&
      other.descuento == descuento &&
      other.exentoTotal == exentoTotal;

    @override
    int get hashCode =>
        cotizacionId.hashCode +
        tarifarioId.hashCode +
        referenciaTipo.hashCode +
        referenciaId.hashCode +
        usuarioObligadoId.hashCode +
        (grupoId == null ? 0 : grupoId.hashCode) +
        descuento.hashCode +
        exentoTotal.hashCode;

  factory EntradaDevengo.fromJson(Map<String, dynamic> json) => _$EntradaDevengoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDevengoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDevengoReferenciaTipoEnum {
@JsonValue(r'CICLO')
CICLO(r'CICLO'),
@JsonValue(r'ENTREGA_FONDO')
ENTREGA_FONDO(r'ENTREGA_FONDO'),
@JsonValue(r'ORDEN_RETIRO')
ORDEN_RETIRO(r'ORDEN_RETIRO'),
@JsonValue(r'PAGO')
PAGO(r'PAGO');

const EntradaDevengoReferenciaTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


