import SwiftUI

/// Aviso pequeño sobre el origen de los datos: sin conexión (copia local),
/// un error de la API o un mensaje informativo.
struct EstadoDatosBanner: View {
    var sinConexion: Bool = false
    var mensajeError: String? = nil
    var mensajeInfo: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: AppTheme.Spacing.xs) {
            if let mensajeError {
                fila(icono: "exclamationmark.triangle.fill", texto: mensajeError, color: AppTheme.caro)
            }
            if sinConexion {
                fila(icono: "wifi.slash", texto: "Sin conexión: mostrando la última copia guardada.", color: AppTheme.secundario)
            }
            if let mensajeInfo {
                fila(icono: "info.circle.fill", texto: mensajeInfo, color: AppTheme.secundario)
            }
        }
    }

    private func fila(icono: String, texto: String, color: Color) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Image(systemName: icono)
                .foregroundStyle(color)
            Text(texto)
                .font(.footnote)
                .foregroundStyle(AppTheme.texto)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, AppTheme.Spacing.sm)
        .padding(.vertical, 10)
        .background(color.opacity(0.15), in: RoundedRectangle(cornerRadius: AppTheme.Radius.button, style: .continuous))
    }
}

#Preview {
    VStack(spacing: 12) {
        EstadoDatosBanner(sinConexion: true)
        EstadoDatosBanner(mensajeError: "No hay conexión con el servidor de PrecioGas.")
        EstadoDatosBanner(mensajeInfo: "Los precios se enviarán al reconectar.")
    }
    .padding()
}
