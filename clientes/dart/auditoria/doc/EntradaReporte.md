# aportaya_cliente_auditoria.model.EntradaReporte

## Load the model package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**parametros** | **Map&lt;String, String&gt;** | Se validan contra `parametros_esperados` de la definicion. Viajan como cadena a proposito: el tipo lo declara el reporte, no el llamador, y asi no hay dos opiniones sobre si «2026-01» es una fecha o un texto. | 
**formato** | **String** | Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`. | [optional] 
**justificacion** | **String** | Obligatoria cuando la definicion tiene `contiene_datos_sensibles`. El minimo de diez no es capricho: lo exige `ck_acceso_justificacion`, porque una justificacion de tres letras no se puede leer dentro de un ano. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


