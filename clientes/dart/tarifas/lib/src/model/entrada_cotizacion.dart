//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cotizacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCotizacion {
  /// Returns a new [EntradaCotizacion] instance.
  EntradaCotizacion({

    required  this.codigoTarifario,

    required  this.hechoGenerador,

    required  this.referenciaTipo,

    required  this.referenciaId,

    required  this.montoBase,

     this.grupoId,

     this.descuento,

     this.segmentoCodigo,
  });

  @JsonKey(
    
    name: r'codigoTarifario',
    required: true,
    includeIfNull: false,
  )


  final String codigoTarifario;



  @JsonKey(
    
    name: r'hechoGenerador',
    required: true,
    includeIfNull: false,
  )


  final String hechoGenerador;



  @JsonKey(
    
    name: r'referenciaTipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaCotizacionReferenciaTipoEnum referenciaTipo;



  @JsonKey(
    
    name: r'referenciaId',
    required: true,
    includeIfNull: false,
  )


  final String referenciaId;



  @JsonKey(
    
    name: r'montoBase',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoBase;



      /// si viene y el grupo tiene tarifa congelada, manda el snapshot (R-TAR-07)
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



      /// el segmento ya resuelto por CU-36. Queda en el desglose: cotizacion_comision no tiene columna para el y no se invento una
  @JsonKey(
    
    name: r'segmentoCodigo',
    required: false,
    includeIfNull: false,
  )


  final String? segmentoCodigo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCotizacion &&
      other.codigoTarifario == codigoTarifario &&
      other.hechoGenerador == hechoGenerador &&
      other.referenciaTipo == referenciaTipo &&
      other.referenciaId == referenciaId &&
      other.montoBase == montoBase &&
      other.grupoId == grupoId &&
      other.descuento == descuento &&
      other.segmentoCodigo == segmentoCodigo;

    @override
    int get hashCode =>
        codigoTarifario.hashCode +
        hechoGenerador.hashCode +
        referenciaTipo.hashCode +
        referenciaId.hashCode +
        montoBase.hashCode +
        (grupoId == null ? 0 : grupoId.hashCode) +
        descuento.hashCode +
        (segmentoCodigo == null ? 0 : segmentoCodigo.hashCode);

  factory EntradaCotizacion.fromJson(Map<String, dynamic> json) => _$EntradaCotizacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCotizacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCotizacionReferenciaTipoEnum {
@JsonValue(r'ENTREGA_FONDO')
ENTREGA_FONDO(r'ENTREGA_FONDO'),
@JsonValue(r'ORDEN_RECARGA')
ORDEN_RECARGA(r'ORDEN_RECARGA'),
@JsonValue(r'ORDEN_RETIRO')
ORDEN_RETIRO(r'ORDEN_RETIRO'),
@JsonValue(r'PAGO')
PAGO(r'PAGO'),
@JsonValue(r'PERIODO')
PERIODO(r'PERIODO'),
@JsonValue(r'TRANSACCION_BILLETERA')
TRANSACCION_BILLETERA(r'TRANSACCION_BILLETERA');

const EntradaCotizacionReferenciaTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


