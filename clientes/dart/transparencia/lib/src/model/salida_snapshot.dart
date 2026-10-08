//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_snapshot.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSnapshot {
  /// Returns a new [SalidaSnapshot] instance.
  SalidaSnapshot({

    required  this.snapshotId,
  });

  @JsonKey(
    
    name: r'snapshotId',
    required: true,
    includeIfNull: false,
  )


  final String snapshotId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSnapshot &&
      other.snapshotId == snapshotId;

    @override
    int get hashCode =>
        snapshotId.hashCode;

  factory SalidaSnapshot.fromJson(Map<String, dynamic> json) => _$SalidaSnapshotFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSnapshotToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

