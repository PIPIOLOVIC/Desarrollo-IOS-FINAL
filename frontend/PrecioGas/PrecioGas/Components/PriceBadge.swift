import SwiftUI

enum NivelPrecio {
    case barato
    case promedio
    case caro

    var colorFondo: Color {
        switch self {
        case .barato: AppTheme.principal
        case .promedio: AppTheme.secundario
        case .caro: AppTheme.caro
        }
    }

    var colorTexto: Color {
        switch self {
        case .barato, .promedio: AppTheme.fondo
        case .caro: AppTheme.texto
        }
    }

    static func evaluar(precio: Double, referencia: Double?, tolerancia: Double = 0.05) -> NivelPrecio {
        guard let referencia else { return .promedio }
        let diferencia = precio - referencia
        if diferencia < -tolerancia { return .barato }
        if diferencia > tolerancia { return .caro }
        return .promedio
    }
}

struct PriceBadge: View {
    let texto: String
    let nivel: NivelPrecio
    var compacto: Bool = false

    init(precio: Double, nivel: NivelPrecio) {
        self.texto = String(format: "$%.2f", precio)
        self.nivel = nivel
        self.compacto = false
    }

    init(texto: String, nivel: NivelPrecio, compacto: Bool = true) {
        self.texto = texto
        self.nivel = nivel
        self.compacto = compacto
    }

    var body: some View {
        Text(texto)
            .font(compacto ? .footnote.weight(.bold) : .headline.weight(.bold))
            .monospacedDigit()
            .foregroundStyle(nivel.colorTexto)
            .lineLimit(1)
            .padding(.horizontal, compacto ? 12 : 16)
            .padding(.vertical, compacto ? 6 : 8)
            .background(nivel.colorFondo, in: Capsule())
    }
}

#Preview {
    VStack(spacing: 16) {
        HStack(spacing: 12) {
            PriceBadge(precio: 23.50, nivel: .barato)
            PriceBadge(precio: 23.50, nivel: .promedio)
            PriceBadge(precio: 23.50, nivel: .caro)
        }
        HStack(spacing: 8) {
            PriceBadge(texto: "-$0.15 más barata", nivel: .barato)
            PriceBadge(texto: "+$0.19 más cara", nivel: .caro)
        }
    }
    .padding()
}
