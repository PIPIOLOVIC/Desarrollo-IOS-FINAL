import Foundation

/// Errores de red y de la API, con mensajes listos para mostrar al usuario.
enum APIError: LocalizedError, Equatable {
    case urlInvalida
    case sinConexion
    /// La API respondió con 4xx/5xx; `mensaje` viene de `{"error": "..."}` si lo trae.
    case servidor(codigo: Int, mensaje: String?)
    case respuestaInvalida
    case decodificacion(String)

    var errorDescription: String? {
        switch self {
        case .urlInvalida:
            "La dirección de la API no es válida."
        case .sinConexion:
            "No hay conexión con el servidor de PrecioGas."
        case let .servidor(codigo, mensaje):
            mensaje ?? "El servidor respondió con un error (\(codigo))."
        case .respuestaInvalida:
            "El servidor mandó una respuesta inesperada."
        case let .decodificacion(detalle):
            "No se pudieron leer los datos del servidor (\(detalle))."
        }
    }

    /// Si vale la pena caer a la copia local: falla de red o del servidor (5xx),
    /// no un error de validación (4xx), que hay que mostrar tal cual.
    var permiteUsarCache: Bool {
        switch self {
        case .sinConexion, .respuestaInvalida: true
        case let .servidor(codigo, _): codigo >= 500
        default: false
        }
    }
}

/// Configuración de la API.
///
/// La URL base se toma, en orden, de:
/// 1. El argumento de arranque `-apiBaseURL http://192.168.1.50:8080/api`
///    (Product > Scheme > Edit Scheme > Arguments) o de `UserDefaults`.
/// 2. La clave `PrecioGasAPIBaseURL` del Info.plist.
/// 3. `http://localhost:8080/api`, que funciona en el simulador con Tomcat local.
enum APIConfig {
    static let claveBaseURL = "apiBaseURL"
    static let urlPorOmision = "http://localhost:8080/api"

    static var baseURL: URL {
        let candidatos = [
            UserDefaults.standard.string(forKey: claveBaseURL),
            Bundle.main.object(forInfoDictionaryKey: "PrecioGasAPIBaseURL") as? String
        ]
        for texto in candidatos.compactMap({ $0?.trimmingCharacters(in: .whitespaces) }) where !texto.isEmpty {
            if let url = URL(string: texto), url.scheme != nil { return url }
        }
        return URL(string: urlPorOmision)!
    }
}

/// Cliente HTTP mínimo sobre URLSession con async/await.
final class APIClient {
    let baseURL: URL
    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(baseURL: URL = APIConfig.baseURL, session: URLSession? = nil) {
        self.baseURL = baseURL
        if let session {
            self.session = session
        } else {
            let config = URLSessionConfiguration.default
            config.timeoutIntervalForRequest = 10
            config.waitsForConnectivity = false
            self.session = URLSession(configuration: config)
        }
    }

    func get<T: Decodable>(_ ruta: String, query: [String: String?] = [:]) async throws -> T {
        try await enviar(ruta, metodo: "GET", query: query, cuerpo: nil as Data?)
    }

    func post<T: Decodable, B: Encodable>(_ ruta: String, cuerpo: B) async throws -> T {
        try await enviar(ruta, metodo: "POST", cuerpo: try encoder.encode(cuerpo))
    }

    func put<T: Decodable, B: Encodable>(_ ruta: String, cuerpo: B) async throws -> T {
        try await enviar(ruta, metodo: "PUT", cuerpo: try encoder.encode(cuerpo))
    }

    private func enviar<T: Decodable>(
        _ ruta: String,
        metodo: String,
        query: [String: String?] = [:],
        cuerpo: Data?
    ) async throws -> T {
        let url = baseURL.appending(path: ruta)
        guard var componentes = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            throw APIError.urlInvalida
        }
        let items = query.compactMap { clave, valor in valor.map { URLQueryItem(name: clave, value: $0) } }
        if !items.isEmpty {
            componentes.queryItems = items.sorted { $0.name < $1.name }
        }
        guard let urlFinal = componentes.url else { throw APIError.urlInvalida }

        var peticion = URLRequest(url: urlFinal)
        peticion.httpMethod = metodo
        peticion.setValue("application/json", forHTTPHeaderField: "Accept")
        if let cuerpo {
            peticion.httpBody = cuerpo
            peticion.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        }

        let datos: Data
        let respuesta: URLResponse
        do {
            (datos, respuesta) = try await session.data(for: peticion)
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as URLError where error.code == .cancelled {
            throw CancellationError()
        } catch {
            throw APIError.sinConexion
        }

        guard let http = respuesta as? HTTPURLResponse else { throw APIError.respuestaInvalida }
        guard (200..<300).contains(http.statusCode) else {
            let mensaje = try? decoder.decode(ErrorDTO.self, from: datos).error
            throw APIError.servidor(codigo: http.statusCode, mensaje: mensaje)
        }

        do {
            return try decoder.decode(T.self, from: datos)
        } catch let DecodingError.keyNotFound(clave, _) {
            throw APIError.decodificacion("falta \(clave.stringValue)")
        } catch let DecodingError.typeMismatch(_, contexto),
                let DecodingError.valueNotFound(_, contexto) {
            let ruta = contexto.codingPath.map(\.stringValue).joined(separator: ".")
            throw APIError.decodificacion("tipo inesperado en \(ruta)")
        } catch {
            throw APIError.decodificacion("formato no válido")
        }
    }
}
