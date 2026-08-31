import SwiftUI
import UIKit
import MomentumShared

/// Aloja la interfaz Compose compartida dentro de SwiftUI.
///
/// `MainViewControllerKt` lo genera Kotlin/Native al exportar el framework: es
/// el puente por el que la misma interfaz que corre en Android se dibuja aquí.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}
