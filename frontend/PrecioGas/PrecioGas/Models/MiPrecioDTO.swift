import Foundation

/// Un registro de `GET/POST/PUT /mis-precios`.
struct MiPrecioDTO: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let combustible: String
    let precio: Double
    /// Solo fecha en ISO 8601 (`"2026-11-17"`); la pone el servidor.
    let fecha: String

    var tipoCombustible: Combustible? {
        Combustible(rawValue: combustible)
    }
}

/// Cuerpo de `POST /mis-precios`.
struct NuevoPrecioRequest: Encodable, Sendable {
    let combustible: String
    let precio: Double
}

/// Cuerpo de `PUT /mis-precios/{id}`.
struct CorreccionPrecioRequest: Encodable, Sendable {
    let precio: Double
}

/// Respuesta de error del contrato: `{"error": "mensaje"}`.
struct ErrorDTO: Decodable, Sendable {
    let error: String
}
