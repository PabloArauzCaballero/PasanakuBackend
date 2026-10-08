//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_devolucion_fondo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDevolucionFondo {
  /// Returns a new [EntradaDevolucionFondo] instance.
  EntradaDevolucionFondo({

    required  this.grupoCerrado,

    required  this.deudasVivas,
  });

      /// el fondo esta ahi para cubrir lo que falta; devolverlo antes lo deja sin respaldo justo cuando hace falta
  @JsonKey(
    
    name: r'grupoCerrado',
    required: true,
    includeIfNull: false,
  )


  final bool grupoCerrado;



      /// devolver mientras alguien debe es repartir plata que todavia tiene dueno
          // minimum: 0
  @JsonKey(
    
    name: r'deudasVivas',
    required: true,
    includeIfNull: false,
  )


  final int deudasVivas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDevolucionFondo &&
      other.grupoCerrado == grupoCerrado &&
      other.deudasVivas == deudasVivas;

    @override
    int get hashCode =>
        grupoCerrado.hashCode +
        deudasVivas.hashCode;

  factory EntradaDevolucionFondo.fromJson(Map<String, dynamic> json) => _$EntradaDevolucionFondoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDevolucionFondoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

