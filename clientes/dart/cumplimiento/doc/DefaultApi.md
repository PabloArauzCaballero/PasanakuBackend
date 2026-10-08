# aportaya_cliente_cumplimiento.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**aceptarContrato**](DefaultApi.md#aceptarcontrato) | **POST** /cumplimiento/contratos/{contratoId}/aceptaciones | Aceptar el contrato de adhesion y el tarifario
[**declararPep**](DefaultApi.md#declararpep) | **POST** /cumplimiento/usuarios/{usuarioId}/pep | Declaracion PEP y beneficiario final
[**elevarDiligencia**](DefaultApi.md#elevardiligencia) | **POST** /cumplimiento/usuarios/{usuarioId}/diligencia | Elevar el nivel de debida diligencia
[**listarContratosVigentes**](DefaultApi.md#listarcontratosvigentes) | **GET** /cumplimiento/contratos/vigentes | Los contratos de adhesion que rigen hoy
[**notificarTitulares**](DefaultApi.md#notificartitulares) | **POST** /cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones | Deja constancia de la notificacion a los titulares afectados
[**registrarIncidente**](DefaultApi.md#registrarincidente) | **POST** /cumplimiento/seguridad/incidentes | Abre el expediente de un incidente de seguridad y arranca sus relojes
[**registrarRiesgoOperativo**](DefaultApi.md#registrarriesgooperativo) | **POST** /cumplimiento/riesgos/eventos | Registra un evento de riesgo operativo y su plan de accion
[**reportarIncidente**](DefaultApi.md#reportarincidente) | **POST** /cumplimiento/seguridad/incidentes/{incidenteId}/reportes | Deja constancia de que el incidente se reporto al organismo
[**verificarAlcance**](DefaultApi.md#verificaralcance) | **GET** /licencia/alcance | Verificar el alcance de la licencia


# **aceptarContrato**
> SalidaAceptacion aceptarContrato(idempotencyKey, contratoId, entradaAceptacion)

Aceptar el contrato de adhesion y el tarifario

Vive aca y no en identidad —aunque la ficha del CU apunte alla— porque contrato_adhesion y aceptacion_contrato estan en el esquema de cumplimiento, y el esquema tiene precedencia sobre la ficha. Append-only: publicar una version nueva no toca las aceptaciones viejas.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String contratoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |
final EntradaAceptacion entradaAceptacion = ; // EntradaAceptacion |

try {
    final response = api.aceptarContrato(idempotencyKey, contratoId, entradaAceptacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aceptarContrato: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |
 **contratoId** | **String**|  |
 **entradaAceptacion** | [**EntradaAceptacion**](EntradaAceptacion.md)|  |

### Return type

[**SalidaAceptacion**](SalidaAceptacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **declararPep**
> SalidaPep declararPep(usuarioId, entradaPep)

Declaracion PEP y beneficiario final

Mira tambien a los beneficiarios finales: un titular que no es PEP pero cuya estructura de control termina en un PEP es exactamente el caso que la norma quiere ver.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |
final EntradaPep entradaPep = ; // EntradaPep |

try {
    final response = api.declararPep(usuarioId, entradaPep);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->declararPep: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  |
 **entradaPep** | [**EntradaPep**](EntradaPep.md)|  |

### Return type

[**SalidaPep**](SalidaPep.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **elevarDiligencia**
> SalidaDiligencia elevarDiligencia(idempotencyKey, usuarioId, entradaDiligencia)

Elevar el nivel de debida diligencia

Subir de nivel desbloquea topes mas altos, asi que la puerta es igual de dura que los topes que abre. Nunca baja: un destino que no supera al nivel actual se rechaza en vez de aplicarse.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |
final EntradaDiligencia entradaDiligencia = ; // EntradaDiligencia |

try {
    final response = api.elevarDiligencia(idempotencyKey, usuarioId, entradaDiligencia);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->elevarDiligencia: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |
 **usuarioId** | **String**|  |
 **entradaDiligencia** | [**EntradaDiligencia**](EntradaDiligencia.md)|  |

### Return type

[**SalidaDiligencia**](SalidaDiligencia.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listarContratosVigentes**
> List<ContratoVigente> listarContratosVigentes()

Los contratos de adhesion que rigen hoy

**Ruta publica, y tiene que serlo.** Se consulta durante el alta, cuando por definicion todavia no hay sesion: quien se registra tiene que poder leer que contrato esta aceptando ANTES de existir como usuario. Lo que devuelve es lo mismo que ya esta publicado en el sitio web —codigo, version, url del documento y su hash—; no hay dato de nadie aca. Sin esto la app no puede armar `aceptaContratos` de CU-01, que son UUID: los ids son `gen_random_uuid()` por entorno y no se pueden cablear.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();

try {
    final response = api.listarContratosVigentes();
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->listarContratosVigentes: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;ContratoVigente&gt;**](ContratoVigente.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **notificarTitulares**
> notificarTitulares(incidenteId, idempotencyKey)

Deja constancia de la notificacion a los titulares afectados

Se notifica con lo que se sabe, sin esperar al numero final: un aviso tardio y exacto le sirve menos a la persona afectada que uno a tiempo y aproximado. Sobre un incidente que no afecto datos personales se rechaza: asustar a gente sin motivo gasta la credibilidad del canal para cuando de verdad haga falta.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String incidenteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    api.notificarTitulares(incidenteId, idempotencyKey);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->notificarTitulares: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **incidenteId** | **String**|  |
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarIncidente**
> SalidaIncidente registrarIncidente(idempotencyKey, entradaIncidente)

Abre el expediente de un incidente de seguridad y arranca sus relojes

Contener rapido, reportar en plazo y notificar a los titulares cuando hay datos personales afectados. Son TRES relojes distintos corriendo en paralelo, y los tres se guardan: recalcularlos al consultar los haria moverse solos, y bastaria cambiar la politica el mes que viene para que un incidente de hace tres meses apareciera reportado en plazo cuando no lo estuvo. Sin responsable de seguridad designado y activo NO se abre el incidente. No es burocracia: sin designacion no hay a quien le corran los plazos ni quien firme ante el organismo. El campo datosPersonalesAfectados es lo que DECLARA quien reporta, y lo puede sobrescribir el activo: si el inventario dice que ese activo contiene datos personales, los contiene. Quien esta apagando el incendio a las tres de la manana no esta en condiciones de determinar el alcance. El enlace con el contrato del proveedor se resuelve POR EL ACTIVO: la tabla incidente_seguridad no tiene columna propia para el, y agregarsela duplicaria un dato que ya vive en el inventario de activos. Vive en cumplimiento y no en auditoria, donde lo pone su ficha, porque las tablas que escribe estan en el esquema cumplimiento (invariante 11).

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaIncidente entradaIncidente = ; // EntradaIncidente |

try {
    final response = api.registrarIncidente(idempotencyKey, entradaIncidente);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarIncidente: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |
 **entradaIncidente** | [**EntradaIncidente**](EntradaIncidente.md)|  |

### Return type

[**SalidaIncidente**](SalidaIncidente.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarRiesgoOperativo**
> SalidaRiesgoOperativo registrarRiesgoOperativo(idempotencyKey, entradaRiesgoOperativo)

Registra un evento de riesgo operativo y su plan de accion

Ponerle numero a lo que cuesta hacer las cosas mal, con las categorias y factores que exige la norma, y cerrar el circulo con un plan de accion con responsable y plazo. Una base de perdidas sin plan de accion es una coleccion de anecdotas caras. Un evento SIN perdida monetaria se registra igual, con `perdidaBruta` en cero: la frecuencia tambien es informacion, y filtrar las casi-perdidas por no costar plata deja el analisis ciego justo para lo que todavia no salio caro. La `perdidaNeta` que devuelve es la que calculo el MOTOR: la columna es GENERATED. La aplicacion no la reemplaza, la lee de vuelta (`R-RIS-02`). La ruta va bajo `/cumplimiento` y no bajo `/riesgos` como dice la ficha del caso de uso: `/riesgos` no es un prefijo reservado de este servicio, y una ruta fuera de los prefijos es un rechazo automatico. Este caso de uso vive en `cumplimiento` y no en `auditoria` —donde lo pone su ficha— porque las cuatro tablas que escribe estan en el esquema `cumplimiento`, y `svc_auditoria` no tiene GRANT sobre el. Donde la ficha y el modelo no coinciden, manda el modelo (invariante 11).

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRiesgoOperativo entradaRiesgoOperativo = ; // EntradaRiesgoOperativo |

try {
    final response = api.registrarRiesgoOperativo(idempotencyKey, entradaRiesgoOperativo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarRiesgoOperativo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |
 **entradaRiesgoOperativo** | [**EntradaRiesgoOperativo**](EntradaRiesgoOperativo.md)|  |

### Return type

[**SalidaRiesgoOperativo**](SalidaRiesgoOperativo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reportarIncidente**
> reportarIncidente(incidenteId, idempotencyKey)

Deja constancia de que el incidente se reporto al organismo

Se reporta IGUAL aunque el plazo haya vencido: llegar tarde es una observacion, no reportar es un incumplimiento. Lo que no se hace es disimularlo: el vencimiento queda visible y el control diario abre el hallazgo.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String incidenteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    api.reportarIncidente(incidenteId, idempotencyKey);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->reportarIncidente: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **incidenteId** | **String**|  |
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). |

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarAlcance**
> SalidaAlcance verificarAlcance(servicio, usuarioId)

Verificar el alcance de la licencia

Lo consultan los otros servicios antes de habilitar una operacion. Deniega por omision: sin licencia legible, sin alcance o sin sandbox, la respuesta es no. Un fallo de lectura no puede traducirse en «segui». No lleva Idempotency-Key porque no tiene efecto: solo responde.

### Example
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';

final api = AportayaClienteCumplimiento().getDefaultApi();
final String servicio = servicio_example; // String |
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String |

try {
    final response = api.verificarAlcance(servicio, usuarioId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarAlcance: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **servicio** | **String**|  |
 **usuarioId** | **String**|  | [optional]

### Return type

[**SalidaAlcance**](SalidaAlcance.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

