import Foundation
import Observation

@Observable
@MainActor
final class CompetenciaViewModel {
    private let repository: PreciosRepository

    var combustibleSeleccionado: Combustible = .magna
    var soloCercanas: Bool = true
    var estaciones: [EstacionDTO] = []
    var actualizado: String = ""
    var cargando = false
    var mensajeError: String?

    init(repository: PreciosRepository) {
        self.repository = repository
    }

    func cargar() async {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        let radio: Double = soloCercanas ? 5.0 : 15.0
        do {
            let respuesta = try await repository.obtenerCompetencia(
                combustible: combustibleSeleccionado,
                radioKm: radio
            )
            estaciones = respuesta.estaciones
            actualizado = respuesta.actualizado
        } catch {
            mensajeError = error.localizedDescription
        }
    }
}
