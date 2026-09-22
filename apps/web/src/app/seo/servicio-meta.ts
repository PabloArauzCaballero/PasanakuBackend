import { DOCUMENT } from '@angular/common'
import { Injectable, inject } from '@angular/core'
import { Meta, Title } from '@angular/platform-browser'
import { NavigationEnd, Router } from '@angular/router'
import { filter } from 'rxjs'
import type { JsonLd } from './json-ld'
import { sinTiposProhibidos } from './json-ld'
import { RUTAS_NO_INDEXABLES } from '../geo/robots'

const BASE = 'https://aportaya.bo'
const ID_SCRIPT_JSONLD = 'ap-json-ld'
const ID_CANONICAL = 'ap-canonical'
const ID_ALTERNATE_MARKDOWN = 'ap-alternate-markdown'

/**
 * El mismo patrón que `scripts/contenido.mjs` usa para escribir el espejo: `/` → `/index.md`,
 * cualquier otra ruta → `/<ruta-sin-la-barra-inicial>.md`. Es el valor por omisión que
 * `ServicioMeta` emite como `<link rel="alternate">`; `seo.alternateMarkdown` lo pisa cuando
 * una página necesita otra cosa (micro-PR de F11, no de este archivo).
 */
export function espejoMarkdownDe(ruta: string): string {
  return ruta === '/' ? '/index.md' : `/${ruta.replace(/^\//, '')}.md`
}

/**
 * Metadatos de una ruta indexable. F11 (GEO) agrega `alternateMarkdown` acá — es el
 * único campo que le pertenece; el resto es de F10 y no se edita desde otro carril
 * (planes/18, ficha F10 «Dónde se rompe»).
 */
export interface MetaDeRuta {
  /** Se antepone a "· AportaYa" salvo que `tituloCompleto` sea true. */
  titulo: string
  tituloCompleto?: boolean
  descripcion: string
  /** Uno o más bloques JSON-LD. Nunca `Review`, `AggregateRating` ni `FinancialService`. */
  jsonLd?: JsonLd[]
  /** Fecha AAAA-MM-DD de la última edición del contenido, si la ruta la tiene. */
  actualizado?: string
  /** F11: el equivalente en Markdown de la misma página, para el espejo /**.md. */
  alternateMarkdown?: string
}

/**
 * Pone `<title>`, `<meta name="description">`, `<link rel="canonical">` y el JSON-LD de
 * la página activa, a partir del `data.seo` de la ruta (`app.routes.ts`).
 *
 * Regla que no se negocia: si la ruta está en `RUTAS_NO_INDEXABLES` (ADR-042,
 * `geo/robots.ts`), se fuerza `noindex,nofollow` sin importar lo que diga `data.seo` —
 * cuando el SEO y la protección de datos se pelean, gana la protección.
 */
@Injectable({ providedIn: 'root' })
export class ServicioMeta {
  private readonly router = inject(Router)
  private readonly title = inject(Title)
  private readonly meta = inject(Meta)
  private readonly documento = inject(DOCUMENT)

  observar(): void {
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(() => {
      let ruta = this.router.routerState.root
      while (ruta.firstChild) ruta = ruta.firstChild
      const seo = ruta.snapshot.data['seo'] as MetaDeRuta | undefined
      this.aplicar(this.router.url.split('?')[0] ?? '/', seo)
    })
  }

  private aplicar(rutaActual: string, seo: MetaDeRuta | undefined): void {
    const noIndexable = RUTAS_NO_INDEXABLES.some((r) => rutaActual === r || rutaActual.startsWith(r))

    if (seo) {
      this.title.setTitle(seo.tituloCompleto ? seo.titulo : `${seo.titulo} · AportaYa`)
      this.meta.updateTag({ name: 'description', content: seo.descripcion })
    }

    this.meta.updateTag({ name: 'robots', content: noIndexable ? 'noindex, nofollow' : 'index, follow' })

    this.actualizarCanonical(`${BASE}${rutaActual === '/' ? '' : rutaActual}`)
    this.actualizarAlternateMarkdown(noIndexable ? null : (seo?.alternateMarkdown ?? espejoMarkdownDe(rutaActual)))

    if (seo?.jsonLd?.length && !noIndexable) {
      const bloques = seo.jsonLd.filter((j) => sinTiposProhibidos(j))
      this.actualizarJsonLd(bloques)
    } else {
      this.actualizarJsonLd([])
    }
  }

  private actualizarAlternateMarkdown(rutaMarkdown: string | null): void {
    const existente = this.documento.getElementById(ID_ALTERNATE_MARKDOWN) as HTMLLinkElement | null
    if (!rutaMarkdown) {
      existente?.remove()
      return
    }
    let enlace = existente
    if (!enlace) {
      enlace = this.documento.createElement('link')
      enlace.setAttribute('id', ID_ALTERNATE_MARKDOWN)
      enlace.setAttribute('rel', 'alternate')
      enlace.setAttribute('type', 'text/markdown')
      this.documento.head.appendChild(enlace)
    }
    enlace.setAttribute('href', `${BASE}${rutaMarkdown}`)
  }

  private actualizarCanonical(href: string): void {
    let enlace = this.documento.getElementById(ID_CANONICAL) as HTMLLinkElement | null
    if (!enlace) {
      enlace = this.documento.createElement('link')
      enlace.setAttribute('id', ID_CANONICAL)
      enlace.setAttribute('rel', 'canonical')
      this.documento.head.appendChild(enlace)
    }
    enlace.setAttribute('href', href)
  }

  private actualizarJsonLd(bloques: JsonLd[]): void {
    const existente = this.documento.getElementById(ID_SCRIPT_JSONLD)
    if (existente) existente.remove()
    if (bloques.length === 0) return
    const script = this.documento.createElement('script')
    script.setAttribute('id', ID_SCRIPT_JSONLD)
    script.setAttribute('type', 'application/ld+json')
    script.text = JSON.stringify(bloques.length === 1 ? bloques[0] : bloques)
    this.documento.head.appendChild(script)
  }
}
