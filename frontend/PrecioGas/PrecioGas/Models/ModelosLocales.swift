import Foundation
import SwiftData

// Modelos de SwiftData: la copia local que permite usar la app sin conexión.
// La API sigue siendo la fuente de verdad; estos registros se reemplazan con
// cada respuesta buena y solo se leen cuando la API no contesta.

/// Uno de mis precios (actual o del historial).
@Model
final class MiPrecio {
    /// `id` que asignó la API. Es `nil` mientras el precio no se ha podido enviar.
    var idRemoto: Int?
    var combustible: String
    var precio: Double
    /// Fecha ISO 8601 (`"2026-11-17"`).
    var fecha: String
    /// `true` si se guardó sin conexión y falta mandarlo con `POST /mis-precios`.
    var pendiente: Bool
    /// Para ordenar del más reciente al más antiguo, igual que la API.
    var creadoEn: Date

    init(idRemoto: Int?, combustible: String, precio: Double, fecha: String, pendiente: Bool = false, creadoEn: Date = .now) {
        self.idRemoto = idRemoto
        self.combustible = combustible
        self.precio = precio
        self.fecha = fecha
        self.pendiente = pendiente
        self.creadoEn = creadoEn
    }

    convenience init(dto: MiPrecioDTO, creadoEn: Date) {
        self.init(idRemoto: dto.id, combustible: dto.combustible, precio: dto.precio, fecha: dto.fecha, creadoEn: creadoEn)
    }

    /// Los pendientes usan un id negativo para que la interfaz los distinga.
    var dto: MiPrecioDTO {
        MiPrecioDTO(id: idRemoto ?? -abs(Int(creadoEn.timeIntervalSince1970 * 1000)), combustible: combustible, precio: precio, fecha: fecha)
    }
}

/// Copia de una estación de la competencia.
@Model
final class EstacionCache {
    @Attribute(.unique) var id: Int
    var permiso: String
    var nombre: String
    var marca: String
    var direccion: String?
    var lat: Double
    var lng: Double
    var distanciaKm: Double
    /// Precios codificados en JSON (`{"magna": 23.69, ...}`).
    var preciosJSON: Data
    /// `actualizado` de la respuesta de `/competencia` en la que llegó.
    var actualizado: String?

    init(dto: EstacionDTO, actualizado: String?) {
        id = dto.id
        permiso = dto.permiso
        nombre = dto.nombre
        marca = dto.marca
        direccion = dto.direccion
        lat = dto.lat
        lng = dto.lng
        distanciaKm = dto.distanciaKm
        preciosJSON = (try? JSONEncoder().encode(dto.precios)) ?? Data()
        self.actualizado = actualizado
    }

    func actualizar(con dto: EstacionDTO, actualizado: String?) {
        permiso = dto.permiso
        nombre = dto.nombre
        marca = dto.marca
        direccion = dto.direccion
        lat = dto.lat
        lng = dto.lng
        distanciaKm = dto.distanciaKm
        preciosJSON = (try? JSONEncoder().encode(dto.precios)) ?? Data()
        self.actualizado = actualizado
    }

    var dto: EstacionDTO {
        let precios = (try? JSONDecoder().decode([String: Double].self, from: preciosJSON)) ?? [:]
        return EstacionDTO(
            id: id, permiso: permiso, nombre: nombre, marca: marca, direccion: direccion,
            lat: lat, lng: lng, distanciaKm: distanciaKm, precios: precios
        )
    }
}

/// Último resumen de zona recibido por combustible.
@Model
final class ResumenCache {
    @Attribute(.unique) var combustible: String
    var promedio: Double?
    var minimo: Double?
    var maximo: Double?
    var miPrecio: Double?
    var diferencia: Double?
    var estaciones: Int
    var guardadoEn: Date

    init(dto: ResumenDTO) {
        combustible = dto.combustible
        promedio = dto.promedio
        minimo = dto.minimo
        maximo = dto.maximo
        miPrecio = dto.miPrecio
        diferencia = dto.diferencia
        estaciones = dto.estaciones
        guardadoEn = .now
    }

    func actualizar(con dto: ResumenDTO) {
        promedio = dto.promedio
        minimo = dto.minimo
        maximo = dto.maximo
        miPrecio = dto.miPrecio
        diferencia = dto.diferencia
        estaciones = dto.estaciones
        guardadoEn = .now
    }

    var dto: ResumenDTO {
        ResumenDTO(
            combustible: combustible, promedio: promedio, minimo: minimo, maximo: maximo,
            miPrecio: miPrecio, diferencia: diferencia, estaciones: estaciones
        )
    }
}

enum Persistencia {
    static let modelos: [any PersistentModel.Type] = [MiPrecio.self, EstacionCache.self, ResumenCache.self]

    /// Contenedor de la app. Si la base local está dañada (por ejemplo tras cambiar un
    /// modelo durante el desarrollo), se usa uno en memoria para que la app no truene.
    static func crearContenedor(enMemoria: Bool = false) -> ModelContainer {
        let schema = Schema(modelos)
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: enMemoria)
        if let contenedor = try? ModelContainer(for: schema, configurations: config) {
            return contenedor
        }
        let memoria = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        return try! ModelContainer(for: schema, configurations: memoria)
    }
}
