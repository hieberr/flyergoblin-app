//
//  ShareViewController.swift
//  ShareExtension
//

import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        loadSharedImage()
    }
    
    private func loadSharedImage() {
        let imageType = UTType.image.identifier
        
        guard
            let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
            let attachments = extensionItem.attachments,
            let provider = attachments.first(where: { $0.hasItemConformingToTypeIdentifier(imageType) })
        else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        provider.loadItem(forTypeIdentifier: imageType, options: nil) { [weak self] item, error in
            guard error == nil else {
                self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
                return
            }

            var imageData: Data?

            if let url = item as? URL {
                imageData = try? Data(contentsOf: url)
            } else if let data = item as? Data {
                imageData = data
            } else if let image = item as? UIImage {
                imageData = image.jpegData(compressionQuality: 0.9)
            }

            guard let data = imageData else {
                self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
                return
            }

            self?.saveImageAndOpenApp(data)
        }
    }
    
    // Saves the image to a file that the app will load when it runs. Then open the app.
    private func saveImageAndOpenApp(_ imageData: Data) {
        let groupID = "group.com.hologrampacific.flyergoblin"
        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: groupID
        ) else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        let imageURL = container.appendingPathComponent("pending_shared_image.jpg")
        do {
            try imageData.write(to: imageURL)
        } catch {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        extensionContext?.completeRequest(returningItems: []) { [weak self] _ in
            self?.openMainApp()
        }
    }

    private func openMainApp() {
        guard let url = URL(string: "flyergoblin://share-image") else { return }
    
        var responder: UIResponder? = self
        while responder != nil {
            if let application = responder as? UIApplication {
                application.open(url)
                return
            }
            responder = responder?.next
        }
    }
}
