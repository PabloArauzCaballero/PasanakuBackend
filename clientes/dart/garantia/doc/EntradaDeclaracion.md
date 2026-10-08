# aportaya_cliente_garantia.model.EntradaDeclaracion

## Load the model package
```dart
import 'package:aportaya_cliente_garantia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigoExpediente** | **String** |  | 
**usuarioId** | **String** |  | 
**participanteId** | **String** |  | 
**grupoId** | **String** |  | 
**periodoId** | **String** |  | [optional] 
**cupoId** | **String** |  | [optional] 
**obligacionId** | **String** |  | 
**tipo** | **String** |  | 
**severidad** | **String** |  | 
**origenDeteccion** | **String** |  | 
**montoInvolucrado** | [**Dinero**](Dinero.md) |  | 
**diasMora** | **int** |  | 
**afectoALaEntrega** | **bool** |  | [optional] [default to false]
**tipoDeEvidencia** | **String** |  | 
**descripcionDeLaEvidencia** | **String** | obligatoria. Declarar sin con que probarlo deja al participante sin nada contra que defenderse | 
**urlDeLaEvidencia** | **String** |  | [optional] 
**hashDeLaEvidencia** | **String** |  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


