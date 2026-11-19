import Foundation

protocol PreciosRepository: Sendable {
    func obtenerCompetencia(combustible: Combustible?, radioKm: Double) async throws -> CompetenciaResponseDTO
    func obtenerResumen(combustible: Combustible) async throws -> ResumenDTO
    func obtenerMisPrecios() async throws -> [MiPrecioDTO]
    func guardarMiPrecio(combustible: Combustible, precio: Double) async throws -> MiPrecioDTO
}

final class MockRepository: PreciosRepository, @unchecked Sendable {
    private var misPrecios: [MiPrecioDTO] = [
        MiPrecioDTO(id: 1, combustible: "magna", precio: 23.50, fecha: "19 nov 2026, 06:00 h"),
        MiPrecioDTO(id: 2, combustible: "premium", precio: 25.40, fecha: "19 nov 2026, 06:00 h"),
        MiPrecioDTO(id: 3, combustible: "diesel", precio: 24.80, fecha: "19 nov 2026, 06:00 h")
    ]

    private let estacionesEjemplo: [EstacionDTO] = [
        EstacionDTO(
            id: 101,
            permiso: "PL/10214/EXP/ES/2015",
            nombre: "Pemex Insurgentes Sur",
            marca: "Pemex",
            direccion: "Av. Insurgentes Sur 318, Col. Roma Norte, Cuauhtémoc, 06700 Ciudad de México, CDMX",
            lat: 19.420,
            lng: -99.163,
            distanciaKm: 0.9,
            precios: ["magna": 23.35, "premium": 25.20, "diesel": 24.65]
        ),
        EstacionDTO(
            id: 102,
            permiso: "PL/19482/EXP/ES/2016",
            nombre: "Shell Paseo de la Reforma",
            marca: "Shell",
            direccion: "Av. Paseo de la Reforma 412, Col. Juárez, Cuauhtémoc, 06600 Ciudad de México, CDMX",
            lat: 19.427,
            lng: -99.167,
            distanciaKm: 1.4,
            precios: ["magna": 23.69, "premium": 25.25, "diesel": 24.80]
        ),
        EstacionDTO(
            id: 103,
            permiso: "PL/20541/EXP/ES/2018",
            nombre: "Mobil Polanco",
            marca: "Mobil",
            direccion: "Av. Presidente Masaryk 215, Polanco, Miguel Hidalgo, 11560 Ciudad de México, CDMX",
            lat: 19.432,
            lng: -99.190,
            distanciaKm: 2.3,
            precios: ["magna": 23.50, "premium": 25.40, "diesel": 24.90]
        ),
        EstacionDTO(
            id: 104,
            permiso: "PL/21890/EXP/ES/2019",
            nombre: "Oxxo Gas Chapultepec",
            marca: "Oxxo Gas",
            direccion: "Av. Chapultepec 490, Col. Roma Norte, Cuauhtémoc, 06700 Ciudad de México, CDMX",
            lat: 19.422,
            lng: -99.172,
            distanciaKm: 3.1,
            precios: ["magna": 23.29, "premium": 25.10, "diesel": 24.55]
        ),
        EstacionDTO(
            id: 105,
            permiso: "PL/23410/EXP/ES/2020",
            nombre: "BP Mariano Escobedo",
            marca: "BP",
            direccion: "Calz. Gral. Mariano Escobedo 510, Anzures, Miguel Hidalgo, 11590 Ciudad de México, CDMX",
            lat: 19.434,
            lng: -99.181,
            distanciaKm: 4.4,
            precios: ["magna": 23.85, "premium": 25.75, "diesel": 25.15]
        ),
        EstacionDTO(
            id: 106,
            permiso: "PL/25109/EXP/ES/2021",
            nombre: "Estación Periférico Norte",
            marca: "G500",
            direccion: "Blvd. Manuel Ávila Camacho 145, Lomas de Chapultepec, 11000 Ciudad de México, CDMX",
            lat: 19.448,
            lng: -99.210,
            distanciaKm: 6.8,
            precios: ["magna": 22.90, "premium": 24.95, "diesel": 24.40]
        )
    ]

    func obtenerCompetencia(combustible: Combustible?, radioKm: Double) async throws -> CompetenciaResponseDTO {
        let filtradas = estacionesEjemplo
            .filter { $0.distanciaKm <= radioKm }
            .filter { estacion in
                guard let combustible else { return true }
                return estacion.precio(para: combustible) != nil
            }
            .sorted { $0.distanciaKm < $1.distanciaKm }

        return CompetenciaResponseDTO(
            actualizado: "19 nov 2026, 06:00 h",
            estaciones: filtradas
        )
    }

    func obtenerResumen(combustible: Combustible) async throws -> ResumenDTO {
        let preciosZona = estacionesEjemplo
            .filter { $0.distanciaKm <= 5.0 }
            .compactMap { $0.precio(para: combustible) }

        let promedio = preciosZona.isEmpty ? nil : preciosZona.reduce(0, +) / Double(preciosZona.count)
        let minimo = preciosZona.min()
        let maximo = preciosZona.max()
        let miPrecio = misPrecios.first(where: { $0.combustible == combustible.rawValue })?.precio
        let diferencia: Double? = if let miPrecio, let promedio { miPrecio - promedio } else { nil }

        return ResumenDTO(
            combustible: combustible.rawValue,
            promedio: promedio,
            minimo: minimo,
            maximo: maximo,
            miPrecio: miPrecio,
            diferencia: diferencia,
            estaciones: 12
        )
    }

    func obtenerMisPrecios() async throws -> [MiPrecioDTO] {
        misPrecios
    }

    func guardarMiPrecio(combustible: Combustible, precio: Double) async throws -> MiPrecioDTO {
        let nuevoId = (misPrecios.map(\.id).max() ?? 0) + 1
        let registro = MiPrecioDTO(
            id: nuevoId,
            combustible: combustible.rawValue,
            precio: precio,
            fecha: "19 nov 2026, 06:00 h"
        )
        misPrecios.insert(registro, at: 0)
        return registro
    }
}
