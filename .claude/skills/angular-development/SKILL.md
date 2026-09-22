---
name: angular-development
description: Estándar de Angular moderno (v21) — componentes standalone, control flow `@if`/`@for`, `input()`/`output()`/`model()`, DI con `inject()`, zoneless + OnPush, rutas lazy con guards, resolvers e interceptors funcionales, CLI y estructura por feature. Usar al crear o revisar cualquier componente, servicio, ruta, guard o interceptor, al migrar código con NgModules, `@Input` o `*ngIf`, o cuando una vista no se actualiza.
---

# Desarrollo Angular

Aplica a todo código de la web de la casa: Angular 21 standalone + signals + SSR, CSS
puro. Angular cambió mucho entre v17 y v21: **no escribas de memoria**. Verificá la API
contra angular.dev *en la versión instalada* (`package.json`); la doc vigente puede
describir una versión posterior a la del proyecto (ver `anti-hallucination-guard`).
Antes de crear nada, muestreá 2–3 vecinos del mismo tipo (`native-code-patterns`).

## 1. Componentes

1. Standalone siempre; sin NgModules nuevos. Lo que el template usa va en `imports`.
2. Generá con el CLI (`ng generate component|service|guard|…`), no copiando carpetas: el
   CLI aplica la convención de nombres y archivos configurada en el proyecto.
3. Archivos de un componente con el mismo nombre base (`user-profile.ts`, `.html`,
   `.css`). La convención de sufijos la fija el proyecto; seguí la existente.
4. `changeDetection: ChangeDetectionStrategy.OnPush` en todo componente (§4).
5. Miembros usados solo por el template: `protected`. Inputs, outputs y queries: `readonly`.
6. Bindings al host con la propiedad `host` del decorador, no `@HostBinding`/`@HostListener`.
7. Queries con signals: `viewChild()`, `viewChild.required()`, `contentChild()`.

```ts
@Component({
  selector: 'app-member-card',
  imports: [DatePipe],
  templateUrl: './member-card.html',
  styleUrl: './member-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-selected]': 'selected()' },
})
export class MemberCard {
  readonly member = input.required<MemberSummary>();
  readonly selected = model(false);              // two-way: [(selected)]="sig"
  readonly opened = output<string>();
  protected readonly initials = computed(() => toInitials(this.member().fullName));
}
```

| Necesitás | API |
|---|---|
| Dato que baja del padre | `input()` / `input.required<T>()` |
| Evento que sube | `output<T>()` |
| Valor que el hijo también modifica | `model()` — el padre bindea la *instancia* del signal: `[(value)]="volume"` |

❌ `@Input() member!: Member` + `ngOnChanges` para derivar → ✅ `input.required()` + `computed()`.

## 2. Templates y control flow

- `@if / @else`, `@for`, `@switch`, `@let`. Nada de `*ngIf`/`*ngFor`/`ngSwitch` en código nuevo.
- `@for` exige `track`: usá un identificador estable (`track item.id`), nunca `$index`
  en listas que se reordenan o filtran. `@empty` resuelve el estado vacío
  (ver `frontend-ux-states`).
- El template no calcula: sin llamadas a métodos que computan, sin lógica de negocio,
  sin ternarios anidados. Derivá con `computed()` y mostrá el resultado.
- `@defer` para lo pesado que no hace falta en el primer render (con `@placeholder` que
  reserve espacio). Bajo SSR, ver `angular-ssr-hydration`.

```html
@for (slot of slots(); track slot.id) {
  <app-slot-row [slot]="slot" (picked)="pick($event)" />
} @empty {
  <app-empty-state title="Sin horarios disponibles" />
}
```

## 3. Inyección de dependencias

- `inject()` en inicializadores de campo o constructor, no parámetros de constructor.
- `inject()` solo funciona en contexto de inyección (construcción, factories, guards,
  resolvers, interceptors funcionales). Fuera de él, capturá la dependencia antes o usá
  `runInInjectionContext`.
- Servicios singleton con `providedIn: 'root'`; estado acotado a una feature, en
  `providers` de la ruta. No expongas clientes HTTP crudos a los componentes: pasá por
  la capa de datos (`frontend-data-access`).

## 4. Change detection: zoneless + OnPush

En v21 zoneless es el **default**: sin ZoneJS, Angular solo re-renderiza cuando se le
notifica. Notifican: actualizar un signal leído en el template, un listener del template
o del host, `ComponentRef.setInput`, `markForCheck()` (lo llama `AsyncPipe`).

- Todo estado que el template lee vive en signals. Mutar un campo plano desde un
  `setTimeout`, una promesa o un callback de librería **no** refresca la vista.
- Una vista que "no se actualiza" casi siempre es estado fuera de signals: convertí a
  signal; `markForCheck()` es el último recurso, no el parche por defecto.
- Verificá que el proyecto no tenga `provideZoneChangeDetection` pisando el default y
  que `zone.js` no siga en los polyfills de `build` y `test`.

## 5. Rutas, guards y resolvers

```ts
export const routes: Routes = [
  {
    path: 'calendario',
    canActivate: [authGuard],
    loadChildren: () => import('./calendario/calendario.routes'),
  },
];

export const authGuard: CanActivateFn = () => {
  const session = inject(SessionStore);
  return session.isAuthenticated() || inject(Router).createUrlTree(['/login']);
};
```

- Lazy por defecto: `loadComponent` / `loadChildren` con `import()` dinámico.
- Guards y resolvers **funcionales** (`CanActivateFn`, `ResolveFn`), no clases.
- Un guard es UX, no seguridad: el backend autoriza siempre (`authz-access-control`).
- Parámetros de ruta como inputs con `withComponentInputBinding()` si el proyecto lo usa.
- Transiciones de ruta: `withViewTransitions()` (ver `frontend-motion`).

## 6. HTTP e interceptors

```ts
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const tenantId = inject(TenantStore).activeId();
  return next(tenantId ? req.clone({ setHeaders: { 'X-Tenant-Id': tenantId } }) : req);
};
// app.config.ts
provideHttpClient(withFetch(), withInterceptors([tenantInterceptor, errorInterceptor]))
```

Interceptors funcionales (`HttpInterceptorFn` + `withInterceptors`). El orden del array
es el orden de ejecución. El nombre del header es ilustrativo: definilo en el CLAUDE.md
del proyecto. Cliente tipado y mapeo de errores: `frontend-data-access`.

## 7. Estructura

- Carpetas por **feature/dominio**, no por tipo técnico (`calendario/`, no `components/` +
  `services/` globales). Lo compartido va a una zona común explícita, organizada según
  `atomic-design-components`.
- Una feature no importa de las entrañas de otra: cruza por una API pública.
- Un concepto por archivo; tests junto al código (`angular-testing`).

## 8. Anti-patrones

- `subscribe` dentro de `subscribe` → operadores (`switchMap`, `combineLatest`) o signals.
- Suscripciones manuales sin teardown → `toSignal`, `AsyncPipe` o `takeUntilDestroyed()`.
- `any`, `as` y `!` para callar al compilador (ver `typescript-standards`).
- `effect()` para copiar un signal a otro → `computed`/`linkedSignal` (`angular-signals-state`).
- `window`/`document`/`localStorage` directos en constructor u `ngOnInit` (rompe SSR).
- `ElementRef.nativeElement.innerHTML = …` (XSS — ver `frontend-security`).
- Hex y px literales en el CSS del componente en vez de tokens (`frontend-design-system`).
- NgModules, `@Input()`/`@Output()`, `*ngIf`, guards de clase, `@angular/animations`
  en código nuevo.

## Checklist

- [ ] Standalone, OnPush, generado por CLI, nombres según la convención del proyecto.
- [ ] `input()`/`output()`/`model()`; derivaciones en `computed()`, no en el template.
- [ ] Control flow nuevo; todo `@for` con `track` estable y `@empty` donde aplique.
- [ ] Todo estado leído por el template está en signals (compatible con zoneless).
- [ ] Rutas lazy; guards/resolvers/interceptors funcionales.
- [ ] Sin suscripciones colgadas ni `subscribe` anidados; sin `any`.
- [ ] Sin acceso directo a APIs de navegador fuera de `afterNextRender`.
- [ ] Cada API usada se verificó contra la doc de la versión instalada.
