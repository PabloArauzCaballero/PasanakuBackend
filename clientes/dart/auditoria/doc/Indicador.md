# aportaya_cliente_auditoria.model.Indicador

## Load the model package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigo** | **String** |  | 
**nombre** | **String** |  | 
**valor** | **String** | Cadena decimal, con la escala que guardo el calculo. No se redondea al publicar: el numero del tablero tiene que ser el numero de la tabla. AUSENTE cuando se suprimio por privacidad. La serie tambien viene vacia en ese caso: publicarla seria dar el numero por la puerta de atras. | [optional] 
**unidad** | **String** |  | 
**meta** | **String** | Ausente cuando no hay meta fijada para el periodo. Sin meta no hay semaforo, y ponerla despues de ver el resultado no es medir. | [optional] 
**cumpleMeta** | **bool** | Ausente cuando no hay meta. | [optional] 
**variacionPeriodoAnterior** | **String** | Ausente en un indicador sin historia. Sin periodo anterior no hay comparacion, y un cero fingido afirmaria que se mantuvo igual. | [optional] 
**serie** | [**List&lt;IndicadorSerieInner&gt;**](IndicadorSerieInner.md) | Los periodos anteriores, del mas viejo al mas nuevo. | [optional] 
**familia** | **String** | Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado). | 
**duenoFamilia** | **String** | Quien responde cuando el indicador esta en rojo. | 
**definicionVersion** | **String** | Con que version de la definicion se calculo. Es lo que permite que un numero de hace un ano vuelva a salir igual. | 
**suprimidoPorPrivacidad** | **bool** | Verdadero cuando la muestra no alcanza el minimo de la definicion. Un promedio de tres personas identifica a las tres (R-SEG-03). | 
**casos** | **int** | Tamano de la muestra, para poder explicar la supresion. | [optional] 
**minimoCasos** | **int** | El piso que fija la definicion del indicador. | [optional] 
**calculadoEn** | [**DateTime**](DateTime.md) |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


