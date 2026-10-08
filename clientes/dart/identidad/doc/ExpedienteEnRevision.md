# aportaya_cliente_identidad.model.ExpedienteEnRevision

## Load the model package
```dart
import 'package:aportaya_cliente_identidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**verificacionId** | **String** |  | 
**usuarioId** | **String** |  | 
**nombreCompleto** | **String** |  | 
**documento** | **String** | Tipo y lugar de expedicion; el numero NUNCA viaja. | [optional] 
**estado** | **String** |  | 
**iniciadaEn** | [**DateTime**](DateTime.md) |  | 
**resueltaEn** | [**DateTime**](DateTime.md) |  | [optional] 
**motivoRechazo** | **String** |  | [optional] 
**fotos** | **List&lt;String&gt;** | Que caras tiene cargadas. Un expediente sin las tres no se puede aprobar. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


