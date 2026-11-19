import Foundation

enum Combustible: String, CaseIterable, Identifiable, Codable, Hashable {
    case magna
    case premium
    case diesel

    var id: String { rawValue }

    var nombreVisible: String {
        switch self {
        case .magna: "Magna"
        case .premium: "Premium"
        case .diesel: "Diésel"
        }
    }
}
