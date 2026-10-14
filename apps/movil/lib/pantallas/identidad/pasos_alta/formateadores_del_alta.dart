import 'package:flutter/services.dart';

final formateadoresDelNumeroDeCi = <TextInputFormatter>[
  FilteringTextInputFormatter.digitsOnly,
  LengthLimitingTextInputFormatter(10),
];
