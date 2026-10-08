//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'devolucion_de_participante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DevolucionDeParticipante {
  /// Returns a new [DevolucionDeParticipante] instance.
  DevolucionDeParticipante({

    required  this.participanteId,

    required  this.aportado,

    required  this.aDevolver,
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
    
    name: r'aDevolver',
    required: true,
    includeIfNull: false,
  )


  final Dinero aDevolver;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DevolucionDeParticipante &&
      other.participanteId == participanteId &&
      other.aportado == aportado &&
      other.aDevolver == aDevolver;

    @override
    int get hashCode =>
        participanteId.hashCode +
        aportado.hashCode +
        aDevolver.hashCode;

  factory DevolucionDeParticipante.fromJson(Map<String, dynamic> json) => _$DevolucionDeParticipanteFromJson(json);

  Map<String, dynamic> toJson() => _$DevolucionDeParticipanteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

