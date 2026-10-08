//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_pep_beneficiarios_finales_inner.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPepBeneficiariosFinalesInner {
  /// Returns a new [EntradaPepBeneficiariosFinalesInner] instance.
  EntradaPepBeneficiariosFinalesInner({

    required  this.nombre,

    required  this.documento,

     this.esPep,

     this.tipoPep,
  });

  @JsonKey(

    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(

    name: r'documento',
    required: true,
    includeIfNull: false,
  )


  final String documento;



  @JsonKey(

    name: r'esPep',
    required: false,
    includeIfNull: false,
  )


  final bool? esPep;



  @JsonKey(

    name: r'tipoPep',
    required: false,
    includeIfNull: false,
  )


  final EntradaPepBeneficiariosFinalesInnerTipoPepEnum? tipoPep;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPepBeneficiariosFinalesInner &&
      other.nombre == nombre &&
      other.documento == documento &&
      other.esPep == esPep &&
      other.tipoPep == tipoPep;

    @override
    int get hashCode =>
        nombre.hashCode +
        documento.hashCode +
        esPep.hashCode +
        tipoPep.hashCode;

  factory EntradaPepBeneficiariosFinalesInner.fromJson(Map<String, dynamic> json) => _$EntradaPepBeneficiariosFinalesInnerFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPepBeneficiariosFinalesInnerToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPepBeneficiariosFinalesInnerTipoPepEnum {
@JsonValue(r'NACIONAL')
NACIONAL(r'NACIONAL'),
@JsonValue(r'EXTRANJERO')
EXTRANJERO(r'EXTRANJERO'),
@JsonValue(r'ORG_INTERNACIONAL')
ORG_INTERNACIONAL(r'ORG_INTERNACIONAL'),
@JsonValue(r'FAMILIAR')
FAMILIAR(r'FAMILIAR'),
@JsonValue(r'ALLEGADO')
ALLEGADO(r'ALLEGADO');

const EntradaPepBeneficiariosFinalesInnerTipoPepEnum(this.value);

final String value;

@override
String toString() => value;
}


