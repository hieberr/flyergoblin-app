import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    checkForPendingSharedImage()
                }
                .onReceive(
                    NotificationCenter.default.publisher(
                        for: UIApplication.willEnterForegroundNotification
                    )
                ) { _ in
                    checkForPendingSharedImage()
                }
                .onOpenURL { url in
                    guard url.scheme == "flyergoblin", url.host == "share-image" else { return }
                    checkForPendingSharedImage()
                }
        }
    }
}

private func checkForPendingSharedImage() {
    let groupID = "group.com.hologrampacific.flyergoblin"
    guard
        let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: groupID
        )
    else {
        return
    }

    let imageURL = container.appendingPathComponent("pending_shared_image.jpg")
    guard let data = try? Data(contentsOf: imageURL) else {
        return
    }

    // Atomically claim the file by deleting it before processing.
    // If delete fails (file already claimed by another handler), bail out.
    do {
        try FileManager.default.removeItem(at: imageURL)
    } catch {
        return
    }

    DispatchQueue.global(qos: .userInitiated).async {
        let bytes = data.withUnsafeBytes { buffer -> KotlinByteArray in
            let array = KotlinByteArray(size: Int32(buffer.count))
            for (i, byte) in buffer.enumerated() {
                array.set(index: Int32(i), value: Int8(bitPattern: byte))
            }
            return array
        }
        DispatchQueue.main.async {
            SharedImageBridgeKt.setSharedImageFromNative(imageBytes: bytes)
        }
    }
}
