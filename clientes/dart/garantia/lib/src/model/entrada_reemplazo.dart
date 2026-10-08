//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_reemplazo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaReemplazo {
  /// Returns a new [EntradaReemplazo] instance.
  EntradaReemplazo({

    required  this.cupoId,

     this.entranteId,

    required  this.deudaQueAsumeElEntrante,

    required  this.conservaOrdenDeTurno,
  });

  @JsonKey(
    
    name: r'cupoId',
    required: true,
    includeIfNull: false,
  )


  final String cupoId;



      /// null mientras se busca candidato
  @JsonKey(
    
    name: r'entranteId',
    required: false,
    includeIfNull: false,
  )


  final String? entranteId;



      /// lo que el entrante NO asume queda como deuda del saliente. Perdonarla se la cobraria a los demas participantes
  @JsonKey(
    
    name: r'deudaQueAsumeElEntrante',
    required: true,
    includeIfNull: false,
  )


  final Dinero deudaQueAsumeElEntrante;



  @JsonKey(
    
    name: r'conservaOrdenDeTurno',
    required: true,
    includeIfNull: false,
  )


  final bool conservaOrdenDeTurno;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaReemplazo &&
      other.cupoId == cupoId &&
      other.entranteId == entranteId &&
      other.deudaQueAsumeElEntrante == deudaQueAsumeElEntrante &&
      other.conservaOrdenDeTurno == conservaOrdenDeTurno;

    @override
    int get hashCode =>
        cupoId.hashCode +
        (entranteId == null ? 0 : entranteId.hashCode) +
        deudaQueAsumeElEntrante.hashCode +
        conservaOrdenDeTurno.hashCode;

  factory EntradaReemplazo.fromJson(Map<String, dynamic> json) => _$EntradaReemplazoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaReemplazoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

