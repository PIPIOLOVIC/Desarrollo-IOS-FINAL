import SwiftUI

struct CompetenciaView: View {
    @Bindable var viewModel: CompetenciaViewModel
    let misPrecios: [Combustible: Double]

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: AppTheme.Spacing.md) {
                Picker("Combustible", selection: $viewModel.combustibleSeleccionado) {
                    ForEach(Combustible.allCases) { combustible in
                        Text(combustible.nombreVisible).tag(combustible)
                    }
                }
                .pickerStyle(.segmented)
                .tint(AppTheme.principal)

                Toggle(isOn: $viewModel.soloCercanas) {
                    Text("Solo cercanas (5 km)")
                        .font(.body)
                        .foregroundStyle(AppTheme.texto)
                }
                .tint(AppTheme.principal)
            }
            .padding(.horizontal, AppTheme.Spacing.md)
            .padding(.vertical, AppTheme.Spacing.sm)
            .background(AppTheme.fondo)

            List(viewModel.estaciones) { estacion in
                let precioEstacion = estacion.precio(para: viewModel.combustibleSeleccionado)
                let miPrecio = misPrecios[viewModel.combustibleSeleccionado]

                ZStack {
                    NavigationLink(value: estacion) {
                        EmptyView()
                    }
                    .opacity(0)

                    StationCard(
                        nombre: estacion.nombre,
                        marca: estacion.marca,
                        distanciaKm: estacion.distanciaKm,
                        precio: precioEstacion,
                        nivel: precioEstacion.map { NivelPrecio.evaluar(precio: $0, referencia: miPrecio) } ?? .promedio,
                        mostrarChevron: true
                    )
                }
                .listRowSeparator(.hidden)
                .listRowBackground(AppTheme.fondo)
                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(AppTheme.fondo)
        }
        .background(AppTheme.fondo)
        .navigationTitle("Competencia")
        .navigationDestination(for: EstacionDTO.self) { estacion in
            DetalleEstacionView(estacion: estacion, misPrecios: misPrecios)
        }
        .task(id: "\(viewModel.combustibleSeleccionado.rawValue)-\(viewModel.soloCercanas)") {
            await viewModel.cargar()
        }
    }
}

#Preview {
    NavigationStack {
        CompetenciaView(
            viewModel: CompetenciaViewModel(repository: MockRepository()),
            misPrecios: [.magna: 23.50, .premium: 25.40, .diesel: 24.80]
        )
    }
}
