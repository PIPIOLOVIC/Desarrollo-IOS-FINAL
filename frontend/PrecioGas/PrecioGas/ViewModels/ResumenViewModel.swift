import Foundation
import Observation

@Observable
@MainActor
final class ResumenViewModel {
    private let repository: PreciosRepository

    var combustibleSeleccionado: Combustible = .magna
    var resumen: ResumenDTO?
    /// Fecha de la última carga de datos de la CNE, lista para mostrar.
    var actualizado: String = ""
    var sinConexion = false
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
            let respuesta = try await repository.obtenerResumen(combustible: combustibleSeleccionado)
            resumen = respuesta.valor
            sinConexion = respuesta.desdeCache
        } catch is CancellationError {
            return
        } catch {
            mensajeError = error.localizedDescription
            return
        }

        // /resumen no trae fecha; se toma la de /competencia (misma carga de la CNE).
        if let competencia = try? await repository.obtenerCompetencia(
            combustible: combustibleSeleccionado,
            radioKm: CompetenciaViewModel.radioCercanasKm
        ) {
            actualizado = Formato.fechaVisible(competencia.valor.actualizado)
        }
    }
}
