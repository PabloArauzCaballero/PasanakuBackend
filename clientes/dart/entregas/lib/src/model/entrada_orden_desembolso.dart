//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_orden_desembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaOrdenDesembolso {
  /// Returns a new [EntradaOrdenDesembolso] instance.
  EntradaOrdenDesembolso({

    required  this.entregaId,

    required  this.proveedorId,

    required  this.cuentaDestinoId,

    required  this.glosa,

    required  this.saldoRetenido,
  });

  @JsonKey(
    
    name: r'entregaId',
    required: true,
    includeIfNull: false,
  )


  final String entregaId;



  @JsonKey(
    
    name: r'proveedorId',
    required: true,
    includeIfNull: false,
  )


  final String proveedorId;



  @JsonKey(
    
    name: r'cuentaDestinoId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaDestinoId;



  @JsonKey(
    
    name: r'glosa',
    required: true,
    includeIfNull: false,
  )


  final String glosa;



      /// sin el saldo retenido, desembolsar seria pagar con plata que todavia puede gastarse en otra cosa
  @JsonKey(
    
    name: r'saldoRetenido',
    required: true,
    includeIfNull: false,
  )


  final bool saldoRetenido;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaOrdenDesembolso &&
      other.entregaId == entregaId &&
      other.proveedorId == proveedorId &&
      other.cuentaDestinoId == cuentaDestinoId &&
      other.glosa == glosa &&
      other.saldoRetenido == saldoRetenido;

    @override
    int get hashCode =>
        entregaId.hashCode +
        proveedorId.hashCode +
        cuentaDestinoId.hashCode +
        glosa.hashCode +
        saldoRetenido.hashCode;

  factory EntradaOrdenDesembolso.fromJson(Map<String, dynamic> json) => _$EntradaOrdenDesembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaOrdenDesembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

