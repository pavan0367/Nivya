import React from 'react';

interface StaleIndicatorProps {
  id?: string;
  isStale: boolean;
  recordedAt?: string;
}

export const StaleIndicator: React.FC<StaleIndicatorProps> = ({ id, isStale, recordedAt }) => {
  if (!isStale) {
    return null;
  }

  return (
    <span
      id={id}
      className="badge badge-warning"
      title={recordedAt ? `Last telemetry sync: ${new Date(recordedAt).toLocaleString()}` : 'Telemetry is stale'}
    >
      ⏱️ Stale Telemetry
    </span>
  );
};
