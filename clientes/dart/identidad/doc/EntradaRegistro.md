# aportaya_cliente_identidad.model.EntradaRegistro

## Load the model package
```dart
import 'package:aportaya_cliente_identidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**telefonoE164** | **String** |  | 
**nombres** | **String** |  | 
**apellidos** | **String** |  | 
**fechaNacimiento** | [**DateTime**](DateTime.md) |  | 
**contrasena** | **String** | La clave con la que se va a ingresar. Es obligatoria: el alta no creaba credencial y despues no habia con que entrar — se completaban los ocho pasos y el formulario de ingreso pedia una contrasena que nunca se eligio. El minimo de ocho es el de `PoliticaDeClave`; el resto de la politica —no derivada del telefono ni del documento— la aplica el servicio, que es el unico que puede. | 
**correo** | **String** | Opcional en el contrato y pedido por la app. Una billetera manda extractos y comprobantes, y ese es su destino; ademas es lo que permite elegir el correo como canal de verificacion en vez del SMS. | [optional] 
**canalVerificacion** | **String** | Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`. | [optional] 
**documento** | [**Documento**](Documento.md) |  | 
**aceptaContratos** | **List&lt;String&gt;** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


