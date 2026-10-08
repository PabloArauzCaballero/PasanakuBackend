# aportaya_cliente_transparencia.model.SalidaEventoReputacion

## Load the model package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**eventoId** | **String** |  | 
**impacto** | **String** | Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza. | 
**factorAfectado** | **String** |  | [optional] 
**esNuevo** | **bool** | false cuando el hecho ya estaba puntuado (R-REP-01). El reintento es inocuo. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


