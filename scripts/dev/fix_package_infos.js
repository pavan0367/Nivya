const fs = require('fs');
const path = require('path');

const packages = {
    "auth": "Authentication, registration, JWT generation, token rotation, and password management.",
    "user": "User identity, profiles, credentials, and account lifecycle.",
    "role": "Role definitions, permissions, and hierarchical RBAC guards (PARENT, CHILD).",
    "pairing": "Consent-based one-time connection-code generation, verification, and family link creation.",
    "family": "Family group management, member association, and ownership validation.",
    "device": "Device registration, hardware fingerprinting, sessions, and heartbeat state.",
    "battery": "Battery telemetry ingestion, charging status, health metrics, and battery history.",
    "network": "Network connectivity telemetry, Wi-Fi/cellular state, signal quality, and internet availability.",
    "location": "Geolocation updates, last known location snapshot, reverse geocoding, and 30-day location history.",
    "usage": "Android UsageStats synchronization, app duration aggregation, and screen time reporting.",
    "activity": "Live high-level app and communication activity status (Parent-only, privacy-preserving).",
    "history": "Chronological device activity history timeline and event durations (Parent-only).",
    "communication": "High-level call metadata, call counts, and contact labels without recording audio or private content.",
    "alerts": "Threshold rules evaluation, system health notifications, and safety alerts.",
    "notification": "Push notification delivery (FCM) and generic parent-guidance notification dispatching.",
    "convocation": "Independent family guidance messaging, 2-minute auto-hide viewing mode, and 1-hour expiry engine.",
    "consent": "Consent tracking, terms of service acceptance, and legally auditable parental agreements.",
    "privacy": "Privacy policies, data sharing controls, and boundary enforcement.",
    "audit": "Security-sensitive event logging, access tracking, and system audit trails.",
    "websocket": "WebSocket broker, STOMP endpoint registration, and Redis-backed real-time messaging.",
    "security": "Spring Security filters, JWT authentication token provider, and authorization guards.",
    "common": "Global response wrappers, exception handling advice, system health, and cross-cutting utilities."
};

const baseDir = path.join(__dirname, '../../backend/src/main/java/com/nivya');

for (const [pkg, doc] of Object.entries(packages)) {
    const pkgDir = path.join(baseDir, pkg);
    if (!fs.existsSync(pkgDir)) {
        fs.mkdirSync(pkgDir, { recursive: true });
    }
    const filePath = path.join(pkgDir, 'package-info.java');
    const content = `/**\n * Nivya Module: com.nivya.${pkg}\n * <p>\n * ${doc}\n * </p>\n */\npackage com.nivya.${pkg};\n`;
    fs.writeFileSync(filePath, content, { encoding: 'utf8' }); // Node.js writes utf-8 without BOM by default!
    console.log(`Updated ${filePath} without BOM`);
}
