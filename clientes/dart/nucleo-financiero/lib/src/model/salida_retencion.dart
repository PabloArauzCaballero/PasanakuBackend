//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_retencion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRetencion {
  /// Returns a new [SalidaRetencion] instance.
  SalidaRetencion({

    required  this.retencionId,

    required  this.saldoDisponible,

    required  this.saldoRetenido,

     this.expiraEn,
  });

  @JsonKey(
    
    name: r'retencionId',
    required: true,
    includeIfNull: false,
  )


  final String retencionId;



  @JsonKey(
    
    name: r'saldoDisponible',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoDisponible;



  @JsonKey(
    
    name: r'saldoRetenido',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoRetenido;



  @JsonKey(
    
    name: r'expiraEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? expiraEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRetencion &&
      other.retencionId == retencionId &&
      other.saldoDisponible == saldoDisponible &&
      other.saldoRetenido == saldoRetenido &&
      other.expiraEn == expiraEn;

    @override
    int get hashCode =>
        retencionId.hashCode +
        saldoDisponible.hashCode +
        saldoRetenido.hashCode +
        expiraEn.hashCode;

  factory SalidaRetencion.fromJson(Map<String, dynamic> json) => _$SalidaRetencionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRetencionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

