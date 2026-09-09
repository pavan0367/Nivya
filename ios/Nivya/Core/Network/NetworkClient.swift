import Foundation

public final class NetworkClient {
    public static let shared = NetworkClient()
    private let session: URLSession

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)
    }

    public func request<T: Codable>(
        path: String,
        method: String = "GET",
        body: Data? = nil,
        requiresAuth: Bool = true
    ) async throws -> T {
        let url = APIEndpoint.url(for: path)
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if requiresAuth, let token = KeychainManager.shared.loadString(key: "access_token") {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            req.httpBody = body
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: req)
        } catch {
            throw NetworkError.offline
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.noData
        }

        if httpResponse.statusCode == 401 {
            throw NetworkError.unauthorized
        }

        if httpResponse.statusCode == 403 {
            throw NetworkError.forbidden
        }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        if (200...299).contains(httpResponse.statusCode) {
            do {
                let apiResponse = try decoder.decode(ApiResponse<T>.self, from: data)
                if let payload = apiResponse.data {
                    return payload
                }
                // If T is Void or EmptyResponse
                if let empty = () as? T {
                    return empty
                }
                throw NetworkError.noData
            } catch {
                // Try direct decode as fallback
                do {
                    return try decoder.decode(T.self, from: data)
                } catch {
                    throw NetworkError.decodingError(error)
                }
            }
        } else {
            if let apiResponse = try? decoder.decode(ApiResponse<T>.self, from: data),
               let msg = apiResponse.message ?? apiResponse.error {
                throw NetworkError.serverError(code: httpResponse.statusCode, message: msg)
            }
            throw NetworkError.serverError(code: httpResponse.statusCode, message: "HTTP \(httpResponse.statusCode)")
        }
    }

    public func get<T: Codable>(path: String, requiresAuth: Bool = true) async throws -> T {
        return try await request(path: path, method: "GET", requiresAuth: requiresAuth)
    }

    public func post<T: Codable, B: Encodable>(path: String, body: B, requiresAuth: Bool = true) async throws -> T {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(body)
        return try await request(path: path, method: "POST", body: data, requiresAuth: requiresAuth)
    }

    public func postEmpty<T: Codable>(path: String, requiresAuth: Bool = true) async throws -> T {
        return try await request(path: path, method: "POST", body: nil, requiresAuth: requiresAuth)
    }

    public func put<T: Codable, B: Encodable>(path: String, body: B, requiresAuth: Bool = true) async throws -> T {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(body)
        return try await request(path: path, method: "PUT", body: data, requiresAuth: requiresAuth)
    }

    public func delete<T: Codable>(path: String, requiresAuth: Bool = true) async throws -> T {
        return try await request(path: path, method: "DELETE", requiresAuth: requiresAuth)
    }
}
