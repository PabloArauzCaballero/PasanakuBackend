//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_pep_beneficiarios_finales_inner.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_pep.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPep {
  /// Returns a new [EntradaPep] instance.
  EntradaPep({

    required  this.esPep,

     this.tipoPep,

     this.cargo,

     this.institucion,

     this.beneficiariosFinales,
  });

  @JsonKey(

    name: r'esPep',
    required: true,
    includeIfNull: false,
  )


  final bool esPep;



  @JsonKey(

    name: r'tipoPep',
    required: false,
    includeIfNull: false,
  )


  final EntradaPepTipoPepEnum? tipoPep;



  @JsonKey(

    name: r'cargo',
    required: false,
    includeIfNull: false,
  )


  final String? cargo;



  @JsonKey(

    name: r'institucion',
    required: false,
    includeIfNull: false,
  )


  final String? institucion;



  @JsonKey(

    name: r'beneficiariosFinales',
    required: false,
    includeIfNull: false,
  )


  final List<EntradaPepBeneficiariosFinalesInner>? beneficiariosFinales;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPep &&
      other.esPep == esPep &&
      other.tipoPep == tipoPep &&
      other.cargo == cargo &&
      other.institucion == institucion &&
      other.beneficiariosFinales == beneficiariosFinales;

    @override
    int get hashCode =>
        esPep.hashCode +
        tipoPep.hashCode +
        cargo.hashCode +
        institucion.hashCode +
        beneficiariosFinales.hashCode;

  factory EntradaPep.fromJson(Map<String, dynamic> json) => _$EntradaPepFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPepToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPepTipoPepEnum {
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

const EntradaPepTipoPepEnum(this.value);

final String value;

@override
String toString() => value;
}


