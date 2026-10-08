# aportaya_cliente_tarifas.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_tarifas/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**aceptarCotizacion**](DefaultApi.md#aceptarcotizacion) | **POST** /comisiones/cotizaciones/{cotizacionId}/aceptacion | Registrar que el usuario acepto el precio que vio
[**anotarCobroDeComision**](DefaultApi.md#anotarcobrodecomision) | **POST** /comisiones/devengos/{devengoId}/cobros | Registrar el resultado de un intento de cobro
[**cerrarLiquidacion**](DefaultApi.md#cerrarliquidacion) | **POST** /tarifas/liquidaciones/{periodo}/cierre | Cerrar el mes de ingresos
[**consultarTarifarioVigente**](DefaultApi.md#consultartarifariovigente) | **GET** /tarifas/vigentes/{codigo} | Cual es el tarifario vigente de un codigo, ahora
[**cotizarComision**](DefaultApi.md#cotizarcomision) | **POST** /comisiones/cotizaciones | Calcular y guardar la comision de una operacion
[**crearSegmento**](DefaultApi.md#crearsegmento) | **POST** /tarifas/segmentos | Definir un segmento comercial
[**devengarComision**](DefaultApi.md#devengarcomision) | **POST** /comisiones/devengos | Reconocer el ingreso cuando ocurre el hecho generador
[**devolverComision**](DefaultApi.md#devolvercomision) | **POST** /comisiones/{devengoId}/devoluciones | Devolver una comision y emitir la nota de credito
[**emitirFactura**](DefaultApi.md#emitirfactura) | **POST** /facturas | Emitir el documento fiscal de una comision cobrada
[**ponerTarifarioVigente**](DefaultApi.md#ponertarifariovigente) | **POST** /tarifas/tarifarios/{tarifarioId}/vigencia | Poner en vigencia un tarifario cuyo preaviso ya se cumplio
[**publicarTarifario**](DefaultApi.md#publicartarifario) | **POST** /tarifas/tarifarios | Publicar la version siguiente de un tarifario
[**resolverPrecio**](DefaultApi.md#resolverprecio) | **POST** /tarifas/precios/resolver | Que segmento rige para un usuario, y por que


# **aceptarCotizacion**
> AceptarCotizacion200Response aceptarCotizacion(cotizacionId)

Registrar que el usuario acepto el precio que vio

Sin esto no hay evidencia de consentimiento. Una cotizacion vencida no se acepta: el precio que se acepta tiene que ser el que se mostro.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String cotizacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.aceptarCotizacion(cotizacionId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aceptarCotizacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cotizacionId** | **String**|  | 

### Return type

[**AceptarCotizacion200Response**](AceptarCotizacion200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **anotarCobroDeComision**
> SalidaCobro anotarCobroDeComision(devengoId, entradaCobro)

Registrar el resultado de un intento de cobro

El cobro lo ejecuta quien mueve el dinero (invariante 12). Aca se anota que paso: tres fallos y el devengo va a cuenta por cobrar, sin borrarse. La deuda no desaparece porque el debito falle.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String devengoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaCobro entradaCobro = ; // EntradaCobro | 

try {
    final response = api.anotarCobroDeComision(devengoId, entradaCobro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->anotarCobroDeComision: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **devengoId** | **String**|  | 
 **entradaCobro** | [**EntradaCobro**](EntradaCobro.md)|  | 

### Return type

[**SalidaCobro**](SalidaCobro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cerrarLiquidacion**
> SalidaLiquidacion cerrarLiquidacion(periodo, entradaLiquidacion)

Cerrar el mes de ingresos

Un resultado mensual reproducible desde los devengos, no desde una planilla aparte. Los dias sin cerrar, las excepciones abiertas y el saldo del mayor llegan como entrada: viven en otros esquemas y este servicio no los lee (invariante 11).

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String periodo = periodo_example; // String | 
final EntradaLiquidacion entradaLiquidacion = ; // EntradaLiquidacion | 

try {
    final response = api.cerrarLiquidacion(periodo, entradaLiquidacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cerrarLiquidacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **periodo** | **String**|  | 
 **entradaLiquidacion** | [**EntradaLiquidacion**](EntradaLiquidacion.md)|  | 

### Return type

[**SalidaLiquidacion**](SalidaLiquidacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarTarifarioVigente**
> TarifarioVigente consultarTarifarioVigente(codigo)

Cual es el tarifario vigente de un codigo, ahora

La pregunta CU-20 al crear un grupo: sin tarifario vigente no se abre nada (R-CON-07), porque el precio es lo que cada participante acepta al firmar el reglamento. Que no haya ninguno es una respuesta valida —significa que no se puede operar— y por eso `vigente` puede venir en falso en vez de fallar.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String codigo = codigo_example; // String | 

try {
    final response = api.consultarTarifarioVigente(codigo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarTarifarioVigente: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **codigo** | **String**|  | 

### Return type

[**TarifarioVigente**](TarifarioVigente.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cotizarComision**
> SalidaCotizacion cotizarComision(idempotencyKey, entradaCotizacion)

Calcular y guardar la comision de una operacion

Que el usuario vea el numero final ANTES de aceptar, y que ese numero quede guardado con su desglose. El reclamo tipico no es «me cobraron de mas»: es «nadie me aviso». Si la operacion pertenece a un grupo con tarifa congelada, manda el snapshot (R-TAR-07).

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCotizacion entradaCotizacion = ; // EntradaCotizacion | 

try {
    final response = api.cotizarComision(idempotencyKey, entradaCotizacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cotizarComision: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCotizacion** | [**EntradaCotizacion**](EntradaCotizacion.md)|  | 

### Return type

[**SalidaCotizacion**](SalidaCotizacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearSegmento**
> SalidaSegmento crearSegmento(entradaSegmento)

Definir un segmento comercial

El criterio es JSON evaluable, no texto libre: «se le aplico porque es buen cliente» no es una explicacion que sirva ante un reclamo. Un criterio que se apoya en datos sensibles o categorias protegidas se rechaza.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final EntradaSegmento entradaSegmento = ; // EntradaSegmento | 

try {
    final response = api.crearSegmento(entradaSegmento);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->crearSegmento: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaSegmento** | [**EntradaSegmento**](EntradaSegmento.md)|  | 

### Return type

[**SalidaSegmento**](SalidaSegmento.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **devengarComision**
> SalidaDevengo devengarComision(idempotencyKey, entradaDevengo)

Reconocer el ingreso cuando ocurre el hecho generador

Separa ganar de cobrar. El devengo es append-only: su estado corriente se DERIVA de los cargos y las devoluciones, no de un UPDATE sobre la fila.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaDevengo entradaDevengo = ; // EntradaDevengo | 

try {
    final response = api.devengarComision(idempotencyKey, entradaDevengo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->devengarComision: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaDevengo** | [**EntradaDevengo**](EntradaDevengo.md)|  | 

### Return type

[**SalidaDevengo**](SalidaDevengo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **devolverComision**
> SalidaDevolucion devolverComision(idempotencyKey, devengoId, entradaDevolucion)

Devolver una comision y emitir la nota de credito

La plata y el papel van atados. La factura NUNCA se edita (R-TAR-10): se emite una nota de credito con su propio CUF. El limite se mide contra lo ya devuelto.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String devengoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaDevolucion entradaDevolucion = ; // EntradaDevolucion | 

try {
    final response = api.devolverComision(idempotencyKey, devengoId, entradaDevolucion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->devolverComision: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **devengoId** | **String**|  | 
 **entradaDevolucion** | [**EntradaDevolucion**](EntradaDevolucion.md)|  | 

### Return type

[**SalidaDevolucion**](SalidaDevolucion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirFactura**
> SalidaFactura emitirFactura(entradaFactura)

Emitir el documento fiscal de una comision cobrada

La caida del servicio de impuestos NO detiene la operacion: se emite bajo contingencia con su plazo guardado. La consulta al tercero ocurre fuera de la transaccion (invariante 6).

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final EntradaFactura entradaFactura = ; // EntradaFactura | 

try {
    final response = api.emitirFactura(entradaFactura);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirFactura: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaFactura** | [**EntradaFactura**](EntradaFactura.md)|  | 

### Return type

[**SalidaFactura**](SalidaFactura.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ponerTarifarioVigente**
> PonerTarifarioVigente200Response ponerTarifarioVigente(tarifarioId)

Poner en vigencia un tarifario cuyo preaviso ya se cumplio

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final String tarifarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.ponerTarifarioVigente(tarifarioId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ponerTarifarioVigente: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tarifarioId** | **String**|  | 

### Return type

[**PonerTarifarioVigente200Response**](PonerTarifarioVigente200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicarTarifario**
> SalidaPublicacion publicarTarifario(entradaPublicacion)

Publicar la version siguiente de un tarifario

Un incremento no entra sin preaviso cumplido (R-TAR-08). El anterior no se borra: queda SUSTITUIDO con su vigencia cerrada, porque poder decir que se cobraba en una fecha pasada es lo unico que responde un reclamo viejo.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final EntradaPublicacion entradaPublicacion = ; // EntradaPublicacion | 

try {
    final response = api.publicarTarifario(entradaPublicacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->publicarTarifario: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaPublicacion** | [**EntradaPublicacion**](EntradaPublicacion.md)|  | 

### Return type

[**SalidaPublicacion**](SalidaPublicacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resolverPrecio**
> SalidaSegmentoAplicado resolverPrecio(resolverPrecioRequest)

Que segmento rige para un usuario, y por que

No escribe: es una consulta. Si falta un hecho para evaluar el criterio, NO se adivina — se cotiza al precio base y se dice cual falto. Adivinar a favor regala plata y adivinar en contra cobra de mas.

### Example
```dart
import 'package:aportaya_cliente_tarifas/api.dart';

final api = AportayaClienteTarifas().getDefaultApi();
final ResolverPrecioRequest resolverPrecioRequest = ; // ResolverPrecioRequest | 

try {
    final response = api.resolverPrecio(resolverPrecioRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->resolverPrecio: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **resolverPrecioRequest** | [**ResolverPrecioRequest**](ResolverPrecioRequest.md)|  | 

### Return type

[**SalidaSegmentoAplicado**](SalidaSegmentoAplicado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

