---
name: angular-forms
description: Formularios en Angular — reactive forms tipados con `nonNullable`, validadores sync/async y `updateOn`, errores por campo y de servidor mapeados al control con `setErrors`, datos preservados ante fallo, formularios dinámicos desde un esquema, accesibilidad del markup y estado de Signal Forms (experimental en v21). Usar al crear o revisar cualquier formulario, al mapear un 422 de la API a la UI, al construir un formulario desde una definición o al decidir entre reactive forms y Signal Forms. Cuándo validar y cómo se siente el formulario es `frontend-forms-ux`.
---

# Formularios en Angular

Cubre el *cómo* técnico. El *qué* (diseño, microcopy, cuándo validar, feedback) está en
`frontend-forms-ux` y `frontend-ux-states`; la accesibilidad exigible, en
`frontend-accessibility`. Verificá las APIs contra angular.dev en la versión instalada.

## 1. Qué API usar

| Situación | Usá |
|---|---|
| Cualquier formulario de producción en v21 | Reactive forms **tipados** |
| Formulario trivial sin validación (un buscador) | `[(ngModel)]` o un signal con `(input)`; no armes un `FormGroup` para un campo |
| Signal Forms (`@angular/forms/signals`) | En v21 la doc oficial las marca **experimentales** ("the API may change"). No en producción sin decisión registrada (`technical-docs-and-adr`). En versiones posteriores figuran como estables: verificá tu versión |

Bajo zoneless, si mutás el modelo del formulario directo desde código (no desde el
template), la vista puede no refrescar: la doc indica `valueChanges` →
`ChangeDetectorRef.markForCheck()`; mejor, derivá lo que el template muestra desde
`toSignal(form.valueChanges)` o `toSignal(form.statusChanges)`.

## 2. Reactive forms tipados

```ts
private readonly fb = inject(NonNullableFormBuilder);

readonly form = this.fb.group({
  email: ['', [Validators.required, Validators.email]],
  birthDate: this.fb.control<string | null>(null, Validators.required),
  phone: ['', { validators: [Validators.required], asyncValidators: [this.phoneAvailable], updateOn: 'blur' }],
});
```

- `NonNullableFormBuilder` (o `{ nonNullable: true }`): `reset()` vuelve al valor inicial
  y el tipo no incluye `null` por defecto. Declarar `null` es una decisión explícita.
- Enviá `form.getRawValue()` (incluye deshabilitados), no `form.value` (parcial).
- Un `FormGroup` refleja el DTO de envío; el tipo del grupo sale del DTO
  (`typescript-standards`). Sin `any` ni `UntypedFormGroup` en código nuevo.
- Async validators: solo tras pasar los sync; `updateOn: 'blur'` para los que pegan a la
  API, así no se dispara una llamada por tecla. Devuelven `Promise|Observable` de
  `ValidationErrors | null`.
- Validadores propios como funciones puras `ValidatorFn` nombradas por la regla
  (`dateNotInFuture`, `ciFormat`), reutilizables y testeables sin TestBed.
- Validación cruzada (fecha fin ≥ inicio) va en el **grupo**, no en un control.
- El cliente valida para la UX; **la API valida siempre** (`backend-development`).

## 3. Errores por campo

Cada control muestra su error asociado por `aria-describedby`, y solo cuando el campo
fue tocado o el formulario se intentó enviar.

```html
<label for="email">Correo</label>
<input id="email" type="email" formControlName="email" autocomplete="email"
       [attr.aria-invalid]="showError('email')" [attr.aria-describedby]="showError('email') ? 'email-error' : null" />
@if (showError('email')) {
  <p id="email-error" class="field-error">{{ errorMessage('email') }}</p>
}
```

- `showError(name)` = `control.invalid && (control.touched || submitted())`. Al enviar
  inválido: `form.markAllAsTouched()` y llevar el foco al primer campo con error.
- Un mapa `código de error → mensaje` por proyecto (`required`, `email`, `minlength`,
  los propios). El texto es responsabilidad de `frontend-forms-ux`; acá se garantiza que
  cada error tenga texto y que el mapa esté en el idioma del usuario (`frontend-i18n-l10n`).
- Nunca `<input>` sin `<label for>`; `autocomplete` correcto; el error nunca es solo color.

## 4. Errores de servidor

La API responde validación con un cuerpo estructurado (`error-handling-contract`): un
error por campo con su `path`. Mapealos al control, no a un cartel genérico.

```ts
submit(): void {
  if (this.form.invalid) { this.form.markAllAsTouched(); return; }
  this.submitting.set(true);
  this.api.createMember(this.form.getRawValue()).subscribe({
    next: () => this.router.navigate(['..']),
    error: (err: ApiError) => {
      this.submitting.set(false);            // el formulario conserva lo escrito
      for (const fe of err.fieldErrors ?? []) {
        this.form.get(fe.path)?.setErrors({ server: fe.message });
      }
      this.formError.set(err.fieldErrors?.length ? null : err.message);
    },
  });
}
```

- `setErrors({ server })` se borra en la próxima edición del control (Angular
  revalida); ese es el comportamiento deseado.
- Error sin campo (conflicto, permiso, red) → mensaje a nivel de formulario, con `role="alert"`.
- Ante cualquier fallo el usuario **no pierde lo que escribió**. Deshabilitá el botón
  mientras `submitting()` para evitar doble envío (`concurrency-and-locking` del lado API).
- `409`/`412` por versión de fila: ofrecer recargar y mostrar qué cambió, no pisar.

## 5. Formularios dinámicos desde un esquema

Para formularios estandarizados (plantillas financieras, cuestionarios, formularios por
organización) el formulario se construye a partir de una definición, no se codifica uno
por uno.

```ts
type FieldDef = { key: string; kind: 'text' | 'number' | 'date' | 'select'; required?: boolean; options?: OptionRef };

buildGroup(defs: readonly FieldDef[]): FormGroup {
  const controls: Record<string, FormControl> = {};
  for (const d of defs) {
    controls[d.key] = this.fb.control(defaultFor(d.kind), d.required ? [Validators.required] : []);
  }
  return this.fb.group(controls);          // FormRecord si las claves son abiertas
}
```

- Un componente por `kind` (`@switch (def.kind)`), renderizado desde un catálogo cerrado;
  un `kind` desconocido es error de datos, no se ignora en silencio.
- Las opciones de un `select` vienen de catálogos/value sets, nunca hardcodeadas
  (`terminology-value-sets`).
- La definición del esquema y sus versiones viven en el backend; el cliente no decide
  reglas. Un formulario respondido guarda con qué versión del esquema se respondió.

## 6. Anti-patrones

- Template-driven con `ngModel` en formularios con validación o varios campos.
- `form.value` con `as` para callar tipos; controles `any`.
- Validador async sin `updateOn: 'blur'` que pega a la API en cada tecla.
- Error de servidor solo en un toast, sin marcar el campo.
- Limpiar el formulario ante error; permitir doble submit.
- Mostrar errores antes de que el usuario toque el campo.
- Signal Forms en producción "porque ya está en la doc" sin verificar estabilidad en la
  versión instalada.

## Checklist

- [ ] Reactive forms tipados con `nonNullable`; envío con `getRawValue()`.
- [ ] Validadores nombrados por regla; cruzados en el grupo; async con `updateOn: 'blur'`.
- [ ] Cada campo: `label for`, `aria-invalid`, `aria-describedby` al error, `autocomplete`.
- [ ] Errores visibles solo tras tocar/enviar; `markAllAsTouched` + foco al primer error.
- [ ] Errores de servidor mapeados por `path` con `setErrors`; genéricos con `role="alert"`.
- [ ] Datos preservados ante fallo; sin doble submit.
- [ ] Formularios dinámicos: catálogo cerrado de `kind`, opciones desde value sets, versión del esquema guardada.
- [ ] Bajo zoneless, lo que el template muestra del formulario deriva de signals.
