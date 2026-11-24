import Foundation
import SwiftData

/// Repositorio real: lee y guarda en la API en Java y conserva una copia en SwiftData.
///
/// - Lecturas: si la API responde, se actualiza la copia local; si no hay conexión
///   (o el servidor falla con 5xx), se devuelve la copia con `desdeCache = true`.
/// - Mis precios sin conexión: se guardan como pendientes y se mandan solos en la
///   siguiente carga que logre hablar con la API.
final class RemoteRepository: PreciosRepository {
    private let api: APIClient
    private let contexto: ModelContext

    init(api: APIClient = APIClient(), contexto: ModelContext) {
        self.api = api
        self.contexto = contexto
    }

    // MARK: - Competencia

    func obtenerCompetencia(combustible: Combustible?, radioKm: Double) async throws -> Cargado<CompetenciaResponseDTO> {
        do {
            let respuesta: CompetenciaResponseDTO = try await api.get(
                "competencia",
                query: [
                    "combustible": combustible?.rawValue,
                    "radio": String(format: "%g", radioKm)
                ]
            )
            guardarEnCache(respuesta)
            return Cargado(valor: respuesta)
        } catch let error as APIError where error.permiteUsarCache {
            let copia = try contexto.fetch(FetchDescriptor<EstacionCache>())
            guard !copia.isEmpty else { throw error }
            let estaciones = copia
                .map(\.dto)
                .filter { $0.distanciaKm <= radioKm }
                .filter { estacion in combustible.map { estacion.precio(para: $0) != nil } ?? true }
                .sorted { $0.distanciaKm < $1.distanciaKm }
            let actualizado = copia.compactMap(\.actualizado).max()
            return Cargado(valor: CompetenciaResponseDTO(actualizado: actualizado, estaciones: estaciones), desdeCache: true)
        }
    }

    private func guardarEnCache(_ respuesta: CompetenciaResponseDTO) {
        let ids = respuesta.estaciones.map(\.id)
        let existentes = (try? contexto.fetch(FetchDescriptor<EstacionCache>(predicate: #Predicate { ids.contains($0.id) }))) ?? []
        let porId = Dictionary(existentes.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        for estacion in respuesta.estaciones {
            if let registro = porId[estacion.id] {
                registro.actualizar(con: estacion, actualizado: respuesta.actualizado)
            } else {
                contexto.insert(EstacionCache(dto: estacion, actualizado: respuesta.actualizado))
            }
        }
        try? contexto.save()
    }

    // MARK: - Resumen

    func obtenerResumen(combustible: Combustible) async throws -> Cargado<ResumenDTO> {
        let clave = combustible.rawValue
        let descriptor = FetchDescriptor<ResumenCache>(predicate: #Predicate { $0.combustible == clave })
        do {
            let resumen: ResumenDTO = try await api.get("resumen", query: ["combustible": clave])
            if let registro = try? contexto.fetch(descriptor).first {
                registro.actualizar(con: resumen)
            } else {
                contexto.insert(ResumenCache(dto: resumen))
            }
            try? contexto.save()
            return Cargado(valor: resumen)
        } catch let error as APIError where error.permiteUsarCache {
            guard let copia = try contexto.fetch(descriptor).first else { throw error }
            return Cargado(valor: copia.dto, desdeCache: true)
        }
    }

    // MARK: - Mis precios

    func obtenerMisPrecios() async throws -> Cargado<[MiPrecioDTO]> {
        do {
            try await enviarPendientes()
            let lista: [MiPrecioDTO] = try await api.get("mis-precios")
            reemplazarMisPreciosLocales(con: lista)
            return Cargado(valor: lista)
        } catch let error as APIError where error.permiteUsarCache {
            let locales = try misPreciosLocales()
            guard !locales.isEmpty else { throw error }
            return Cargado(valor: locales.map(\.dto), desdeCache: true)
        }
    }

    /// Devuelve el registro creado. Si no hubo conexión, queda pendiente en SwiftData
    /// y se devuelve con `id` negativo.
    func guardarMiPrecio(combustible: Combustible, precio: Double) async throws -> MiPrecioDTO {
        do {
            let creado: MiPrecioDTO = try await api.post(
                "mis-precios",
                cuerpo: NuevoPrecioRequest(combustible: combustible.rawValue, precio: precio)
            )
            contexto.insert(MiPrecio(dto: creado, creadoEn: .now))
            try? contexto.save()
            return creado
        } catch let error as APIError where error.permiteUsarCache {
            let pendiente = MiPrecio(
                idRemoto: nil,
                combustible: combustible.rawValue,
                precio: precio,
                fecha: Formato.hoyISO(),
                pendiente: true
            )
            contexto.insert(pendiente)
            try contexto.save()
            return pendiente.dto
        }
    }

    func corregirMiPrecio(id: Int, precio: Double) async throws -> MiPrecioDTO {
        // Un precio que todavía no llega a la API se corrige solo en local.
        if id < 0, let pendiente = try misPreciosLocales().first(where: { $0.dto.id == id }) {
            pendiente.precio = precio
            try contexto.save()
            return pendiente.dto
        }
        let actualizado: MiPrecioDTO = try await api.put("mis-precios/\(id)", cuerpo: CorreccionPrecioRequest(precio: precio))
        let idBuscado: Int? = id
        if let registro = try? contexto.fetch(FetchDescriptor<MiPrecio>(predicate: #Predicate { $0.idRemoto == idBuscado })).first {
            registro.precio = actualizado.precio
            registro.fecha = actualizado.fecha
            try? contexto.save()
        }
        return actualizado
    }

    // MARK: - Apoyo

    private func misPreciosLocales() throws -> [MiPrecio] {
        try contexto.fetch(FetchDescriptor<MiPrecio>(sortBy: [SortDescriptor(\.creadoEn, order: .reverse)]))
    }

    /// Manda a la API, del más viejo al más nuevo, los precios guardados sin conexión.
    private func enviarPendientes() async throws {
        let pendientes = try contexto.fetch(FetchDescriptor<MiPrecio>(
            predicate: #Predicate { $0.pendiente },
            sortBy: [SortDescriptor(\.creadoEn)]
        ))
        for registro in pendientes {
            let creado: MiPrecioDTO = try await api.post(
                "mis-precios",
                cuerpo: NuevoPrecioRequest(combustible: registro.combustible, precio: registro.precio)
            )
            registro.idRemoto = creado.id
            registro.fecha = creado.fecha
            registro.pendiente = false
            try? contexto.save()
        }
    }

    /// La lista de la API manda: se borra la copia (menos pendientes) y se vuelve a escribir
    /// conservando el orden del servidor (del más reciente al más antiguo).
    private func reemplazarMisPreciosLocales(con lista: [MiPrecioDTO]) {
        try? contexto.delete(model: MiPrecio.self, where: #Predicate { !$0.pendiente })
        let ahora = Date.now
        for (indice, dto) in lista.enumerated() {
            contexto.insert(MiPrecio(dto: dto, creadoEn: ahora.addingTimeInterval(-Double(indice))))
        }
        try? contexto.save()
    }
}
