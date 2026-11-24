import SwiftUI

struct MiGasolineraView: View {
    @Bindable var viewModel: MiGasolineraViewModel
    @State private var mostrarActualizarPrecios = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppTheme.Spacing.lg) {
                HStack(spacing: AppTheme.Spacing.md) {
                    ZStack {
                        RoundedRectangle(cornerRadius: AppTheme.Radius.iconBox, style: .continuous)
                            .stroke(
                                AppTheme.secundario,
                                style: StrokeStyle(lineWidth: 1.2, dash: [4, 3])
                            )
                            .frame(width: 64, height: 64)

                        Text("LOGO\nPENDIENTE")
                            .font(.system(size: 10, weight: .semibold))
                            .multilineTextAlignment(.center)
                            .foregroundStyle(AppTheme.secundario)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Estación Reforma")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.texto)

                        Text("Actualizado: \(viewModel.fechaUltimaActualizacion.isEmpty ? "--" : viewModel.fechaUltimaActualizacion)")
                            .font(.footnote)
                            .foregroundStyle(AppTheme.secundario)
                    }

                    Spacer()
                }

                EstadoDatosBanner(
                    sinConexion: viewModel.sinConexion,
                    mensajeError: viewModel.mensajeError,
                    mensajeInfo: viewModel.mensajeInfo
                )

                VStack(alignment: .leading, spacing: AppTheme.Spacing.sm) {
                    HStack(alignment: .firstTextBaseline) {
                        Text("Precios actuales")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.texto)
                        Spacer()
                        Text("MXN / litro")
                            .font(.footnote)
                            .foregroundStyle(AppTheme.secundario)
                    }

                    ForEach(Combustible.allCases) { combustible in
                        ZStack {
                            RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                .fill(AppTheme.fondo)
                                .overlay(
                                    RoundedRectangle(cornerRadius: AppTheme.Radius.card, style: .continuous)
                                        .stroke(AppTheme.secundario, lineWidth: 1)
                                )

                            VStack(alignment: .leading, spacing: 10) {
                                Text(combustible.nombreVisible)
                                    .font(.headline)
                                    .fontWeight(.bold)
                                    .foregroundStyle(AppTheme.texto)

                                HStack(alignment: .firstTextBaseline, spacing: 6) {
                                    if let precio = viewModel.preciosActuales[combustible] {
                                        Text(String(format: "$%.2f", precio))
                                            .font(.system(size: 36, weight: .bold))
                                            .monospacedDigit()
                                            .foregroundStyle(AppTheme.texto)
                                    } else {
                                        Text("--")
                                            .font(.system(size: 36, weight: .bold))
                                            .foregroundStyle(AppTheme.secundario)
                                    }

                                    Text("/ L")
                                        .font(.title3)
                                        .foregroundStyle(AppTheme.secundario)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, AppTheme.Spacing.md)
                            .padding(.vertical, 18)
                        }
                    }
                }

                PrimaryButton(titulo: "Actualizar precios", icono: "pencil") {
                    mostrarActualizarPrecios = true
                }
                .padding(.top, AppTheme.Spacing.xs)
            }
            .padding(AppTheme.Spacing.md)
        }
        .background(AppTheme.fondo)
        .navigationTitle("Mi gasolinera")
        .sheet(isPresented: $mostrarActualizarPrecios) {
            ActualizarPreciosSheet(viewModel: viewModel)
        }
        .task {
            await viewModel.cargar()
        }
        .refreshable {
            await viewModel.cargar()
        }
    }
}

#Preview {
    NavigationStack {
        MiGasolineraView(viewModel: MiGasolineraViewModel(repository: MockRepository()))
    }
}
