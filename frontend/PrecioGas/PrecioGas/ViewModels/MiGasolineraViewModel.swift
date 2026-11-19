import Foundation
import Observation

@Observable
@MainActor
final class MiGasolineraViewModel {
    private let repository: PreciosRepository

    var preciosActuales: [Combustible: Double] = [:]
    var fechaUltimaActualizacion: String = ""
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
            let lista = try await repository.obtenerMisPrecios()
            var mapa: [Combustible: Double] = [:]
            for item in lista {
                guard let tipo = item.tipoCombustible, mapa[tipo] == nil else { continue }
                mapa[tipo] = item.precio
            }
            preciosActuales = mapa
            fechaUltimaActualizacion = lista.first?.fecha ?? ""
        } catch {
            mensajeError = error.localizedDescription
        }
    }

    func actualizarPrecios(magna: Double?, premium: Double?, diesel: Double?) async {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        do {
            if let magna { _ = try await repository.guardarMiPrecio(combustible: .magna, precio: magna) }
            if let premium { _ = try await repository.guardarMiPrecio(combustible: .premium, precio: premium) }
            if let diesel { _ = try await repository.guardarMiPrecio(combustible: .diesel, precio: diesel) }
            await cargar()
        } catch {
            mensajeError = error.localizedDescription
        }
    }
}
