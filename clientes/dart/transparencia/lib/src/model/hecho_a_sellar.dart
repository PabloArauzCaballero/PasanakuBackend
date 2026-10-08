//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'hecho_a_sellar.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class HechoASellar {
  /// Returns a new [HechoASellar] instance.
  HechoASellar({

    required  this.tipoEntidad,

    required  this.entidadId,

    required  this.campos,

    required  this.ocurridoEn,
  });

      /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
  @JsonKey(
    
    name: r'tipoEntidad',
    required: true,
    includeIfNull: false,
  )


  final HechoASellarTipoEntidadEnum tipoEntidad;



  @JsonKey(
    
    name: r'entidadId',
    required: true,
    includeIfNull: false,
  )


  final String entidadId;



      /// Todo como cadena, y las claves se ordenan al serializar. Un hash solo sirve si dos implementaciones producen el mismo.
  @JsonKey(
    
    name: r'campos',
    required: true,
    includeIfNull: false,
  )


  final Map<String, String> campos;



  @JsonKey(
    
    name: r'ocurridoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime ocurridoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is HechoASellar &&
      other.tipoEntidad == tipoEntidad &&
      other.entidadId == entidadId &&
      other.campos == campos &&
      other.ocurridoEn == ocurridoEn;

    @override
    int get hashCode =>
        tipoEntidad.hashCode +
        entidadId.hashCode +
        campos.hashCode +
        ocurridoEn.hashCode;

  factory HechoASellar.fromJson(Map<String, dynamic> json) => _$HechoASellarFromJson(json);

  Map<String, dynamic> toJson() => _$HechoASellarToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
enum HechoASellarTipoEntidadEnum {
    /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
@JsonValue(r'ACUERDO')
ACUERDO(r'ACUERDO'),
    /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
@JsonValue(r'COBERTURA')
COBERTURA(r'COBERTURA'),
    /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
@JsonValue(r'ENTREGA')
ENTREGA(r'ENTREGA'),
    /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
@JsonValue(r'PAGO')
PAGO(r'PAGO'),
    /// Catalogo cerrado de ck_registro_sellado_tipo_entidad.
@JsonValue(r'SANCION')
SANCION(r'SANCION');

const HechoASellarTipoEntidadEnum(this.value);

final String value;

@override
String toString() => value;
}


