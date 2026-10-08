//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'posicion_de_participante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PosicionDeParticipante {
  /// Returns a new [PosicionDeParticipante] instance.
  PosicionDeParticipante({

    required  this.participanteId,

    required  this.aportado,

    required  this.recibido,
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



      /// lo que ya cobro de su turno. Sin esto, disolver premia al que llego primero
  @JsonKey(
    
    name: r'recibido',
    required: true,
    includeIfNull: false,
  )


  final Dinero recibido;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PosicionDeParticipante &&
      other.participanteId == participanteId &&
      other.aportado == aportado &&
      other.recibido == recibido;

    @override
    int get hashCode =>
        participanteId.hashCode +
        aportado.hashCode +
        recibido.hashCode;

  factory PosicionDeParticipante.fromJson(Map<String, dynamic> json) => _$PosicionDeParticipanteFromJson(json);

  Map<String, dynamic> toJson() => _$PosicionDeParticipanteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

