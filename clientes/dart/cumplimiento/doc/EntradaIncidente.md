# aportaya_cliente_cumplimiento.model.EntradaIncidente

## Load the model package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tipo** | **String** |  |
**severidad** | **String** |  |
**activoInformacionId** | **String** | El activo del inventario que se vio afectado. De el salen la clasificacion de datos y el contrato del proveedor, si lo hay. | [optional]
**datosPersonalesAfectados** | **bool** | Lo que declara quien reporta. El activo lo puede sobrescribir: la clasificacion del inventario sabe mas que quien esta apagando el incendio. |
**usuariosAfectados** | **int** | Aproximado esta bien. Esperar al numero final para notificar es como se pierde el plazo de notificacion. |
**detectadoEn** | [**DateTime**](DateTime.md) | Desde aca corren los plazos, y no desde la ocurrencia: no se le puede exigir a nadie reportar algo que todavia no sabia. |
**eventoRiesgoId** | **String** | La perdida asociada, si ya se registro (CU-54). | [optional]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


