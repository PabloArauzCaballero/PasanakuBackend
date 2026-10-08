//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'saldo_billetera.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SaldoBilletera {
  /// Returns a new [SaldoBilletera] instance.
  SaldoBilletera({

    required  this.cuentaId,

    required  this.disponible,

    required  this.retenido,

    required  this.alCorteDe,
  });

  @JsonKey(
    
    name: r'cuentaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaId;



  @JsonKey(
    
    name: r'disponible',
    required: true,
    includeIfNull: false,
  )


  final Dinero disponible;



  @JsonKey(
    
    name: r'retenido',
    required: true,
    includeIfNull: false,
  )


  final Dinero retenido;



  @JsonKey(
    
    name: r'alCorteDe',
    required: true,
    includeIfNull: false,
  )


  final DateTime alCorteDe;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SaldoBilletera &&
      other.cuentaId == cuentaId &&
      other.disponible == disponible &&
      other.retenido == retenido &&
      other.alCorteDe == alCorteDe;

    @override
    int get hashCode =>
        cuentaId.hashCode +
        disponible.hashCode +
        retenido.hashCode +
        alCorteDe.hashCode;

  factory SaldoBilletera.fromJson(Map<String, dynamic> json) => _$SaldoBilleteraFromJson(json);

  Map<String, dynamic> toJson() => _$SaldoBilleteraToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

