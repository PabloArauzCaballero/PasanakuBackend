import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { TablaDeDatos, type Columna, type EstadoColeccion } from './tabla-de-datos'

type Fila = { id: string; grupo: string; cupos: number }

function montar() {
  return TestBed.createComponent(TablaDeDatos<Fila>)
}

describe('ap-tabla-de-datos', () => {
  it('ordena con aria-sort y selecciona todas', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = montar()
    fixture.componentRef.setInput('titulo', 'Grupos')
    fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo', ordenable: true }, { clave: 'cupos', titulo: 'Cupos', numerica: true }])
    fixture.componentRef.setInput('filas', [{ id: 'a', grupo: 'A', cupos: 1 }, { id: 'b', grupo: 'B', cupos: 2 }])
    fixture.componentRef.setInput('seleccionable', true)
    fixture.detectChanges()
    const th = fixture.nativeElement.querySelector('th[aria-sort]') as HTMLElement
    expect(th.getAttribute('aria-sort')).toBe('none')
    th.querySelector('button')!.click()
    await fixture.whenStable()
    expect(th.getAttribute('aria-sort')).toBe('ascending')
    th.querySelector('button')!.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.orden()).toEqual({ clave: 'grupo', sentido: 'desc' })
    const todas = fixture.nativeElement.querySelector('thead input[type="checkbox"]') as HTMLInputElement
    todas.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegidas()).toEqual(['a', 'b'])
  })

  describe('identidad de fila (H2.S1.M2)', () => {
    it('reordenar la colección no pierde la selección ni el foco de una fila', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      fixture.componentRef.setInput('filas', [
        { id: 'a', grupo: 'A', cupos: 1 },
        { id: 'b', grupo: 'B', cupos: 2 },
        { id: 'c', grupo: 'C', cupos: 3 },
      ])
      fixture.componentRef.setInput('seleccionable', true)
      fixture.detectChanges()
      fixture.componentInstance.marcar('b', true)
      expect(fixture.componentInstance.elegidas()).toEqual(['b'])

      // Mismas filas, orden invertido en el arreglo de entrada: la identidad ('b') sigue
      // marcando la misma fila, no la que ahora ocupa la posición 1.
      fixture.componentRef.setInput('filas', [
        { id: 'c', grupo: 'C', cupos: 3 },
        { id: 'b', grupo: 'B', cupos: 2 },
        { id: 'a', grupo: 'A', cupos: 1 },
      ])
      fixture.detectChanges()
      expect(fixture.componentInstance.elegidas()).toEqual(['b'])
      const filaMarcada = fixture.nativeElement.querySelector('tr.marcada td:nth-child(2)') as HTMLElement
      expect(filaMarcada.textContent?.trim()).toBe('B')
    })

    it('la identidad por omisión usa el campo `id`, nunca la posición en el arreglo', () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      fixture.componentRef.setInput('filas', [{ id: 'x9', grupo: 'A', cupos: 1 }])
      fixture.detectChanges()
      expect(fixture.componentInstance.identidad()({ id: 'x9', grupo: 'A', cupos: 1 })).toBe('x9')
    })
  })

  describe('estado de la colección (H2.S1.M4 — doble en tres niveles, Q-J1)', () => {
    function exhaustivo(e: EstadoColeccion<Fila>): string {
      switch (e.tipo) {
        case 'cargando':
          return 'cargando'
        case 'lista':
          return e.obsoleta ? 'lista-obsoleta' : 'lista'
        case 'error':
          return `error-${e.motivo}`
        default: {
          // Si `EstadoColeccion` gana una variante y este `switch` no la cubre, esto no compila.
          const _exhaustivo: never = e
          return _exhaustivo
        }
      }
    }

    it('nivel correcto: una lista con filas válidas se muestra', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      const estado: EstadoColeccion<Fila> = { tipo: 'lista', filas: [{ id: 'a', grupo: 'A', cupos: 1 }] }
      fixture.componentRef.setInput('estado', estado)
      fixture.detectChanges()
      expect(exhaustivo(estado)).toBe('lista')
      expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
      expect(fixture.nativeElement.querySelector('.mensaje-estado')).toBeNull()
    })

    it('nivel límite: colección vacía muestra el texto vacío, no una tabla en blanco muda', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      const estado: EstadoColeccion<Fila> = { tipo: 'lista', filas: [] }
      fixture.componentRef.setInput('estado', estado)
      fixture.detectChanges()
      expect(exhaustivo(estado)).toBe('lista')
      expect(fixture.nativeElement.querySelector('.mensaje-estado')?.textContent).toContain('Sin resultados')
    })

    it('nivel límite: una sola fila se muestra igual que una lista de muchas', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      fixture.componentRef.setInput('estado', { tipo: 'lista', filas: [{ id: 'a', grupo: 'A', cupos: 1 }] } satisfies EstadoColeccion<Fila>)
      fixture.detectChanges()
      expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    })

    it('nivel límite: una respuesta marcada obsoleta se sigue mostrando, con aviso, sin vaciarse', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      const estado: EstadoColeccion<Fila> = { tipo: 'lista', filas: [{ id: 'a', grupo: 'A', cupos: 1 }], obsoleta: true }
      fixture.componentRef.setInput('estado', estado)
      fixture.detectChanges()
      expect(exhaustivo(estado)).toBe('lista-obsoleta')
      expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
      expect(fixture.nativeElement.querySelector('.mensaje-obsoleta')?.textContent).toContain('Actualizando')
    })

    it('nivel inválido: un error con identificador de petición se anuncia y oculta la tabla', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      const estado: EstadoColeccion<Fila> = { tipo: 'error', idPeticion: 'req-42', motivo: 'desconocido', mensaje: 'No se pudo cargar la lista.' }
      fixture.componentRef.setInput('estado', estado)
      fixture.detectChanges()
      expect(exhaustivo(estado)).toBe('error-desconocido')
      const alerta = fixture.nativeElement.querySelector('[role="alert"]')
      expect(alerta?.textContent).toContain('No se pudo cargar la lista.')
      expect(fixture.nativeElement.querySelector('table')).toBeNull()
    })

    it('nivel inválido: sin permiso y no encontrado son motivos distintos y exhaustivos', () => {
      const sinPermiso: EstadoColeccion<Fila> = { tipo: 'error', idPeticion: 'r1', motivo: 'sin-permiso', mensaje: 'x' }
      const noEncontrado: EstadoColeccion<Fila> = { tipo: 'error', idPeticion: 'r2', motivo: 'no-encontrado', mensaje: 'x' }
      expect(exhaustivo(sinPermiso)).toBe('error-sin-permiso')
      expect(exhaustivo(noEncontrado)).toBe('error-no-encontrado')
    })

    it('cargando oculta la tabla y anuncia el estado sin usar solo un ícono', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      fixture.componentRef.setInput('estado', { tipo: 'cargando' } satisfies EstadoColeccion<Fila>)
      fixture.detectChanges()
      expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('Cargando')
      expect(fixture.nativeElement.querySelector('table')).toBeNull()
    })

    it('`estado` es aditivo: sin pasarlo, el comportamiento es exactamente el de antes de la extensión', async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo' }])
      fixture.componentRef.setInput('filas', [{ id: 'a', grupo: 'A', cupos: 1 }])
      fixture.detectChanges()
      expect(fixture.nativeElement.querySelector('.mensaje-estado')).toBeNull()
      expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    })
  })

  describe('mutación de la entrada (H4.S2.M1)', () => {
    it('no muta el arreglo de filas recibido', () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      const filas = [{ id: 'a', grupo: 'A', cupos: 1 }, { id: 'b', grupo: 'B', cupos: 2 }]
      const copia = structuredClone(filas)
      fixture.componentRef.setInput('titulo', 'Grupos')
      fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo', ordenable: true }])
      fixture.componentRef.setInput('filas', filas)
      fixture.componentRef.setInput('seleccionable', true)
      fixture.detectChanges()
      fixture.nativeElement.querySelector('thead input[type="checkbox"]').click()
      fixture.detectChanges()
      fixture.nativeElement.querySelector('th button')?.click()
      fixture.detectChanges()
      expect(filas).toEqual(copia)
    })
  })

  describe('cardinalidad de columnas requeridas (H3.S1.M1)', () => {
    it('montar sin `titulo` ni `columnas` falla de forma visible (input requerido de Angular)', () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
      const fixture = montar()
      expect(() => fixture.detectChanges()).toThrow()
    })
  })

  it('no hay ningún nombre de pantalla dentro del organismo (H3.S1.M3)', async () => {
    const fuente = await import('fs/promises').then((fs) => fs.readFile(new URL('./tabla-de-datos.ts', import.meta.url), 'utf-8'))
    const nombresDePantalla = ['estado-de-estado', 'PantallaDeEstado', 'PantallaDeAnunciantes', 'anunciantes', 'webhooks', 'operacion/estado']
    for (const nombre of nombresDePantalla) {
      expect(fuente.toLowerCase()).not.toContain(nombre.toLowerCase())
    }
  })

  it('el caso negativo de acceso a celda documentado sigue existiendo como función (H2.S1.M3)', () => {
    // Referenciarla mantiene viva la comprobación de tipos de `@ts-expect-error` de acá abajo;
    // no se llama porque su valor de retorno no importa, solo que compile con el error esperado.
    expect(typeof _casoNegativoQueNoCompila).toBe('function')
  })
})

/** Caso negativo documentado (H2.S1.M3): una clave de columna que no existe en `T` no compila. */
function _casoNegativoQueNoCompila() {
  const columnasInvalidas: Columna<Fila>[] = [
    // @ts-expect-error 'noExiste' no es una clave de `Fila`: el acceso a celda es una función pura tipada, no `any`.
    { clave: 'noExiste', titulo: 'Inválida' },
  ]
  return columnasInvalidas
}
