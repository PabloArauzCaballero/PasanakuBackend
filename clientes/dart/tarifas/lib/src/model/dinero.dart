//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'dinero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Dinero {
  /// Returns a new [Dinero] instance.
  Dinero({

    required  this.monto,

    required  this.moneda,
  });

  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final DineroMonedaEnum moneda;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Dinero &&
      other.monto == monto &&
      other.moneda == moneda;

    @override
    int get hashCode =>
        monto.hashCode +
        moneda.hashCode;

  factory Dinero.fromJson(Map<String, dynamic> json) => _$DineroFromJson(json);

  Map<String, dynamic> toJson() => _$DineroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DineroMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const DineroMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


