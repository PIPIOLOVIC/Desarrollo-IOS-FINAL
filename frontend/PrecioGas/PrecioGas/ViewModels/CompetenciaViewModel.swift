import Foundation
import Observation

@Observable
@MainActor
final class CompetenciaViewModel {
    /// Radio cuando el Toggle "Solo cercanas" está encendido / apagado.
    static let radioCercanasKm = 5.0
    static let radioAmpliadoKm = 15.0

    private let repository: PreciosRepository

    var combustibleSeleccionado: Combustible = .magna
    var soloCercanas: Bool = true
    var estaciones: [EstacionDTO] = []
    /// Fecha de la última carga de datos de la CNE, lista para mostrar.
    var actualizado: String = ""
    var sinConexion = false
    var cargando = false
    var mensajeError: String?

    var radioKm: Double { soloCercanas ? Self.radioCercanasKm : Self.radioAmpliadoKm }

    init(repository: PreciosRepository) {
        self.repository = repository
    }

    func cargar() async {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        do {
            let respuesta = try await repository.obtenerCompetencia(
                combustible: combustibleSeleccionado,
                radioKm: radioKm
            )
            estaciones = respuesta.valor.estaciones
            actualizado = Formato.fechaVisible(respuesta.valor.actualizado)
            sinConexion = respuesta.desdeCache
        } catch is CancellationError {
            return
        } catch {
            mensajeError = error.localizedDescription
        }
    }
}
