//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_identidad/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'limite_aplicable.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LimiteAplicable {
  /// Returns a new [LimiteAplicable] instance.
  LimiteAplicable({

    required  this.concepto,

    required  this.ventana,

    required  this.monto,
  });

  @JsonKey(
    
    name: r'concepto',
    required: true,
    includeIfNull: false,
  )


  final String concepto;



  @JsonKey(
    
    name: r'ventana',
    required: true,
    includeIfNull: false,
  )


  final String ventana;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LimiteAplicable &&
      other.concepto == concepto &&
      other.ventana == ventana &&
      other.monto == monto;

    @override
    int get hashCode =>
        concepto.hashCode +
        ventana.hashCode +
        monto.hashCode;

  factory LimiteAplicable.fromJson(Map<String, dynamic> json) => _$LimiteAplicableFromJson(json);

  Map<String, dynamic> toJson() => _$LimiteAplicableToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

