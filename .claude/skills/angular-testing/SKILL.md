---
name: angular-testing
description: Tests unitarios y de componente en Angular — Vitest como runner del CLI, TestBed standalone, `setInput` para inputs signal, `whenStable` en zoneless, `TestBed.tick()` para effects, `HttpTestingController`, `RouterTestingHarness`, component harnesses del CDK, dobles de servicios y qué probar en componente vs servicio vs E2E. Usar al escribir o arreglar cualquier spec de Angular, al migrar desde Jasmine/Karma, o cuando un test solo pasa con `detectChanges` a ciegas.
---

# Testing en Angular

Complementa a `unit-testing` (principios, dobles, naming) con lo específico de Angular.
Lo que cruza red real o navegador real es E2E (`e2e-playwright`). Verificá las APIs en
angular.dev contra la versión instalada.

## 1. Runner

- Angular CLI usa **Vitest** como runner por defecto en proyectos nuevos, con el builder
  `@angular/build:unit-test`. Config avanzada en `vitest-base.config.ts`
  (`ng generate config vitest`); el CLI pisa `test.projects` y `test.include`.
- Proyectos con Karma/Jasmine: guía oficial de migración + schematic
  `ng g @schematics/angular:refactor-jasmine-vitest`. No mezcles los dos runners.
- Corré los tests por el script del proyecto (`ng test`/script de `package.json`), no
  invocando el runner a mano: la config del CLI no se aplica igual.
- Zoneless: `zone.js` fuera del target `test` de `angular.json`.

## 2. Qué se prueba dónde

| Nivel | Qué | Herramienta |
|---|---|---|
| Función pura | validadores, mappers, cálculos, `computed` de un store | test sin TestBed |
| Servicio / store | transiciones de estado, llamadas HTTP y mapeo de errores | `TestBed.inject` + `HttpTestingController` |
| Componente | render según inputs, eventos, estados (carga/vacío/error), accesibilidad del markup | `TestBed.createComponent` + harness |
| Ruta / guard / resolver | redirección, carga de datos por ruta | `RouterTestingHarness` |
| Flujo de usuario | login → acción → persistencia | Playwright |

Un componente tonto (`atomic-design-components`) se prueba por su contrato: inputs →
DOM, DOM → outputs. La lógica de negocio no se testea a través de un componente.

## 3. Componentes standalone

```ts
beforeEach(async () => {
  await TestBed.configureTestingModule({
    imports: [MemberCard],
    providers: [{ provide: MemberApi, useValue: memberApiStub }],
  }).compileComponents();

  fixture = TestBed.createComponent(MemberCard);
  fixture.componentRef.setInput('member', aMember({ fullName: 'Ana Pérez' }));
  await fixture.whenStable();
});

it('muestra las iniciales del participante', () => {
  expect(fixture.nativeElement.querySelector('.initials').textContent).toBe('AP');
});
```

- Inputs signal: **`fixture.componentRef.setInput('name', value)`**, nunca asignar la
  propiedad (es un `InputSignal`, no una variable).
- Zoneless: la doc recomienda `await fixture.whenStable()` y evitar
  `fixture.detectChanges()` cuando sea posible, para que el test se comporte como
  producción. Un test que solo pasa con `detectChanges()` esparcidos está tapando estado
  fuera de signals (`angular-development` §4).
- Effects pendientes: `TestBed.tick()` (`flushEffects()` está deprecado).
- Dependencias de un hijo pesado: reemplazá el hijo con un stub standalone del mismo
  selector, o testeá el padre por harness del hijo. Sin `NO_ERRORS_SCHEMA` para esconder
  errores de template.
- Animaciones nativas desactivadas por defecto en TestBed; `animationsEnabled: true`
  solo si el test las verifica.

## 4. Servicios y HTTP

```ts
TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
const http = TestBed.inject(HttpTestingController);
const api = TestBed.inject(MemberApi);

const result = firstValueFrom(api.getMember('p1'));
const req = http.expectOne('/api/members/p1');
expect(req.request.method).toBe('GET');
req.flush(aMemberDto());
expect(await result).toEqual(aMember());
http.verify();                                   // no quedaron pedidos sin atender
```

- `expectOne` por URL o predicado; `flush` para responder; `flush(body, { status: 422,
  statusText: 'Unprocessable Entity' })` para errores; `verify()` al final siempre.
- Testeá el **mapeo** (DTO → modelo, error HTTP → `ApiError` tipado), no que `HttpClient`
  funcione.
- Interceptors: se testean solos con `provideHttpClient(withInterceptors([...]))` +
  `HttpTestingController` mirando `req.request.headers`.
- `resource`/`httpResource`: escribí `params` y esperá `whenStable()`; verificá
  `status()`/`value()`. Si la API es experimental en tu versión, encapsulada detrás de
  la capa de datos el test no la conoce.

## 5. Rutas, guards y resolvers

```ts
TestBed.configureTestingModule({
  providers: [provideRouter([{ path: 'calendario', component: CalendarioPage, canActivate: [authGuard] }]), provideHttpClientTesting(), { provide: SessionStore, useValue: { isAuthenticated: signal(false) } }],
});
const harness = await RouterTestingHarness.create();
await harness.navigateByUrl('/calendario');
expect(TestBed.inject(Router).url).toBe('/login');
```

Guards funcionales se testean también en aislamiento con
`TestBed.runInInjectionContext(() => authGuard(route, state))`.

## 6. Component harnesses

Para los componentes compartidos de la casa, escribí un harness (`@angular/cdk/testing`
— el CDK no es Material) y usalo desde los tests de quien los consume: el test no conoce
el DOM interno del componente y sobrevive a sus refactors.

```ts
export class DatePickerHarness extends ComponentHarness {
  static hostSelector = 'app-date-picker';
  private input = this.locatorFor('input');
  async setDate(iso: string) { const i = await this.input(); await i.clear(); await i.sendKeys(iso); await i.blur(); }
}
// en el test del consumidor
const loader = TestbedHarnessEnvironment.loader(fixture);
const picker = await loader.getHarness(DatePickerHarness);
```

Los locators devuelven funciones (`this.input()`), no elementos, para no cachear nodos
viejos. El mismo harness sirve en E2E con el entorno correspondiente.

## 7. Dobles

- Servicios: objeto literal con `vi.fn()` o un stub tipado `Partial<T>`; un signal real
  para estado (`isAuthenticated: signal(false)`) — así el componente reacciona igual que
  en producción.
- No mockees lo que es barato de usar real (pipes, componentes chicos, stores puros).
- Reloj: `vi.useFakeTimers()`/`vi.setSystemTime()` para fechas, debounce y timers; datos
  con factories deterministas (`test-data-management`).

## 8. Anti-patrones

- `detectChanges()` en cascada hasta que pase.
- `NO_ERRORS_SCHEMA` o `CUSTOM_ELEMENTS_SCHEMA` para silenciar un template roto.
- Asignar un input signal como propiedad; mutar `componentInstance` desde afuera.
- Aserciones sobre clases CSS de estilo en vez de contenido, roles y estados.
- Testear lógica de negocio a través del DOM.
- `expectOne` sin `verify()`; pedidos "sobrantes" que nadie vio.
- `setTimeout` para esperar: `whenStable()`, fake timers o `TestBed.tick()`.
- Tests que sobreviven solo con `test.skip` o `retry`. Un rojo se diagnostica
  (`e2e-failure-triage` aplica igual a unitarios).

## Checklist

- [ ] Corridos por el script del proyecto, salida literal pegada (`evidence-and-verification`).
- [ ] Inputs por `setInput`; espera con `whenStable`; effects con `TestBed.tick()`.
- [ ] Cada componente: al menos inputs → DOM, evento → output, y estados de carga/vacío/error.
- [ ] HTTP: `expectOne` + `flush` + `verify`; mapeo de errores cubierto.
- [ ] Guards/resolvers testeados con `RouterTestingHarness` o en contexto de inyección.
- [ ] Componentes compartidos con harness; consumidores no tocan su DOM.
- [ ] Sin esquemas que silencien errores; sin `skip`; sin `setTimeout`.
- [ ] Nada de red real ni datos reales de personas (`test-data-management`).
