//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'usuario_encontrado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UsuarioEncontrado {
  /// Returns a new [UsuarioEncontrado] instance.
  UsuarioEncontrado({

    required  this.existe,

     this.usuarioId,
  });

  @JsonKey(
    
    name: r'existe',
    required: true,
    includeIfNull: false,
  )


  final bool existe;



  @JsonKey(
    
    name: r'usuarioId',
    required: false,
    includeIfNull: false,
  )


  final String? usuarioId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UsuarioEncontrado &&
      other.existe == existe &&
      other.usuarioId == usuarioId;

    @override
    int get hashCode =>
        existe.hashCode +
        (usuarioId == null ? 0 : usuarioId.hashCode);

  factory UsuarioEncontrado.fromJson(Map<String, dynamic> json) => _$UsuarioEncontradoFromJson(json);

  Map<String, dynamic> toJson() => _$UsuarioEncontradoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

