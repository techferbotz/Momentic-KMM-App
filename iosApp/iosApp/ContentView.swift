import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        // Launched with -designGallery (Edit Scheme → Arguments): the design system on one screen.
        if ProcessInfo.processInfo.arguments.contains("-designGallery") {
            return MainViewControllerKt.DesignGalleryViewController()
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
