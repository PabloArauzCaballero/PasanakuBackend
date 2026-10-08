//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/linea_desglose.dart';
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_cotizacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCotizacion {
  /// Returns a new [SalidaCotizacion] instance.
  SalidaCotizacion({

    required  this.cotizacionId,

    required  this.montoComision,

    required  this.montoImpuesto,

    required  this.montoTotal,

    required  this.desglose,

    required  this.validaHasta,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'cotizacionId',
    required: true,
    includeIfNull: false,
  )


  final String cotizacionId;



  @JsonKey(
    
    name: r'montoComision',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoComision;



  @JsonKey(
    
    name: r'montoImpuesto',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoImpuesto;



  @JsonKey(
    
    name: r'montoTotal',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoTotal;



  @JsonKey(
    
    name: r'desglose',
    required: true,
    includeIfNull: false,
  )


  final List<LineaDesglose> desglose;



  @JsonKey(
    
    name: r'validaHasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime validaHasta;



      /// false cuando la respuesta viene de un reintento con la misma clave
  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCotizacion &&
      other.cotizacionId == cotizacionId &&
      other.montoComision == montoComision &&
      other.montoImpuesto == montoImpuesto &&
      other.montoTotal == montoTotal &&
      other.desglose == desglose &&
      other.validaHasta == validaHasta &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        cotizacionId.hashCode +
        montoComision.hashCode +
        montoImpuesto.hashCode +
        montoTotal.hashCode +
        desglose.hashCode +
        validaHasta.hashCode +
        esNueva.hashCode;

  factory SalidaCotizacion.fromJson(Map<String, dynamic> json) => _$SalidaCotizacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCotizacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

