import Foundation
import Observation

@Observable
@MainActor
final class MiGasolineraViewModel {
    private let repository: PreciosRepository

    /// Precio vigente de cada combustible (el primero que aparece en la lista de la API).
    var preciosActuales: [Combustible: Double] = [:]
    /// Registro vigente de cada combustible, para poder corregirlo con `PUT`.
    var registrosActuales: [Combustible: MiPrecioDTO] = [:]
    /// Todos mis precios, del más reciente al más antiguo.
    var historial: [MiPrecioDTO] = []
    /// Fecha del precio más reciente, lista para mostrar (`"19 nov 2026"`).
    var fechaUltimaActualizacion: String = ""
    /// `true` si lo que se muestra viene de la copia local porque no hubo conexión.
    var sinConexion = false
    var cargando = false
    var mensajeError: String?
    /// Aviso no grave, por ejemplo "se guardó sin conexión".
    var mensajeInfo: String?

    init(repository: PreciosRepository) {
        self.repository = repository
    }

    func cargar() async {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        do {
            let respuesta = try await repository.obtenerMisPrecios()
            aplicar(respuesta.valor)
            sinConexion = respuesta.desdeCache
            if !respuesta.desdeCache, historial.allSatisfy({ $0.id > 0 }) {
                mensajeInfo = nil
            }
        } catch is CancellationError {
            return
        } catch {
            mensajeError = error.localizedDescription
        }
    }

    /// Registra los precios que cambiaron. Devuelve `true` si todo se guardó
    /// (en la API o, sin conexión, en la copia local para enviarlo después).
    @discardableResult
    func actualizarPrecios(magna: Double?, premium: Double?, diesel: Double?) async -> Bool {
        cargando = true
        mensajeError = nil
        defer { cargando = false }

        let nuevos: [(Combustible, Double?)] = [(.magna, magna), (.premium, premium), (.diesel, diesel)]
        var quedaronPendientes = false
        do {
            for (combustible, precio) in nuevos {
                guard let precio, !esIgual(precio, preciosActuales[combustible]) else { continue }
                let registro = try await repository.guardarMiPrecio(combustible: combustible, precio: precio)
                if registro.id < 0 { quedaronPendientes = true }
            }
        } catch {
            mensajeError = error.localizedDescription
            await cargar()
            return false
        }

        await cargar()
        mensajeInfo = quedaronPendientes
            ? "Sin conexión: los precios se guardaron en el teléfono y se enviarán al reconectar."
            : nil
        return true
    }

    /// Corrige el precio vigente de un combustible (`PUT /mis-precios/{id}`).
    @discardableResult
    func corregirPrecio(_ combustible: Combustible, a precio: Double) async -> Bool {
        guard let registro = registrosActuales[combustible] else { return false }
        cargando = true
        mensajeError = nil
        defer { cargando = false }
        do {
            _ = try await repository.corregirMiPrecio(id: registro.id, precio: precio)
            await cargar()
            return true
        } catch {
            mensajeError = error.localizedDescription
            return false
        }
    }

    private func aplicar(_ lista: [MiPrecioDTO]) {
        var registros: [Combustible: MiPrecioDTO] = [:]
        for item in lista {
            guard let tipo = item.tipoCombustible, registros[tipo] == nil else { continue }
            registros[tipo] = item
        }
        historial = lista
        registrosActuales = registros
        preciosActuales = registros.mapValues(\.precio)
        fechaUltimaActualizacion = Formato.fechaVisible(lista.first?.fecha)
    }

    private func esIgual(_ a: Double, _ b: Double?) -> Bool {
        guard let b else { return false }
        return abs(a - b) < 0.005
    }
}
