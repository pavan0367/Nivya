import React from 'react';

export type DatePreset = 'TODAY' | 'YESTERDAY' | 'WEEK' | 'CUSTOM';

interface DateSelectorProps {
  id?: string;
  selectedPreset: DatePreset;
  onPresetChange: (preset: DatePreset) => void;
  customDate?: string;
  onCustomDateChange?: (date: string) => void;
}

export const DateSelector: React.FC<DateSelectorProps> = ({
  id,
  selectedPreset,
  onPresetChange,
  customDate,
  onCustomDateChange,
}) => {
  return (
    <div
      id={id}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        background: 'rgba(255, 255, 255, 0.04)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        padding: '0.25rem',
        gap: '0.25rem',
      }}
    >
      <button
        type="button"
        className="btn btn-sm"
        style={{
          background: selectedPreset === 'TODAY' ? 'var(--primary)' : 'transparent',
          color: selectedPreset === 'TODAY' ? '#fff' : 'var(--text-muted)',
          padding: '0.35rem 0.75rem',
        }}
        onClick={() => onPresetChange('TODAY')}
      >
        Today
      </button>
      <button
        type="button"
        className="btn btn-sm"
        style={{
          background: selectedPreset === 'YESTERDAY' ? 'var(--primary)' : 'transparent',
          color: selectedPreset === 'YESTERDAY' ? '#fff' : 'var(--text-muted)',
          padding: '0.35rem 0.75rem',
        }}
        onClick={() => onPresetChange('YESTERDAY')}
      >
        Yesterday
      </button>
      <button
        type="button"
        className="btn btn-sm"
        style={{
          background: selectedPreset === 'WEEK' ? 'var(--primary)' : 'transparent',
          color: selectedPreset === 'WEEK' ? '#fff' : 'var(--text-muted)',
          padding: '0.35rem 0.75rem',
        }}
        onClick={() => onPresetChange('WEEK')}
      >
        Last 7 Days
      </button>

      {onCustomDateChange && (
        <input
          type="date"
          className="form-input"
          style={{
            padding: '0.25rem 0.5rem',
            fontSize: '0.8rem',
            width: '130px',
            background: selectedPreset === 'CUSTOM' ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
            borderColor: selectedPreset === 'CUSTOM' ? 'var(--primary)' : 'transparent',
          }}
          value={customDate || ''}
          onChange={(e) => {
            onPresetChange('CUSTOM');
            onCustomDateChange(e.target.value);
          }}
        />
      )}
    </div>
  );
};
