//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_diligencia_documentos_inner.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_diligencia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDiligencia {
  /// Returns a new [EntradaDiligencia] instance.
  EntradaDiligencia({

    required  this.nivelDestino,

    required  this.documentos,

    required  this.aprobadaPor,

     this.segundaRevisionPor,
  });

  @JsonKey(

    name: r'nivelDestino',
    required: true,
    includeIfNull: false,
  )


  final EntradaDiligenciaNivelDestinoEnum nivelDestino;



  @JsonKey(

    name: r'documentos',
    required: true,
    includeIfNull: false,
  )


  final List<EntradaDiligenciaDocumentosInner> documentos;



  @JsonKey(

    name: r'aprobadaPor',
    required: true,
    includeIfNull: false,
  )


  final String aprobadaPor;



  @JsonKey(

    name: r'segundaRevisionPor',
    required: false,
    includeIfNull: false,
  )


  final String? segundaRevisionPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDiligencia &&
      other.nivelDestino == nivelDestino &&
      other.documentos == documentos &&
      other.aprobadaPor == aprobadaPor &&
      other.segundaRevisionPor == segundaRevisionPor;

    @override
    int get hashCode =>
        nivelDestino.hashCode +
        documentos.hashCode +
        aprobadaPor.hashCode +
        segundaRevisionPor.hashCode;

  factory EntradaDiligencia.fromJson(Map<String, dynamic> json) => _$EntradaDiligenciaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDiligenciaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDiligenciaNivelDestinoEnum {
@JsonValue(r'ESTANDAR')
ESTANDAR(r'ESTANDAR'),
@JsonValue(r'AMPLIADA')
AMPLIADA(r'AMPLIADA'),
@JsonValue(r'REFORZADA')
REFORZADA(r'REFORZADA');

const EntradaDiligenciaNivelDestinoEnum(this.value);

final String value;

@override
String toString() => value;
}


