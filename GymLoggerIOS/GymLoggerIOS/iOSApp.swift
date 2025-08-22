import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        // Initialize the database when the app starts
        DatabaseModule.shared.initialize(databaseDriverFactory: DatabaseDriverFactory())
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
