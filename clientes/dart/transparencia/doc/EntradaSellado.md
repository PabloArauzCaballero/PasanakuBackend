# aportaya_cliente_transparencia.model.EntradaSellado

## Load the model package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**grupoId** | **String** |  | 
**motivo** | **String** |  | 
**hechos** | [**List&lt;HechoASellar&gt;**](HechoASellar.md) |  | 
**desde** | [**DateTime**](DateTime.md) |  | 
**hasta** | [**DateTime**](DateTime.md) |  | 
**excepcionesDeConciliacionAbiertas** | **int** | Lo resuelve nucleo-financiero y llega resuelto: no se lee su esquema (invariante 11) ni se lo consulta dentro de la transaccion (invariante 6). | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


