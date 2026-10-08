# aportaya_cliente_transparencia.model.EntradaEvaluacionRiesgo

## Load the model package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**ambito** | **String** | Catalogo cerrado de ck_alerta_riesgo_ambito. | 
**ambitoId** | **String** |  | [optional] 
**periodoId** | **String** |  | [optional] 
**periodoCerrado** | **bool** |  | [optional] [default to true]
**metricas** | [**List&lt;MetricaDeRiesgo&gt;**](MetricaDeRiesgo.md) |  | 
**codigoDeAlerta** | **Map&lt;String, String&gt;** | De codigo de metrica al codigo de alerta. Es catalogo, no constante. | 
**observaciones** | **int** |  | 
**puntajeDeRiesgo** | **String** | NO sale de aca hacia el usuario. Quien conoce el numero lo puede jugar. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


