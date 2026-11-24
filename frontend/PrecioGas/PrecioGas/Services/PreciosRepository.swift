import Foundation

/// Datos que devuelve un repositorio, con la marca de si vienen de la copia local
/// (SwiftData) porque no hubo conexión con la API.
struct Cargado<Valor> {
    let valor: Valor
    var desdeCache: Bool = false
}

/// Puerta única de la app hacia los datos. Las vistas y los ViewModels solo conocen
/// este protocolo, así se cambia de datos de ejemplo a la API real sin reescribir la interfaz.
///
/// - `MockRepository`: datos de ejemplo de `ejemplo.json` (previews y demo sin API).
/// - `RemoteRepository`: API REST en Java + copia local en SwiftData para trabajar sin conexión.
protocol PreciosRepository: AnyObject {
    func obtenerCompetencia(combustible: Combustible?, radioKm: Double) async throws -> Cargado<CompetenciaResponseDTO>
    func obtenerResumen(combustible: Combustible) async throws -> Cargado<ResumenDTO>
    func obtenerMisPrecios() async throws -> Cargado<[MiPrecioDTO]>
    func guardarMiPrecio(combustible: Combustible, precio: Double) async throws -> MiPrecioDTO
    func corregirMiPrecio(id: Int, precio: Double) async throws -> MiPrecioDTO
}
