import Foundation

/// Respuesta de `GET /competencia`.
struct CompetenciaResponseDTO: Codable, Hashable, Sendable {
    /// Fecha y hora ISO 8601 de la última carga de datos de la CNE (puede faltar).
    let actualizado: String?
    let estaciones: [EstacionDTO]
}

/// Una estación de la competencia, tal como la manda la API.
struct EstacionDTO: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let permiso: String
    let nombre: String
    let marca: String
    let direccion: String?
    let lat: Double
    let lng: Double
    let distanciaKm: Double
    /// Precio por combustible (`magna`, `premium`, `diesel`). Una estación puede no tener los tres.
    let precios: [String: Double]

    init(
        id: Int,
        permiso: String,
        nombre: String,
        marca: String,
        direccion: String? = nil,
        lat: Double,
        lng: Double,
        distanciaKm: Double,
        precios: [String: Double]
    ) {
        self.id = id
        self.permiso = permiso
        self.nombre = nombre
        self.marca = marca
        self.direccion = direccion
        self.lat = lat
        self.lng = lng
        self.distanciaKm = distanciaKm
        self.precios = precios
    }

    private enum CodingKeys: String, CodingKey {
        case id, permiso, nombre, marca, direccion, lat, lng, distanciaKm, precios
    }

    /// Decodificación tolerante: la API puede mandar `null` en la distancia, en textos
    /// o en algún precio, y eso no debe tirar toda la lista.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        permiso = try c.decodeIfPresent(String.self, forKey: .permiso) ?? ""
        nombre = try c.decodeIfPresent(String.self, forKey: .nombre) ?? "Sin nombre"
        marca = try c.decodeIfPresent(String.self, forKey: .marca) ?? "Sin marca"
        direccion = try c.decodeIfPresent(String.self, forKey: .direccion)
        lat = try c.decodeIfPresent(Double.self, forKey: .lat) ?? 0
        lng = try c.decodeIfPresent(Double.self, forKey: .lng) ?? 0
        distanciaKm = try c.decodeIfPresent(Double.self, forKey: .distanciaKm) ?? 0
        let crudos = try c.decodeIfPresent([String: Double?].self, forKey: .precios) ?? [:]
        precios = crudos.compactMapValues { $0 }
    }

    func precio(para combustible: Combustible) -> Double? {
        precios[combustible.rawValue]
    }
}
