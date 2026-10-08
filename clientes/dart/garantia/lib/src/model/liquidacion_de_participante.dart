//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'liquidacion_de_participante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LiquidacionDeParticipante {
  /// Returns a new [LiquidacionDeParticipante] instance.
  LiquidacionDeParticipante({

    required  this.participanteId,

    required  this.aportado,

    required  this.recibido,

    required  this.aDevolver,

    required  this.aCobrarle,
  });

  @JsonKey(
    
    name: r'participanteId',
    required: true,
    includeIfNull: false,
  )


  final String participanteId;



  @JsonKey(
    
    name: r'aportado',
    required: true,
    includeIfNull: false,
  )


  final Dinero aportado;



  @JsonKey(
    
    name: r'recibido',
    required: true,
    includeIfNull: false,
  )


  final Dinero recibido;



  @JsonKey(
    
    name: r'aDevolver',
    required: true,
    includeIfNull: false,
  )


  final Dinero aDevolver;



  @JsonKey(
    
    name: r'aCobrarle',
    required: true,
    includeIfNull: false,
  )


  final Dinero aCobrarle;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LiquidacionDeParticipante &&
      other.participanteId == participanteId &&
      other.aportado == aportado &&
      other.recibido == recibido &&
      other.aDevolver == aDevolver &&
      other.aCobrarle == aCobrarle;

    @override
    int get hashCode =>
        participanteId.hashCode +
        aportado.hashCode +
        recibido.hashCode +
        aDevolver.hashCode +
        aCobrarle.hashCode;

  factory LiquidacionDeParticipante.fromJson(Map<String, dynamic> json) => _$LiquidacionDeParticipanteFromJson(json);

  Map<String, dynamic> toJson() => _$LiquidacionDeParticipanteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

