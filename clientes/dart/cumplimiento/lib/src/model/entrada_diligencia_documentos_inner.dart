//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_diligencia_documentos_inner.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDiligenciaDocumentosInner {
  /// Returns a new [EntradaDiligenciaDocumentosInner] instance.
  EntradaDiligenciaDocumentosInner({

    required  this.tipo,

    required  this.url,

    required  this.hash,
  });

  @JsonKey(

    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final String tipo;



  @JsonKey(

    name: r'url',
    required: true,
    includeIfNull: false,
  )


  final String url;



  @JsonKey(

    name: r'hash',
    required: true,
    includeIfNull: false,
  )


  final String hash;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDiligenciaDocumentosInner &&
      other.tipo == tipo &&
      other.url == url &&
      other.hash == hash;

    @override
    int get hashCode =>
        tipo.hashCode +
        url.hashCode +
        hash.hashCode;

  factory EntradaDiligenciaDocumentosInner.fromJson(Map<String, dynamic> json) => _$EntradaDiligenciaDocumentosInnerFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDiligenciaDocumentosInnerToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

