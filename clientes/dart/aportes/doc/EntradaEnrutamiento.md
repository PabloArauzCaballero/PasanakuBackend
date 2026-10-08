# aportaya_cliente_aportes.model.EntradaEnrutamiento

## Load the model package
```dart
import 'package:aportaya_cliente_aportes/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**claveIdempotencia** | **String** |  | 
**yaIntentados** | **List&lt;String&gt;** | Ausente equivale a lista vacía. Sin `default` a propósito — el generador Dart (dart-dio) emite el valor por omisión sin `const`, y una lista no-const no es una expresión constante válida (rompe la compilación del cliente). | [optional] 
**saludObservada** | **Map&lt;String, int&gt;** | Salud por codigo de proveedor, de quien la mide. No hay tabla de salud en aportes; un proveedor sin medicion se trata como sano porque acaba de pasar contrato y pruebas. Hueco declarado en planes/informes/carril-3A.md. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


