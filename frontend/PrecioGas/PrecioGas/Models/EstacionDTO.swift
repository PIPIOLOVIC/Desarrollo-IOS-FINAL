import Foundation

struct CompetenciaResponseDTO: Codable, Sendable {
    let actualizado: String
    let estaciones: [EstacionDTO]
}

struct EstacionDTO: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let permiso: String
    let nombre: String
    let marca: String
    let direccion: String?
    let lat: Double
    let lng: Double
    let distanciaKm: Double
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

    func precio(para combustible: Combustible) -> Double? {
        precios[combustible.rawValue]
    }
}
