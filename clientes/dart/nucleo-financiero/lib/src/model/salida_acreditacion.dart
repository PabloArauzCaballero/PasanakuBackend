//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_acreditacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAcreditacion {
  /// Returns a new [SalidaAcreditacion] instance.
  SalidaAcreditacion({

    required  this.ordenRecargaId,

    required  this.transaccionId,

    required  this.saldoDespues,
  });

  @JsonKey(
    
    name: r'ordenRecargaId',
    required: true,
    includeIfNull: false,
  )


  final String ordenRecargaId;



  @JsonKey(
    
    name: r'transaccionId',
    required: true,
    includeIfNull: false,
  )


  final String transaccionId;



  @JsonKey(
    
    name: r'saldoDespues',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoDespues;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAcreditacion &&
      other.ordenRecargaId == ordenRecargaId &&
      other.transaccionId == transaccionId &&
      other.saldoDespues == saldoDespues;

    @override
    int get hashCode =>
        ordenRecargaId.hashCode +
        transaccionId.hashCode +
        saldoDespues.hashCode;

  factory SalidaAcreditacion.fromJson(Map<String, dynamic> json) => _$SalidaAcreditacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAcreditacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

