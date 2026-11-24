import SwiftUI

struct ActualizarPreciosSheet: View {
    @Bindable var viewModel: MiGasolineraViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var magnaTexto = ""
    @State private var premiumTexto = ""
    @State private var dieselTexto = ""

    private var esValido: Bool {
        parsear(magnaTexto) != nil || parsear(premiumTexto) != nil || parsear(dieselTexto) != nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Nuevos precios por litro (MXN)") {
                    campoPrecio(titulo: "Magna", texto: $magnaTexto)
                    campoPrecio(titulo: "Premium", texto: $premiumTexto)
                    campoPrecio(titulo: "Diésel", texto: $dieselTexto)
                }

                Section {
                    PrimaryButton(titulo: "Guardar precios", icono: "checkmark", habilitado: esValido) {
                        Task {
                            let guardado = await viewModel.actualizarPrecios(
                                magna: parsear(magnaTexto),
                                premium: parsear(premiumTexto),
                                diesel: parsear(dieselTexto)
                            )
                            if guardado { dismiss() }
                        }
                    }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                if let error = viewModel.mensajeError {
                    Section {
                        EstadoDatosBanner(mensajeError: error)
                            .listRowInsets(EdgeInsets())
                            .listRowBackground(Color.clear)
                    }
                }
            }
            .navigationTitle("Actualizar precios")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                }
            }
            .onAppear {
                if let magna = viewModel.preciosActuales[.magna] {
                    magnaTexto = String(format: "%.2f", magna)
                }
                if let premium = viewModel.preciosActuales[.premium] {
                    premiumTexto = String(format: "%.2f", premium)
                }
                if let diesel = viewModel.preciosActuales[.diesel] {
                    dieselTexto = String(format: "%.2f", diesel)
                }
            }
        }
    }

    private func campoPrecio(titulo: String, texto: Binding<String>) -> some View {
        HStack {
            Text(titulo)
                .font(.headline)
            Spacer()
            TextField("0.00", text: texto)
                .multilineTextAlignment(.trailing)
                #if os(iOS)
                .keyboardType(.decimalPad)
                #endif
        }
    }

    private func parsear(_ texto: String) -> Double? {
        let limpio = texto.replacingOccurrences(of: ",", with: ".").trimmingCharacters(in: .whitespaces)
        guard let valor = Double(limpio), valor > 0, valor < 100 else { return nil }
        return valor
    }
}

#Preview {
    ActualizarPreciosSheet(viewModel: MiGasolineraViewModel(repository: MockRepository()))
}
