# aportaya_cliente_cumplimiento.model.SalidaRiesgoOperativo

## Load the model package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**eventoId** | **String** |  |
**codigo** | **String** | Derivado del hecho —fecha de deteccion y categoria— para que sea legible en un listado y para que dos cargas del mismo hecho choquen contra el UNIQUE en vez de duplicar la base de perdidas. |
**perdidaNeta** | **String** | Bruta menos recuperacion, calculada por el motor. |
**moneda** | **String** |  |
**planAccionId** | **String** |  | [optional]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


