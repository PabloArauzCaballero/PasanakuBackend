//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_retiro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRetiro {
  /// Returns a new [EntradaRetiro] instance.
  EntradaRetiro({

    required  this.cuentaBilleteraId,

    required  this.monto,

    required  this.instrumentoDestinoId,

    required  this.factorMfa,
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
    
    name: r'instrumentoDestinoId',
    required: true,
    includeIfNull: false,
  )


  final String instrumentoDestinoId;



  @JsonKey(
    
    name: r'factorMfa',
    required: true,
    includeIfNull: false,
  )


  final String factorMfa;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRetiro &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.monto == monto &&
      other.instrumentoDestinoId == instrumentoDestinoId &&
      other.factorMfa == factorMfa;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        monto.hashCode +
        instrumentoDestinoId.hashCode +
        factorMfa.hashCode;

  factory EntradaRetiro.fromJson(Map<String, dynamic> json) => _$EntradaRetiroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRetiroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

