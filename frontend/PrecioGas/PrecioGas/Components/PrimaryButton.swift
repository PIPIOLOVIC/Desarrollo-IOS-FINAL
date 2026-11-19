import SwiftUI

struct PrimaryButton: View {
    let titulo: String
    var icono: String? = nil
    var habilitado: Bool = true
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            HStack(spacing: 8) {
                if let icono {
                    Image(systemName: icono)
                        .font(.headline.weight(.semibold))
                }
                Text(titulo)
                    .font(.headline)
                    .fontWeight(.bold)
            }
            .foregroundStyle(AppTheme.fondo)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                habilitado ? AppTheme.principal : AppTheme.secundario.opacity(0.5),
                in: RoundedRectangle(cornerRadius: AppTheme.Radius.button, style: .continuous)
            )
        }
        .buttonStyle(.plain)
        .disabled(!habilitado)
    }
}

#Preview {
    VStack(spacing: 12) {
        PrimaryButton(titulo: "Actualizar precios", icono: "pencil") {}
        PrimaryButton(titulo: "Guardar", habilitado: false) {}
    }
    .padding()
}
