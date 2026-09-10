import { ChangeDetectionStrategy, Component } from '@angular/core'
import { CatalogoAtomos } from './catalogo-atomos'
import { CatalogoMoleculas } from './catalogo-moleculas'
import { CatalogoOrganismos } from './catalogo-organismos'

/**
 * El catálogo vivo de `@aportaya/ui`. Lo monta `/catalogo` en el sitio (sin indexar) y
 * lo recorre la prueba de accesibilidad: **cada pieza pasa por axe en claro y oscuro**.
 */
@Component({
  selector: 'ap-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CatalogoAtomos, CatalogoMoleculas, CatalogoOrganismos],
  template: `
    <nav aria-label="Secciones del catálogo">
      <a href="#botones">Botones</a><a href="#campos">Campos</a><a href="#seleccion">Selección</a><a href="#indicadores">Indicadores</a>
      <a href="#tarjetas">Tarjetas</a><a href="#listas">Listas</a><a href="#avisos">Avisos</a><a href="#negocio">Del negocio</a><a href="#organismos">Organismos</a>
    </nav>
    <ap-catalogo-atomos />
    <ap-catalogo-moleculas />
    <ap-catalogo-organismos />
  `,
  styles: `
    :host { display: block; }
    nav { display: flex; flex-wrap: wrap; gap: var(--s2) var(--s4); padding: var(--s3) 0; }
    a { color: var(--brand-texto); font-weight: 600; min-height: var(--area-tactil); display: inline-flex; align-items: center; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); border-radius: var(--r-sm); }
  `,
})
export class Catalogo {}
