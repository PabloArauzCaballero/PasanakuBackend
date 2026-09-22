---
titulo: Fundamentos
nivel: 0
tags: [diseno, tokens]
---

# Nivel 0 · Fundamentos (tokens)

Página visual: `Fundamentos.html`. Nunca uses un hex suelto: todo sale de un token.

## Verde Pasanaku (primario)
`--g900 #0C2C1D` · `--g800 #123A26` · `--g700 #164A30` · **`--g600 #1C5A3A` (base/logo)** · `--g500 #237349` · `--g400 #3C9366` · `--g300 #7CBE9C` · `--g200 #BCDFCC` · `--g100 #E7F2EB`

## Naranja Aporte (acción)
`--o700 #BC6217` · `--o600 #D6741C` · **`--o500 #E5852B` (CTA)** · `--o400 #EF9E4E` · `--o300 #F6BE85` · `--o200 #FBDBB8` · `--o100 #FDF0DF` · texto sobre naranja `#3A1E02`

## Neutros
Tinta `#10231A` · Pizarra `#38473F` · Musgo `#6C7B72` · Línea `#DCE4DE` · Borde campo `#C9D4CD` · Nube `#F3F6F2` · Crema `#F6F4EC` · Blanco `#FFFFFF`

## Semánticos
Éxito `#1F9D57` (bg `#E7F5EC`) · Aviso `#F0B429` (bg `#FEF4DA`) · Error `#D64545` (bg `#FBECEC`) · Info `#2E7FB8` (bg `#E7F1F8`)

## Escalas
- Espaciado (base 4): 4 · 8 · 12 · 16 · 24 · 32 · 48
- Radio: 8 (chip) · 12 (campo/botón) · 16 (tarjeta) · 24 (bottom sheet) · 999 (pill)
- Tipografía: **Bricolage Grotesque** (display/cifras) + **Instrument Sans** (cuerpo), las dos OFL y **empaquetadas** en la app y en el sitio —nunca por CDN: una billetera tiene que ser legible sin señal. Dinero con `tabular-nums` → `Bs 1.240,00`
- Escala tipográfica: un rol por uso (`--t-cifra-grande`, `--t-titulo-1`, `--t-boton`, `--t-cuerpo`, `--t-campo-etiqueta`, `--t-ayuda`…), no un tamaño suelto. De acá se genera la clase `Tipo` de Flutter: los dos mundos miden la misma letra

## Colores de turno
Para distinguir **personas** dentro de una rueda de pasanaku, y solo para eso: nunca un botón ni un fondo. Salen de la misma paleta, en un orden que evita que dos vecinos compartan tono. El color nunca es la única señal — el turno propio lleva además un punto.

Siguiente: [[Atomos]]
