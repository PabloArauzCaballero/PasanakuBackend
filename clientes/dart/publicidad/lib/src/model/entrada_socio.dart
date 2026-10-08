//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_socio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaSocio {
  /// Returns a new [EntradaSocio] instance.
  EntradaSocio({

    required  this.razonSocial,

    required  this.numeroDocumento,

     this.rubro,

    required  this.emailContacto,
  });

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
    
    name: r'rubro',
    required: false,
    includeIfNull: false,
  )


  final String? rubro;



  @JsonKey(
    
    name: r'emailContacto',
    required: true,
    includeIfNull: false,
  )


  final String emailContacto;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaSocio &&
      other.razonSocial == razonSocial &&
      other.numeroDocumento == numeroDocumento &&
      other.rubro == rubro &&
      other.emailContacto == emailContacto;

    @override
    int get hashCode =>
        razonSocial.hashCode +
        numeroDocumento.hashCode +
        rubro.hashCode +
        emailContacto.hashCode;

  factory EntradaSocio.fromJson(Map<String, dynamic> json) => _$EntradaSocioFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaSocioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

