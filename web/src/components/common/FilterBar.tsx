import React from 'react';

interface FilterOption {
  value: string;
  label: string;
  badge?: number;
}

interface FilterBarProps {
  id?: string;
  options: FilterOption[];
  selected: string;
  onSelect: (value: string) => void;
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (val: string) => void;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  id,
  options,
  selected,
  onSelect,
  searchPlaceholder = 'Search...',
  searchValue,
  onSearchChange,
}) => {
  return (
    <div
      id={id}
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '0.75rem',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '1.5rem',
      }}
    >
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
        {options.map((opt) => {
          const isActive = selected === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              onClick={() => onSelect(opt.value)}
              className="btn btn-sm"
              style={{
                borderRadius: '9999px',
                padding: '0.35rem 0.85rem',
                fontSize: '0.825rem',
                background: isActive ? 'var(--primary)' : 'rgba(255, 255, 255, 0.06)',
                color: isActive ? '#fff' : 'var(--text-muted)',
                borderColor: isActive ? 'transparent' : 'var(--border-subtle)',
                fontWeight: isActive ? 600 : 400,
              }}
            >
              {opt.label}
              {opt.badge !== undefined && (
                <span
                  style={{
                    marginLeft: '0.35rem',
                    padding: '0.1rem 0.4rem',
                    borderRadius: '9999px',
                    fontSize: '0.7rem',
                    background: isActive ? 'rgba(255, 255, 255, 0.25)' : 'rgba(255, 255, 255, 0.1)',
                  }}
                >
                  {opt.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {onSearchChange && (
        <div style={{ minWidth: '220px' }}>
          <input
            type="text"
            className="form-input"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
            placeholder={searchPlaceholder}
            value={searchValue || ''}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
      )}
    </div>
  );
};
