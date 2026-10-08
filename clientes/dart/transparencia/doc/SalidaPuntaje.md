# aportaya_cliente_transparencia.model.SalidaPuntaje

## Load the model package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**puntajeId** | **String** |  | 
**puntaje** | **String** | Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza. | 
**nivelDeConfianza** | **String** |  | 
**componentes** | [**List&lt;Componente&gt;**](Componente.md) | Su suma es el total (R-REP-03). Es lo que hace que el numero se pueda apelar. | 
**puntajeAnterior** | **String** |  | [optional] 
**eventosConsiderados** | **int** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


