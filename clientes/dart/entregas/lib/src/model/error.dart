//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'error.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Error {
  /// Returns a new [Error] instance.
  Error({

    required  this.codigo,

    required  this.mensaje,

     this.detalle,

    required  this.trazaId,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'mensaje',
    required: true,
    includeIfNull: false,
  )


  final String mensaje;



  @JsonKey(
    
    name: r'detalle',
    required: false,
    includeIfNull: false,
  )


  final Object? detalle;



  @JsonKey(
    
    name: r'trazaId',
    required: true,
    includeIfNull: false,
  )


  final String trazaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Error &&
      other.codigo == codigo &&
      other.mensaje == mensaje &&
      other.detalle == detalle &&
      other.trazaId == trazaId;

    @override
    int get hashCode =>
        codigo.hashCode +
        mensaje.hashCode +
        detalle.hashCode +
        trazaId.hashCode;

  factory Error.fromJson(Map<String, dynamic> json) => _$ErrorFromJson(json);

  Map<String, dynamic> toJson() => _$ErrorToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

