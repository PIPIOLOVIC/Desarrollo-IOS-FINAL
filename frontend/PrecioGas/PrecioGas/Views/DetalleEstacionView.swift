import SwiftUI

struct DetalleEstacionView: View {
    let estacion: EstacionDTO
    let misPrecios: [Combustible: Double]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppTheme.Spacing.md) {
                Text(estacion.nombre)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.texto)

                HStack(spacing: AppTheme.Spacing.md) {
                    ZStack {
                        RoundedRectangle(cornerRadius: AppTheme.Radius.iconBox, style: .continuous)
                            .fill(AppTheme.fondo)
                            .overlay(
                                RoundedRectangle(cornerRadius: AppTheme.Radius.iconBox, style: .continuous)
                                    .stroke(AppTheme.secundario, lineWidth: 1)
                            )
                            .frame(width: 58, height: 58)

                        Image(systemName: "fuelpump")
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(AppTheme.principal)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(format: "%@ · %.1f km", estacion.marca, estacion.distanciaKm))
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.secundario)

                        Text("Permiso CNE/CRE: \(estacion.permiso)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.secundario)
                    }

                    Spacer()
                }

                ZStack {
                    RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                        .fill(AppTheme.fondo)
                        .overlay(
                            RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                .stroke(AppTheme.secundario, lineWidth: 1)
                        )

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Dirección")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundStyle(AppTheme.secundario)

                        Text(estacion.direccion ?? String(format: "Lat %.4f, Lng %.4f", estacion.lat, estacion.lng))
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.texto)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(AppTheme.Spacing.md)
                }

                HStack(alignment: .firstTextBaseline) {
                    Text("Precios por combustible")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.texto)
                    Spacer()
                    Text("vs. Mi estación")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.secundario)
                }
                .padding(.top, AppTheme.Spacing.xs)

                ForEach(Combustible.allCases) { combustible in
                    let precioEstacion = estacion.precio(para: combustible)
                    let miPrecio = misPrecios[combustible]

                    ZStack {
                        RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                            .fill(AppTheme.fondo)
                            .overlay(
                                RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                    .stroke(AppTheme.secundario, lineWidth: 1)
                            )

                        VStack(spacing: 12) {
                            HStack(alignment: .center) {
                                Text(combustible.nombreVisible)
                                    .font(.headline)
                                    .fontWeight(.bold)
                                    .foregroundStyle(AppTheme.texto)

                                Spacer()

                                if let precioEstacion, let miPrecio {
                                    let diff = precioEstacion - miPrecio
                                    PriceBadge(
                                        texto: textoDiferencia(diff),
                                        nivel: NivelPrecio.evaluar(precio: precioEstacion, referencia: miPrecio)
                                    )
                                }
                            }

                            HStack(alignment: .firstTextBaseline) {
                                if let precioEstacion {
                                    Text(String(format: "$%.2f", precioEstacion))
                                        .font(.system(size: 32, weight: .bold))
                                        .monospacedDigit()
                                        .foregroundStyle(AppTheme.texto)
                                    Text("/ L")
                                        .font(.subheadline)
                                        .foregroundStyle(AppTheme.secundario)
                                } else {
                                    Text("N/D")
                                        .font(.title2)
                                        .foregroundStyle(AppTheme.secundario)
                                }

                                Spacer()

                                if let miPrecio {
                                    Text(String(format: "Mi precio: $%.2f", miPrecio))
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundStyle(AppTheme.secundario)
                                }
                            }
                        }
                        .padding(AppTheme.Spacing.md)
                    }
                }
            }
            .padding(AppTheme.Spacing.md)
        }
        .background(AppTheme.fondo)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
    }

    private func textoDiferencia(_ diff: Double) -> String {
        if abs(diff) < 0.005 {
            return "$0.00 igual"
        } else if diff > 0 {
            return String(format: "+$%.2f más cara", diff)
        } else {
            return String(format: "-$%.2f más barata", abs(diff))
        }
    }
}

#Preview {
    NavigationStack {
        DetalleEstacionView(
            estacion: EstacionDTO(
                id: 102,
                permiso: "PL/19482/EXP/ES/2016",
                nombre: "Shell Paseo de la Reforma",
                marca: "Shell",
                direccion: "Av. Paseo de la Reforma 412, Col. Juárez, Cuauhtémoc, 06600 Ciudad de México, CDMX",
                lat: 19.427,
                lng: -99.167,
                distanciaKm: 1.4,
                precios: ["magna": 23.69, "premium": 25.25, "diesel": 24.80]
            ),
            misPrecios: [.magna: 23.50, .premium: 25.40, .diesel: 24.80]
        )
    }
}
