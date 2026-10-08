//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/posicion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_disolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDisolucion {
  /// Returns a new [EntradaDisolucion] instance.
  EntradaDisolucion({

    required  this.causal,

    required  this.motivo,

    required  this.totalAportado,

    required  this.totalEntregado,

    required  this.masaDisponible,

    required  this.posiciones,
  });

  @JsonKey(
    
    name: r'causal',
    required: true,
    includeIfNull: false,
  )


  final EntradaDisolucionCausalEnum causal;



      /// obligatorio. Un grupo se disuelve por algo, y ese algo tiene que quedar dicho para los que pierden con la decision
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    
    name: r'totalAportado',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalAportado;



  @JsonKey(
    
    name: r'totalEntregado',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalEntregado;



  @JsonKey(
    
    name: r'masaDisponible',
    required: true,
    includeIfNull: false,
  )


  final Dinero masaDisponible;



  @JsonKey(
    
    name: r'posiciones',
    required: true,
    includeIfNull: false,
  )


  final List<PosicionDeParticipante> posiciones;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDisolucion &&
      other.causal == causal &&
      other.motivo == motivo &&
      other.totalAportado == totalAportado &&
      other.totalEntregado == totalEntregado &&
      other.masaDisponible == masaDisponible &&
      other.posiciones == posiciones;

    @override
    int get hashCode =>
        causal.hashCode +
        motivo.hashCode +
        totalAportado.hashCode +
        totalEntregado.hashCode +
        masaDisponible.hashCode +
        posiciones.hashCode;

  factory EntradaDisolucion.fromJson(Map<String, dynamic> json) => _$EntradaDisolucionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDisolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaDisolucionCausalEnum {
@JsonValue(r'ACUERDO')
ACUERDO(r'ACUERDO'),
@JsonValue(r'CAUSA_GRAVE')
CAUSA_GRAVE(r'CAUSA_GRAVE'),
@JsonValue(r'MORA_GENERALIZADA')
MORA_GENERALIZADA(r'MORA_GENERALIZADA'),
@JsonValue(r'SIN_REEMPLAZO')
SIN_REEMPLAZO(r'SIN_REEMPLAZO');

const EntradaDisolucionCausalEnum(this.value);

final String value;

@override
String toString() => value;
}


