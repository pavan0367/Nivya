import React from 'react';

interface OfflineIndicatorProps {
  id?: string;
  isOnline: boolean;
  deviceName?: string;
  lastSyncAt?: string;
  banner?: boolean;
}

export const OfflineIndicator: React.FC<OfflineIndicatorProps> = ({
  id,
  isOnline,
  deviceName = 'Child device',
  lastSyncAt,
  banner = false,
}) => {
  if (banner && !isOnline) {
    return (
      <div
        id={id}
        style={{
          padding: '0.85rem 1.25rem',
          borderRadius: 'var(--radius-md)',
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid rgba(239, 68, 68, 0.35)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
          <span className="pulse-dot offline" />
          <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--danger)' }}>
            {deviceName} is currently OFFLINE
          </span>
          {lastSyncAt && (
            <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              (Last seen: {new Date(lastSyncAt).toLocaleTimeString()})
            </span>
          )}
        </div>
        <span className="badge badge-danger">Disconnected</span>
      </div>
    );
  }

  return (
    <span id={id} className={`badge ${isOnline ? 'badge-success' : 'badge-danger'}`}>
      <span className={`pulse-dot ${isOnline ? 'online' : 'offline'}`} style={{ marginRight: '4px' }} />
      {isOnline ? 'Online' : 'Offline'}
    </span>
  );
};
