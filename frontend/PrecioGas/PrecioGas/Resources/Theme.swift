import SwiftUI

enum AppTheme {
    // Paleta oficial de 5 colores definida en Assets.xcassets
    static let fondo = Color("AppBackground")
    static let texto = Color("AppText")
    static let principal = Color("AppPrimary")      // Principal y estado Barato
    static let secundario = Color("AppSecondary")   // Subtítulos, bordes y estado Promedio
    static let caro = Color("AppExpensive")         // Estado Caro (con texto negro)

    enum Spacing {
        static let xs: CGFloat = 6
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
    }

    enum Radius {
        static let card: CGFloat = 16
        static let button: CGFloat = 12
        static let iconBox: CGFloat = 14
    }
}
