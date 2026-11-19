import Foundation
import Observation

@Observable
@MainActor
final class ResumenViewModel {
    private let repository: PreciosRepository

    var combustibleSeleccionado: Combustible = .magna
    var resumen: ResumenDTO?
    var cargando = false
    var mensajeError: String?

    init(repository: PreciosRepository) {
        self.repository = repository
    }

    func cargar() async {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        do {
            resumen = try await repository.obtenerResumen(combustible: combustibleSeleccionado)
        } catch {
            mensajeError = error.localizedDescription
        }
    }
}
