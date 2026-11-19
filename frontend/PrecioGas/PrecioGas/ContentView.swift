import SwiftUI

struct ContentView: View {
    @State private var miGasolineraVM: MiGasolineraViewModel
    @State private var competenciaVM: CompetenciaViewModel
    @State private var resumenVM: ResumenViewModel

    init(repository: PreciosRepository = MockRepository()) {
        _miGasolineraVM = State(initialValue: MiGasolineraViewModel(repository: repository))
        _competenciaVM = State(initialValue: CompetenciaViewModel(repository: repository))
        _resumenVM = State(initialValue: ResumenViewModel(repository: repository))
    }

    var body: some View {
        TabView {
            Tab("Mi gasolinera", systemImage: "fuelpump") {
                NavigationStack {
                    MiGasolineraView(viewModel: miGasolineraVM)
                }
            }

            Tab("Competencia", systemImage: "mappin.and.ellipse") {
                NavigationStack {
                    CompetenciaView(
                        viewModel: competenciaVM,
                        misPrecios: miGasolineraVM.preciosActuales
                    )
                }
            }

            Tab("Resumen", systemImage: "chart.bar.fill") {
                NavigationStack {
                    ResumenView(viewModel: resumenVM)
                }
            }
        }
        .tint(AppTheme.principal)
    }
}

#Preview {
    ContentView()
}
