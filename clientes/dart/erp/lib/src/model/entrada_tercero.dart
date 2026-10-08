//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_tercero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTercero {
  /// Returns a new [EntradaTercero] instance.
  EntradaTercero({

    required  this.tipo,

    required  this.razonSocial,

    required  this.numeroDocumento,

     this.email,

     this.cuentaContableId,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaTerceroTipoEnum tipo;



  @JsonKey(
    
    name: r'razonSocial',
    required: true,
    includeIfNull: false,
  )


  final String razonSocial;



  @JsonKey(
    
    name: r'numeroDocumento',
    required: true,
    includeIfNull: false,
  )


  final String numeroDocumento;



  @JsonKey(
    
    name: r'email',
    required: false,
    includeIfNull: false,
  )


  final String? email;



  @JsonKey(
    
    name: r'cuentaContableId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaContableId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTercero &&
      other.tipo == tipo &&
      other.razonSocial == razonSocial &&
      other.numeroDocumento == numeroDocumento &&
      other.email == email &&
      other.cuentaContableId == cuentaContableId;

    @override
    int get hashCode =>
        tipo.hashCode +
        razonSocial.hashCode +
        numeroDocumento.hashCode +
        email.hashCode +
        cuentaContableId.hashCode;

  factory EntradaTercero.fromJson(Map<String, dynamic> json) => _$EntradaTerceroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTerceroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaTerceroTipoEnum {
@JsonValue(r'PROVEEDOR')
PROVEEDOR(r'PROVEEDOR'),
@JsonValue(r'CLIENTE')
CLIENTE(r'CLIENTE'),
@JsonValue(r'AMBOS')
AMBOS(r'AMBOS');

const EntradaTerceroTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


