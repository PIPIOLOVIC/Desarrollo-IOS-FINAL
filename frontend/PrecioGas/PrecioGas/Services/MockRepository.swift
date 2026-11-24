import Foundation

/// Datos de ejemplo para previews y para la demo sin API.
/// Lee `ejemplo.json` (mismo formato que el contrato) y calcula el resumen como la API:
/// estaciones a 5 km, sin incluir la propia, y `diferencia = miPrecio - promedio`.
final class MockRepository: PreciosRepository {
    private struct Ejemplo: Decodable {
        let competencia: CompetenciaResponseDTO
        let misPrecios: [MiPrecioDTO]
    }

    private let competencia: CompetenciaResponseDTO
    private var misPrecios: [MiPrecioDTO]
    private let radioResumenKm = 5.0

    init(bundle: Bundle = .main) {
        let ejemplo = bundle.url(forResource: "ejemplo", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? JSONDecoder().decode(Ejemplo.self, from: $0) }
        competencia = ejemplo?.competencia ?? CompetenciaResponseDTO(actualizado: nil, estaciones: [])
        misPrecios = ejemplo?.misPrecios ?? []
    }

    func obtenerCompetencia(combustible: Combustible?, radioKm: Double) async throws -> Cargado<CompetenciaResponseDTO> {
        let filtradas = competencia.estaciones
            .filter { $0.distanciaKm <= radioKm }
            .filter { estacion in combustible.map { estacion.precio(para: $0) != nil } ?? true }
            .sorted { $0.distanciaKm < $1.distanciaKm }
        return Cargado(valor: CompetenciaResponseDTO(actualizado: competencia.actualizado, estaciones: filtradas))
    }

    func obtenerResumen(combustible: Combustible) async throws -> Cargado<ResumenDTO> {
        let precios = competencia.estaciones
            .filter { $0.distanciaKm <= radioResumenKm }
            .compactMap { $0.precio(para: combustible) }
        let promedio = precios.isEmpty ? nil : redondear(precios.reduce(0, +) / Double(precios.count))
        let miPrecio = misPrecios.first { $0.combustible == combustible.rawValue }?.precio
        let diferencia: Double? = if let miPrecio, let promedio { redondear(miPrecio - promedio) } else { nil }

        return Cargado(valor: ResumenDTO(
            combustible: combustible.rawValue,
            promedio: promedio,
            minimo: precios.min(),
            maximo: precios.max(),
            miPrecio: miPrecio,
            diferencia: diferencia,
            estaciones: precios.count
        ))
    }

    func obtenerMisPrecios() async throws -> Cargado<[MiPrecioDTO]> {
        Cargado(valor: misPrecios)
    }

    func guardarMiPrecio(combustible: Combustible, precio: Double) async throws -> MiPrecioDTO {
        let registro = MiPrecioDTO(
            id: (misPrecios.map(\.id).max() ?? 0) + 1,
            combustible: combustible.rawValue,
            precio: precio,
            fecha: Formato.hoyISO()
        )
        misPrecios.insert(registro, at: 0)
        return registro
    }

    func corregirMiPrecio(id: Int, precio: Double) async throws -> MiPrecioDTO {
        guard let indice = misPrecios.firstIndex(where: { $0.id == id }) else {
            throw APIError.servidor(codigo: 404, mensaje: "No existe el precio \(id).")
        }
        let anterior = misPrecios[indice]
        let corregido = MiPrecioDTO(id: id, combustible: anterior.combustible, precio: precio, fecha: anterior.fecha)
        misPrecios[indice] = corregido
        return corregido
    }

    private func redondear(_ valor: Double) -> Double {
        (valor * 100).rounded() / 100
    }
}
