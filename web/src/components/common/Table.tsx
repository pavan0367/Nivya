import React from 'react';
import { EmptyState } from './EmptyState';
import { LoadingSpinner } from './LoadingState';

export interface Column<T> {
  key: string;
  header: string;
  render?: (item: T) => React.ReactNode;
  width?: string;
  align?: 'left' | 'center' | 'right';
}

interface TableProps<T> {
  id?: string;
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  emptyTitle?: string;
  emptyMessage?: string;
  page?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  keyExtractor: (item: T) => string | number;
}

export function DataTable<T>({
  id,
  columns,
  data,
  loading = false,
  emptyTitle = 'No records found',
  emptyMessage = 'There is currently no data matching your criteria.',
  page,
  totalPages,
  onPageChange,
  keyExtractor,
}: TableProps<T>) {
  if (loading) {
    return (
      <div style={{ padding: '3rem', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <LoadingSpinner text="Loading data..." />
      </div>
    );
  }

  if (data.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyMessage} />;
  }

  return (
    <div id={id} className="data-table-container">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                style={{
                  width: col.width,
                  textAlign: col.align || 'left',
                }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item) => (
            <tr key={keyExtractor(item)}>
              {columns.map((col) => (
                <td
                  key={col.key}
                  style={{
                    textAlign: col.align || 'left',
                  }}
                >
                  {col.render ? col.render(item) : (item as any)[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>

      {totalPages && totalPages > 1 && onPageChange && page !== undefined && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem 1.25rem',
            borderTop: '1px solid var(--border-subtle)',
            background: 'rgba(15, 23, 42, 0.4)',
          }}
        >
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Page {page + 1} of {totalPages}
          </span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="btn btn-secondary btn-sm"
              disabled={page <= 0}
              onClick={() => onPageChange(page - 1)}
            >
              Previous
            </button>
            <button
              className="btn btn-secondary btn-sm"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
