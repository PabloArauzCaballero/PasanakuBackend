//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_retencion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRetencion {
  /// Returns a new [EntradaRetencion] instance.
  EntradaRetencion({

    required  this.cuentaBilleteraId,

    required  this.monto,

    required  this.motivo,

     this.transaccionOrigenId,

     this.referenciaTipo,

     this.referenciaId,

     this.expiraEn,
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
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    
    name: r'transaccionOrigenId',
    required: false,
    includeIfNull: false,
  )


  final String? transaccionOrigenId;



  @JsonKey(
    
    name: r'referenciaTipo',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaTipo;



  @JsonKey(
    
    name: r'referenciaId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaId;



  @JsonKey(
    
    name: r'expiraEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? expiraEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRetencion &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.monto == monto &&
      other.motivo == motivo &&
      other.transaccionOrigenId == transaccionOrigenId &&
      other.referenciaTipo == referenciaTipo &&
      other.referenciaId == referenciaId &&
      other.expiraEn == expiraEn;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        monto.hashCode +
        motivo.hashCode +
        transaccionOrigenId.hashCode +
        referenciaTipo.hashCode +
        referenciaId.hashCode +
        expiraEn.hashCode;

  factory EntradaRetencion.fromJson(Map<String, dynamic> json) => _$EntradaRetencionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRetencionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

