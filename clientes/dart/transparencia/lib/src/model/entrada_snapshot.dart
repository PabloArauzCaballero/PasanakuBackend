//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_snapshot.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaSnapshot {
  /// Returns a new [EntradaSnapshot] instance.
  EntradaSnapshot({

    required  this.usuarioId,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final EntradaSnapshotMotivoEnum motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaSnapshot &&
      other.usuarioId == usuarioId &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        motivo.hashCode;

  factory EntradaSnapshot.fromJson(Map<String, dynamic> json) => _$EntradaSnapshotFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaSnapshotToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaSnapshotMotivoEnum {
@JsonValue(r'AUDITORIA')
AUDITORIA(r'AUDITORIA'),
@JsonValue(r'CIERRE_DE_GRUPO')
CIERRE_DE_GRUPO(r'CIERRE_DE_GRUPO'),
@JsonValue(r'INGRESO_A_GRUPO')
INGRESO_A_GRUPO(r'INGRESO_A_GRUPO'),
@JsonValue(r'PERIODICO')
PERIODICO(r'PERIODICO');

const EntradaSnapshotMotivoEnum(this.value);

final String value;

@override
String toString() => value;
}


