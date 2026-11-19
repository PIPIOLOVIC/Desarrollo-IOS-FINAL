import SwiftUI

struct StationCard: View {
    let nombre: String
    let marca: String
    let distanciaKm: Double
    let precio: Double?
    let nivel: NivelPrecio
    var mostrarChevron: Bool = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                .fill(AppTheme.fondo)
                .overlay(
                    RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                        .stroke(AppTheme.secundario, lineWidth: 1)
                )

            HStack(spacing: AppTheme.Spacing.sm) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(nombre)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.texto)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    Text(String(format: "%@ · %.1f km", marca, distanciaKm))
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.secundario)
                        .lineLimit(1)
                }

                Spacer(minLength: 8)

                if let precio {
                    PriceBadge(precio: precio, nivel: nivel)
                } else {
                    Text("N/D")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.secundario)
                }

                if mostrarChevron {
                    Image(systemName: "chevron.right")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.secundario)
                }
            }
            .padding(.horizontal, AppTheme.Spacing.md)
            .padding(.vertical, 18)
        }
    }
}

#Preview {
    VStack(spacing: 12) {
        StationCard(
            nombre: "Estación Reforma",
            marca: "Independiente",
            distanciaKm: 1.8,
            precio: 23.50,
            nivel: .barato
        )
        StationCard(
            nombre: "Shell Paseo de la Reforma",
            marca: "Shell",
            distanciaKm: 1.4,
            precio: 23.69,
            nivel: .caro,
            mostrarChevron: true
        )
    }
    .padding()
}
