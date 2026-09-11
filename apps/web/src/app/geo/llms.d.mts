export declare const RUTAS_EXCLUIDAS_LLMS: readonly string[]
export declare function rutaExcluidaDeLlms(ruta: string): boolean

export interface PaginaLlms {
  ruta: string
  titulo: string
  bajada: string
  indexable: boolean
  actualizado: string
}

export interface PaginaLlmsFull extends PaginaLlms {
  markdown: string
}

export declare function generarLlmsTxt(paginas: PaginaLlms[], base: string): string
export declare function generarLlmsFullTxt(paginas: PaginaLlmsFull[], base: string): string
