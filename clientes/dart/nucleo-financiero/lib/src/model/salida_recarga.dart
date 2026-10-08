//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_recarga.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRecarga {
  /// Returns a new [SalidaRecarga] instance.
  SalidaRecarga({

    required  this.ordenRecargaId,

    required  this.estado,

    required  this.expiraEn,

    required  this.acreditara,
  });

  @JsonKey(
    
    name: r'ordenRecargaId',
    required: true,
    includeIfNull: false,
  )


  final String ordenRecargaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



  @JsonKey(
    
    name: r'expiraEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiraEn;



  @JsonKey(
    
    name: r'acreditara',
    required: true,
    includeIfNull: false,
  )


  final Dinero acreditara;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRecarga &&
      other.ordenRecargaId == ordenRecargaId &&
      other.estado == estado &&
      other.expiraEn == expiraEn &&
      other.acreditara == acreditara;

    @override
    int get hashCode =>
        ordenRecargaId.hashCode +
        estado.hashCode +
        expiraEn.hashCode +
        acreditara.hashCode;

  factory SalidaRecarga.fromJson(Map<String, dynamic> json) => _$SalidaRecargaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRecargaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

