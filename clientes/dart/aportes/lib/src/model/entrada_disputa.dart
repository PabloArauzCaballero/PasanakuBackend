//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_disputa.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDisputa {
  /// Returns a new [EntradaDisputa] instance.
  EntradaDisputa({

    required  this.tipo,

    required  this.descripcion,

    required  this.montoDisputado,

    required  this.referenciaDelProveedor,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaDisputaTipoEnum tipo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    
    name: r'montoDisputado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoDisputado;



  @JsonKey(
    
    name: r'referenciaDelProveedor',
    required: true,
    includeIfNull: false,
  )


  final String referenciaDelProveedor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDisputa &&
      other.tipo == tipo &&
      other.descripcion == descripcion &&
      other.montoDisputado == montoDisputado &&
      other.referenciaDelProveedor == referenciaDelProveedor;

    @override
    int get hashCode =>
        tipo.hashCode +
        descripcion.hashCode +
        montoDisputado.hashCode +
        referenciaDelProveedor.hashCode;

  factory EntradaDisputa.fromJson(Map<String, dynamic> json) => _$EntradaDisputaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDisputaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDisputaTipoEnum {
@JsonValue(r'CONTRACARGO')
CONTRACARGO(r'CONTRACARGO'),
@JsonValue(r'DESCONOCIMIENTO')
DESCONOCIMIENTO(r'DESCONOCIMIENTO'),
@JsonValue(r'MONTO_INCORRECTO')
MONTO_INCORRECTO(r'MONTO_INCORRECTO');

const EntradaDisputaTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


