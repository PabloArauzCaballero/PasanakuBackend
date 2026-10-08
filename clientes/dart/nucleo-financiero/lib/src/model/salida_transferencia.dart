//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_transferencia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTransferencia {
  /// Returns a new [SalidaTransferencia] instance.
  SalidaTransferencia({

    required  this.transaccionId,

    required  this.saldoDespues,

     this.destinatarioId,

    required  this.obligacionSaldada,
  });

  @JsonKey(
    
    name: r'transaccionId',
    required: true,
    includeIfNull: false,
  )


  final String transaccionId;



  @JsonKey(
    
    name: r'saldoDespues',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoDespues;



  @JsonKey(
    
    name: r'destinatarioId',
    required: false,
    includeIfNull: false,
  )


  final String? destinatarioId;



  @JsonKey(
    
    name: r'obligacionSaldada',
    required: true,
    includeIfNull: false,
  )


  final bool obligacionSaldada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTransferencia &&
      other.transaccionId == transaccionId &&
      other.saldoDespues == saldoDespues &&
      other.destinatarioId == destinatarioId &&
      other.obligacionSaldada == obligacionSaldada;

    @override
    int get hashCode =>
        transaccionId.hashCode +
        saldoDespues.hashCode +
        destinatarioId.hashCode +
        obligacionSaldada.hashCode;

  factory SalidaTransferencia.fromJson(Map<String, dynamic> json) => _$SalidaTransferenciaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTransferenciaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

