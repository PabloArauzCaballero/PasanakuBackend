//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_bloqueo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaBloqueo {
  /// Returns a new [SalidaBloqueo] instance.
  SalidaBloqueo({

    required  this.bloqueoId,

    required  this.retencionId,

    required  this.montoBloqueado,

    required  this.saldoDisponible,
  });

  @JsonKey(
    
    name: r'bloqueoId',
    required: true,
    includeIfNull: false,
  )


  final String bloqueoId;



  @JsonKey(
    
    name: r'retencionId',
    required: true,
    includeIfNull: false,
  )


  final String retencionId;



  @JsonKey(
    
    name: r'montoBloqueado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoBloqueado;



  @JsonKey(
    
    name: r'saldoDisponible',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoDisponible;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaBloqueo &&
      other.bloqueoId == bloqueoId &&
      other.retencionId == retencionId &&
      other.montoBloqueado == montoBloqueado &&
      other.saldoDisponible == saldoDisponible;

    @override
    int get hashCode =>
        bloqueoId.hashCode +
        retencionId.hashCode +
        montoBloqueado.hashCode +
        saldoDisponible.hashCode;

  factory SalidaBloqueo.fromJson(Map<String, dynamic> json) => _$SalidaBloqueoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaBloqueoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

