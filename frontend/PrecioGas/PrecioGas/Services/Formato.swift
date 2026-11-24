import Foundation

/// Conversión de las fechas ISO 8601 del contrato a texto para la interfaz.
enum Formato {
    private static let zonaMexico = TimeZone(identifier: "America/Mexico_City") ?? .current
    private static let locale = Locale(identifier: "es_MX")

    /// `"2026-11-17T06:00:00Z"` (con o sin fracciones de segundo) o `"2026-11-17"`.
    static func fecha(desde texto: String?) -> Date? {
        guard let texto, !texto.isEmpty else { return nil }
        if let fecha = try? Date(texto, strategy: .iso8601) { return fecha }
        let conFraccion = Date.ISO8601FormatStyle(includingFractionalSeconds: true)
        if let fecha = try? Date(texto, strategy: conFraccion) { return fecha }
        let soloDia = Date.ISO8601FormatStyle(timeZone: zonaMexico).year().month().day()
        return try? Date(texto, strategy: soloDia)
    }

    /// `"19 nov 2026, 06:00 h"`, o `"17 nov 2026"` si el texto solo trae el día.
    /// Si no se puede leer, devuelve el texto tal cual.
    static func fechaVisible(_ texto: String?) -> String {
        guard let texto, !texto.isEmpty else { return "" }
        guard let fecha = fecha(desde: texto) else { return texto }
        let dia = Date.FormatStyle(timeZone: zonaMexico).locale(locale).day().month(.abbreviated).year()
        var resultado = fecha.formatted(dia).replacingOccurrences(of: ".", with: "")
        if texto.contains("T") {
            let hora = Date.FormatStyle(timeZone: zonaMexico).locale(locale).hour(.twoDigits(amPM: .omitted)).minute(.twoDigits)
            resultado += ", \(fecha.formatted(hora)) h"
        }
        return resultado
    }

    /// Fecha de hoy en Ciudad de México como `"2026-11-23"`, igual que la pone la API.
    static func hoyISO() -> String {
        Date.now.formatted(Date.ISO8601FormatStyle(timeZone: zonaMexico).year().month().day())
    }
}
