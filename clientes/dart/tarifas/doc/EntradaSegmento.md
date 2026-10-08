# aportaya_cliente_tarifas.model.EntradaSegmento

## Load the model package
```dart
import 'package:aportaya_cliente_tarifas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigo** | **String** |  | 
**descripcion** | **String** |  | 
**criterio** | **Map&lt;String, int&gt;** | hechos exigidos y su minimo. Evaluable, no texto libre. No puede referenciar datos sensibles ni categorias protegidas | 
**prioridad** | **int** | unica entre los activos; resuelve el empate al definir, no al cobrar | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


