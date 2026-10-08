//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/devolucion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_devolucion_fondo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDevolucionFondo {
  /// Returns a new [SalidaDevolucionFondo] instance.
  SalidaDevolucionFondo({

    required  this.fondoId,

    required  this.totalAportado,

    required  this.totalDevuelto,

    required  this.consumidoPorCoberturas,

    required  this.devoluciones,
  });

  @JsonKey(
    
    name: r'fondoId',
    required: true,
    includeIfNull: false,
  )


  final String fondoId;



  @JsonKey(
    
    name: r'totalAportado',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalAportado;



  @JsonKey(
    
    name: r'totalDevuelto',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalDevuelto;



      /// lo que el fondo gasto — lo pierden todos en proporcion
  @JsonKey(
    
    name: r'consumidoPorCoberturas',
    required: true,
    includeIfNull: false,
  )


  final Dinero consumidoPorCoberturas;



  @JsonKey(
    
    name: r'devoluciones',
    required: true,
    includeIfNull: false,
  )


  final List<DevolucionDeParticipante> devoluciones;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDevolucionFondo &&
      other.fondoId == fondoId &&
      other.totalAportado == totalAportado &&
      other.totalDevuelto == totalDevuelto &&
      other.consumidoPorCoberturas == consumidoPorCoberturas &&
      other.devoluciones == devoluciones;

    @override
    int get hashCode =>
        fondoId.hashCode +
        totalAportado.hashCode +
        totalDevuelto.hashCode +
        consumidoPorCoberturas.hashCode +
        devoluciones.hashCode;

  factory SalidaDevolucionFondo.fromJson(Map<String, dynamic> json) => _$SalidaDevolucionFondoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDevolucionFondoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

