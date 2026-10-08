//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_proveedor.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaProveedor {
  /// Returns a new [SalidaProveedor] instance.
  SalidaProveedor({

    required  this.proveedorId,

    required  this.codigo,

    required  this.activo,
  });

  @JsonKey(
    
    name: r'proveedorId',
    required: true,
    includeIfNull: false,
  )


  final String proveedorId;



  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'activo',
    required: true,
    includeIfNull: false,
  )


  final bool activo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaProveedor &&
      other.proveedorId == proveedorId &&
      other.codigo == codigo &&
      other.activo == activo;

    @override
    int get hashCode =>
        proveedorId.hashCode +
        codigo.hashCode +
        activo.hashCode;

  factory SalidaProveedor.fromJson(Map<String, dynamic> json) => _$SalidaProveedorFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaProveedorToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

