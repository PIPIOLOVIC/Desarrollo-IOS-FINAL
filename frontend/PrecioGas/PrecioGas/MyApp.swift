import SwiftUI
import SwiftData

@main struct MyApp: App {
    private let contenedor: ModelContainer
    private let repository: PreciosRepository

    init() {
        contenedor = Persistencia.crearContenedor()
        // Con el argumento de arranque `-usarDatosEjemplo YES` la app usa ejemplo.json
        // y no necesita la API (útil para la demo si Tomcat no está corriendo).
        if UserDefaults.standard.bool(forKey: "usarDatosEjemplo") {
            repository = MockRepository()
        } else {
            repository = RemoteRepository(contexto: contenedor.mainContext)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView(repository: repository)
        }
        .modelContainer(contenedor)
    }
}
