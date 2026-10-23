import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Start Koin (data + presentation) once, before any Compose view model is resolved.
        PlatformDataModule_iosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
