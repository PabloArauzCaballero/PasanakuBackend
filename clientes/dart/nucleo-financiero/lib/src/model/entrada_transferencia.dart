//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:aportaya_cliente_nucleofinanciero/src/model/entrada_transferencia_destino.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_transferencia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTransferencia {
  /// Returns a new [EntradaTransferencia] instance.
  EntradaTransferencia({

    required  this.cuentaOrigenId,

    required  this.destino,

    required  this.monto,

    required  this.concepto,

     this.obligacionId,
  });

  @JsonKey(
    
    name: r'cuentaOrigenId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaOrigenId;



  @JsonKey(
    
    name: r'destino',
    required: true,
    includeIfNull: false,
  )


  final EntradaTransferenciaDestino destino;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



  @JsonKey(
    
    name: r'concepto',
    required: true,
    includeIfNull: false,
  )


  final String concepto;



  @JsonKey(
    
    name: r'obligacionId',
    required: false,
    includeIfNull: false,
  )


  final String? obligacionId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTransferencia &&
      other.cuentaOrigenId == cuentaOrigenId &&
      other.destino == destino &&
      other.monto == monto &&
      other.concepto == concepto &&
      other.obligacionId == obligacionId;

    @override
    int get hashCode =>
        cuentaOrigenId.hashCode +
        destino.hashCode +
        monto.hashCode +
        concepto.hashCode +
        obligacionId.hashCode;

  factory EntradaTransferencia.fromJson(Map<String, dynamic> json) => _$EntradaTransferenciaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTransferenciaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

