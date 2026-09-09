import Foundation

public enum WebSocketStatus {
    case disconnected
    case connecting
    case connected
    case reconnecting
}

public final class WebSocketClient: ObservableObject {
    public static let shared = WebSocketClient()

    @Published public private(set) var status: WebSocketStatus = .disconnected
    private var webSocketTask: URLSessionWebSocketTask?
    private let urlSession = URLSession(configuration: .default)
    private var isIntentionallyClosed = false
    private var reconnectTimer: Timer?
    private var messageHandlers: [(String) -> Void] = []

    private init() {}

    public func connect(wsURL: URL = URL(string: "ws://localhost:8080/ws/websocket")!) {
        guard status != .connected && status != .connecting else { return }

        status = .connecting
        isIntentionallyClosed = false

        webSocketTask = urlSession.webSocketTask(with: wsURL)
        webSocketTask?.resume()

        sendStompConnect()
        listenForMessages()
    }

    public func disconnect() {
        isIntentionallyClosed = true
        status = .disconnected
        reconnectTimer?.invalidate()
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        webSocketTask = nil
    }

    public func onMessage(handler: @escaping (String) -> Void) {
        messageHandlers.append(handler)
    }

    private func sendStompConnect() {
        let token = KeychainManager.shared.loadString(key: "access_token") ?? ""
        let connectFrame = "CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\nAuthorization:Bearer \(token)\n\n\0"
        sendMessage(connectFrame)
    }

    public func subscribe(destination: String, id: String = UUID().uuidString) {
        let subFrame = "SUBSCRIBE\nid:\(id)\ndestination:\(destination)\n\n\0"
        sendMessage(subFrame)
    }

    public func sendStompMessage(destination: String, body: String) {
        let frame = "SEND\ndestination:\(destination)\ncontent-type:application/json\n\n\(body)\0"
        sendMessage(frame)
    }

    private func sendMessage(_ message: String) {
        let wsMessage = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(wsMessage) { error in
            if let error = error {
                print("WebSocket send error: \(error.localizedDescription)")
            }
        }
    }

    private func listenForMessages() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }

            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.handleIncomingFrame(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.handleIncomingFrame(text)
                    }
                @unknown default:
                    break
                }
                self.listenForMessages()

            case .failure(let error):
                print("WebSocket receive error: \(error.localizedDescription)")
                self.handleDisconnection()
            }
        }
    }

    private func handleIncomingFrame(_ frame: String) {
        if frame.starts(with: "CONNECTED") {
            DispatchQueue.main.async {
                self.status = .connected
            }
            if let familyId = AppPreferences.shared.familyId {
                subscribe(destination: "/topic/family.\(familyId)")
            }
            subscribe(destination: "/queue/device.\(AppPreferences.shared.deviceUuid)")
        } else if frame.starts(with: "MESSAGE") {
            // Extract body after double newline
            let parts = frame.components(separatedBy: "\n\n")
            if parts.count > 1 {
                let body = parts[1].trimmingCharacters(in: CharacterSet(charactersIn: "\0\n\r "))
                DispatchQueue.main.async {
                    for handler in self.messageHandlers {
                        handler(body)
                    }
                }
            }
        }
    }

    private func handleDisconnection() {
        DispatchQueue.main.async {
            self.status = .reconnecting
        }
        guard !isIntentionallyClosed else { return }

        reconnectTimer?.invalidate()
        reconnectTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: false) { [weak self] _ in
            self?.connect()
        }
    }
}
