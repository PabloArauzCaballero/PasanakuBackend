# aportaya_cliente_identidad.model.Documento

## Load the model package
```dart
import 'package:aportaya_cliente_identidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tipo** | **String** |  | 
**numero** | **String** |  | 
**lugarExpedicion** | **String** | La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


