import SwiftUI
import Charts

private struct PuntoSemana: Identifiable {
    let id = UUID()
    let dia: String
    let precio: Double
}

struct ResumenView: View {
    @Bindable var viewModel: ResumenViewModel

    private var serieSemanalEjemplo: [PuntoSemana] {
        let base = viewModel.resumen?.promedio ?? 23.70
        return [
            PuntoSemana(dia: "Lun", precio: base - 0.12),
            PuntoSemana(dia: "Mar", precio: base - 0.08),
            PuntoSemana(dia: "Mié", precio: base - 0.04),
            PuntoSemana(dia: "Jue", precio: base + 0.03),
            PuntoSemana(dia: "Vie", precio: base + 0.05),
            PuntoSemana(dia: "Sáb", precio: base - 0.02),
            PuntoSemana(dia: "Dom", precio: base)
        ]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppTheme.Spacing.md) {
                Picker("Combustible", selection: $viewModel.combustibleSeleccionado) {
                    ForEach(Combustible.allCases) { combustible in
                        Text(combustible.nombreVisible).tag(combustible)
                    }
                }
                .pickerStyle(.segmented)
                .tint(AppTheme.principal)

                EstadoDatosBanner(sinConexion: viewModel.sinConexion, mensajeError: viewModel.mensajeError)

                if let resumen = viewModel.resumen {
                    // Tarjeta principal: Mi precio vs promedio
                    ZStack {
                        RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                            .fill(AppTheme.fondo)
                            .overlay(
                                RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                    .stroke(AppTheme.secundario, lineWidth: 1)
                            )

                        VStack(alignment: .leading, spacing: 12) {
                            HStack(alignment: .center, spacing: 8) {
                                Text("Mi precio")
                                    .font(.headline)
                                    .fontWeight(.bold)
                                    .foregroundStyle(AppTheme.texto)

                                Spacer()

                                if let diferencia = resumen.diferencia {
                                    PriceBadge(
                                        texto: textoEstadoPromedio(diferencia),
                                        nivel: nivelVsPromedio(diferencia)
                                    )
                                }
                            }

                            HStack(alignment: .firstTextBaseline, spacing: 6) {
                                if let miPrecio = resumen.miPrecio {
                                    Text(String(format: "$%.2f", miPrecio))
                                        .font(.system(size: 34, weight: .bold))
                                        .monospacedDigit()
                                        .foregroundStyle(AppTheme.texto)
                                } else {
                                    Text("N/D")
                                        .font(.system(size: 34, weight: .bold))
                                        .foregroundStyle(AppTheme.secundario)
                                }

                                Text("/ L")
                                    .font(.title3)
                                    .foregroundStyle(AppTheme.secundario)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(AppTheme.Spacing.md)
                    }

                    // Tarjetas lado a lado: Precio mínimo y Precio máximo
                    HStack(spacing: AppTheme.Spacing.sm) {
                        metricaCard(titulo: "Precio mínimo", valor: resumen.minimo)
                        metricaCard(titulo: "Precio máximo", valor: resumen.maximo)
                    }

                    // Tarjeta de gráfica semanal
                    ZStack {
                        RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                            .fill(AppTheme.fondo)
                            .overlay(
                                RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                    .stroke(AppTheme.secundario, lineWidth: 1)
                            )

                        Chart(serieSemanalEjemplo) { punto in
                            AreaMark(
                                x: .value("Día", punto.dia),
                                y: .value("Precio", punto.precio)
                            )
                            .foregroundStyle(AppTheme.principal.opacity(0.12))
                            .interpolationMethod(.catmullRom)

                            LineMark(
                                x: .value("Día", punto.dia),
                                y: .value("Precio", punto.precio)
                            )
                            .foregroundStyle(AppTheme.principal)
                            .lineStyle(StrokeStyle(lineWidth: 2.5))
                            .interpolationMethod(.catmullRom)

                            PointMark(
                                x: .value("Día", punto.dia),
                                y: .value("Precio", punto.precio)
                            )
                            .foregroundStyle(AppTheme.principal)
                        }
                        .chartYScale(domain: .automatic(includesZero: false))
                        .frame(height: 210)
                        .padding(AppTheme.Spacing.md)
                    }

                    // Pie informativo centrado
                    VStack(spacing: 6) {
                        Text("Precio promedio de la última semana")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.texto)

                        Text("\(resumen.estaciones) estaciones analizadas en un radio de 5km")
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.texto)

                        Text("Actualizado: \(viewModel.actualizado.isEmpty ? "--" : viewModel.actualizado)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.secundario)
                    }
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                    .padding(.top, AppTheme.Spacing.xs)
                } else if viewModel.cargando {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                }
            }
            .padding(AppTheme.Spacing.md)
        }
        .background(AppTheme.fondo)
        .navigationTitle("Resumen de zona")
        .task(id: viewModel.combustibleSeleccionado) {
            await viewModel.cargar()
        }
    }

    private func metricaCard(titulo: String, valor: Double?) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                .fill(AppTheme.fondo)
                .overlay(
                    RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                        .stroke(AppTheme.secundario, lineWidth: 1)
                )

            VStack(alignment: .leading, spacing: 10) {
                Text(titulo)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.texto)

                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    if let valor {
                        Text(String(format: "$%.2f", valor))
                            .font(.system(size: 28, weight: .bold))
                            .monospacedDigit()
                            .foregroundStyle(AppTheme.texto)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                    } else {
                        Text("N/D")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(AppTheme.secundario)
                    }

                    Text("/ L")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.secundario)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(AppTheme.Spacing.md)
        }
        .frame(maxWidth: .infinity)
    }

    private func nivelVsPromedio(_ diff: Double) -> NivelPrecio {
        if diff < -0.05 { return .barato }
        if diff > 0.05 { return .caro }
        return .promedio
    }

    private func textoEstadoPromedio(_ diff: Double) -> String {
        if abs(diff) < 0.005 {
            return "En el promedio"
        } else if diff < 0 {
            return String(format: "-$%.2f / L Por debajo del promedio", abs(diff))
        } else {
            return String(format: "+$%.2f / L Por arriba del promedio", diff)
        }
    }
}

#Preview {
    NavigationStack {
        ResumenView(viewModel: ResumenViewModel(repository: MockRepository()))
    }
}
