//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_recarga.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRecarga {
  /// Returns a new [EntradaRecarga] instance.
  EntradaRecarga({

    required  this.cuentaBilleteraId,

    required  this.monto,

     this.costoProveedor,

    required  this.medio,

     this.instrumentoFondeoId,
  });

  @JsonKey(
    
    name: r'cuentaBilleteraId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaBilleteraId;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



  @JsonKey(
    
    name: r'costoProveedor',
    required: false,
    includeIfNull: false,
  )


  final Dinero? costoProveedor;



  @JsonKey(
    
    name: r'medio',
    required: true,
    includeIfNull: false,
  )


  final String medio;



  @JsonKey(
    
    name: r'instrumentoFondeoId',
    required: false,
    includeIfNull: false,
  )


  final String? instrumentoFondeoId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRecarga &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.monto == monto &&
      other.costoProveedor == costoProveedor &&
      other.medio == medio &&
      other.instrumentoFondeoId == instrumentoFondeoId;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        monto.hashCode +
        costoProveedor.hashCode +
        medio.hashCode +
        instrumentoFondeoId.hashCode;

  factory EntradaRecarga.fromJson(Map<String, dynamic> json) => _$EntradaRecargaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRecargaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

