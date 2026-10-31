"""Modelo de margen por turno de AportaYa.

Entradas tomadas del repo (todas son supuestos del proyecto, no tarifas contratadas):
  seeders/minimos/04-tarifario.json        COM_ENTREGA 0,3 % de la bolsa bruta, piso Bs 10, techo Bs 50,
                                           IVA e IT incluidos en el precio, deducido al liquidar la entrega
  seeders/minimos/14-proveedores-externos.json
                                           QR_INTEROP 0,35 % por cobro · ACH Bs 2,50 por desembolso ·
                                           WHATSAPP 0,28 / SMS 0,42 / PUSH 0,00 por mensaje
  seeders/minimos/05-impuestos.json        IVA 13 % · IT 3 % (ambos por dentro del precio)
Dinero en Decimal con escala 2 (regla 91.1.1). Salida: tabla de margen por turno.
"""
from decimal import Decimal, ROUND_HALF_UP

D = Decimal
CENT = D("0.01")

QR = D("0.0035")          # costo de cobrar cada aporte por QR (proporcional)
ACH = D("2.50")           # costo fijo de un desembolso interbancario
MSG_WHATSAPP = D("0.28")  # por mensaje
MSG_PUSH = D("0.00")
MENSAJES_POR_PERIODO_Y_CUPO = 3   # recordatorio, confirmación, aviso de entrega (supuesto)
IMPUESTOS = D("0.16")     # IVA 13 % + IT 3 %, ambos sobre el precio bruto

ESCENARIOS = [  # (cupos, aporte mensual en Bs)
    (6, D("200")), (6, D("500")), (10, D("500")), (12, D("1000")), (20, D("1000")),
]
TARIFAS = [  # (porcentaje, piso, techo o None)
    (D("0.003"), D("10"), D("50")),     # vigente en el seed
    (D("0.01"), D("10"), None),
    (D("0.015"), D("10"), None),
    (D("0.02"), D("10"), None),
    (D("0.025"), D("10"), None),
]


def q(x: Decimal) -> Decimal:
    return x.quantize(CENT, rounding=ROUND_HALF_UP)


def comision(bolsa: Decimal, pct: Decimal, piso: Decimal, techo) -> Decimal:
    c = bolsa * pct
    c = max(c, piso)
    if techo is not None:
        c = min(c, techo)
    return q(c)


def costo_turno(cupos: int, bolsa: Decimal, msg: Decimal) -> Decimal:
    cobro_qr = bolsa * QR                         # N aportes cobrados por QR = bolsa × 0,35 %
    mensajes = D(cupos) * MENSAJES_POR_PERIODO_Y_CUPO * msg
    return q(cobro_qr + ACH + mensajes)


def main() -> None:
    print("Margen por turno (Bs). Neto = comisión − impuestos 16 % − costo directo.")
    print("Costo directo = QR 0,35 % de la bolsa + ACH 2,50 + mensajes (push gratis / WhatsApp 0,28 × 3 × cupos).")
    print("NO incluye: KYC por participante, infraestructura, soporte, cumplimiento, incobrables. Son el margen que tiene que quedar.\n")
    cab = f"{'cupos':>5} {'aporte':>7} {'bolsa':>8} | {'tarifa':>7} {'comisión':>9} {'neto imp.':>9} | {'costo push':>10} {'margen':>8} | {'costo WA':>9} {'margen':>8} | {'% del aporte/ciclo':>18}"
    print(cab)
    print("-" * len(cab))
    for cupos, aporte in ESCENARIOS:
        bolsa = D(cupos) * aporte
        for pct, piso, techo in TARIFAS:
            com = comision(bolsa, pct, piso, techo)
            neto = q(com * (1 - IMPUESTOS))
            c_push = costo_turno(cupos, bolsa, MSG_PUSH)
            c_wa = costo_turno(cupos, bolsa, MSG_WHATSAPP)
            # Lo que un participante paga una vez en el ciclo, sobre todo lo que aportó en el ciclo (= bolsa)
            pct_ciclo = q(com / bolsa * 100)
            etiqueta = f"{pct*100:.2f}%" + ("" if techo is None else f"≤{techo}")
            print(f"{cupos:>5} {aporte:>7} {bolsa:>8} | {etiqueta:>7} {com:>9} {neto:>9} | {c_push:>10} {q(neto - c_push):>8} | {c_wa:>9} {q(neto - c_wa):>8} | {pct_ciclo:>17}%")
        print()
    print("Punto de equilibrio del porcentaje (sin piso ni techo, push gratis): p tal que p·B·0,84 = costoQR·B + 2,50")
    print("  Dos supuestos de QR: 0,35 % (seed) y 0 % (QR interoperable sin comisión al comercio, según fuentes públicas 2026).")
    for cupos, aporte in ESCENARIOS:
        bolsa = D(cupos) * aporte
        p_seed = (QR * bolsa + ACH) / (bolsa * (1 - IMPUESTOS))
        p_cero = ACH / (bolsa * (1 - IMPUESTOS))
        print(f"  {cupos:>2} cupos × Bs {aporte:>5} (bolsa {bolsa:>6}): p_equilibrio = {q(p_seed*100)} % (QR 0,35 %) · {q(p_cero*100)} % (QR 0 %)")

    print("\nIngreso de la plataforma por grupo y ciclo completo (N turnos), neto de impuestos, Bs:")
    for cupos, aporte in ESCENARIOS:
        bolsa = D(cupos) * aporte
        fila = []
        for pct, piso, techo in TARIFAS:
            com = comision(bolsa, pct, piso, techo)
            fila.append(f"{q(com * (1 - IMPUESTOS) * cupos):>9}")
        print(f"  {cupos:>2} × Bs {aporte:>5}: " + " ".join(fila) + "   (tarifas: " + ", ".join(f"{p*100:.1f}%" for p, _, _ in TARIFAS) + ")")


if __name__ == "__main__":
    main()
