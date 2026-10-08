# aportaya_cliente_garantia.model.EntradaDisolucion

## Load the model package
```dart
import 'package:aportaya_cliente_garantia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**causal** | **String** |  | 
**motivo** | **String** | obligatorio. Un grupo se disuelve por algo, y ese algo tiene que quedar dicho para los que pierden con la decision | 
**totalAportado** | [**Dinero**](Dinero.md) |  | 
**totalEntregado** | [**Dinero**](Dinero.md) |  | 
**masaDisponible** | [**Dinero**](Dinero.md) |  | 
**posiciones** | [**List&lt;PosicionDeParticipante&gt;**](PosicionDeParticipante.md) |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


