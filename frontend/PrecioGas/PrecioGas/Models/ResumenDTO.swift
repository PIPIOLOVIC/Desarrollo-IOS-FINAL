import Foundation

struct ResumenDTO: Codable, Hashable, Sendable {
    let combustible: String
    let promedio: Double?
    let minimo: Double?
    let maximo: Double?
    let miPrecio: Double?
    let diferencia: Double?
    let estaciones: Int
}
