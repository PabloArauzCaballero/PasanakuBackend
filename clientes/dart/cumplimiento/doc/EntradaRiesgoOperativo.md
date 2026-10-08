# aportaya_cliente_cumplimiento.model.EntradaRiesgoOperativo

## Load the model package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**categoriaEvento** | **String** | Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada. |
**factorRiesgo** | **String** |  |
**lineaNegocio** | **String** |  |
**descripcion** | **String** |  |
**fechaOcurrencia** | [**DateTime**](DateTime.md) |  |
**fechaDeteccion** | [**DateTime**](DateTime.md) | Nunca anterior a la ocurrencia. Es el error de carga mas comun, y una fecha invertida rompe el unico indicador que de verdad importa: cuanto tardamos en darnos cuenta. |
**perdidaBruta** | **String** | CADENA decimal. Cero es valido: la casi-perdida se registra igual, porque la frecuencia tambien es informacion. |
**recuperacion** | **String** | Lo ya recuperado al registrar. La recuperacion POSTERIOR no edita el evento: la tabla es append-only y la perdida bruta no se toca. | [optional]
**moneda** | **String** |  |
**responsableId** | **String** | Sin responsable no hay plan de accion, y el evento se registra igual. Es peor perder el registro que registrar sin plan. | [optional]
**accionComprometida** | **String** |  | [optional]
**fechaCompromiso** | [**DateTime**](DateTime.md) |  | [optional]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


