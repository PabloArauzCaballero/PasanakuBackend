# aportaya_cliente_cumplimiento.model.ContratoVigente

## Load the model package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |
**codigo** | **String** |  |
**version** | **int** |  |
**tipo** | **String** |  |
**urlDocumento** | **String** |  |
**hashDocumento** | **String** | SHA-256 del documento publicado. Es lo que permite comprobar que el texto que se leyo en la app es el mismo que quedo registrado en la aceptacion. |
**vigenteDesde** | [**DateTime**](DateTime.md) |  |
**numeroRegistro** | **String** | El numero con el que ASFI registro el contrato de adhesion. Ausente mientras el registro no se haya obtenido: no se inventa. | [optional]
**fechaRegistro** | [**DateTime**](DateTime.md) |  | [optional]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


