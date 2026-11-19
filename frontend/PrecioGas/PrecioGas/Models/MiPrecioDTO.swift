import Foundation

struct MiPrecioDTO: Identifiable, Codable, Hashable, Sendable {
    let id: Int
    let combustible: String
    let precio: Double
    let fecha: String

    var tipoCombustible: Combustible? {
        Combustible(rawValue: combustible)
    }
}
