# aportaya_cliente_grupos.model.EntradaGrupo

## Load the model package
```dart
import 'package:aportaya_cliente_grupos/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**nombre** | **String** |  | 
**montoAporte** | [**Dinero**](Dinero.md) |  | 
**periodicidad** | **String** |  | 
**cupos** | **int** |  | 
**diaCobro** | **int** |  | 
**modalidadTurnos** | **String** |  | 
**fechaDeInicio** | [**DateTime**](DateTime.md) |  | 
**organizadorId** | **String** | Quien lo administra, cuando lo administra alguien. Un grupo sin organizador lo lleva la plataforma, y entonces nadie cobra por administrarlo (RN-18). | [optional] 
**permitePermutaDeTurnos** | **bool** |  | [optional] [default to false]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


