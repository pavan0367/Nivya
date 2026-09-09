import {
  HistoryEventDetail,
  HistoryFilterParams,
  HistoryPageResponse,
} from '../types/history';

const BASE_URL = '/api/v1/history';

/**
 * Service providing API client calls for Nivya Parent-Only History
 */
export const historyService = {
  /**
   * Retrieves paginated chronological activity history with optional date and app filters.
   * Restricted strictly to Parent role.
   */
  async getHistory(params: HistoryFilterParams, token: string): Promise<HistoryPageResponse> {
    const query = new URLSearchParams();
    query.set('deviceId', params.deviceId.toString());
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.startDate) query.set('startDate', params.startDate);
    if (params.endDate) query.set('endDate', params.endDate);
    if (params.application) query.set('application', params.application);

    const res = await fetch(`${BASE_URL}?${query.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      if (res.status === 403) {
        throw new Error('Access denied: History is restricted to Parent accounts only.');
      }
      throw new Error(`Failed to fetch activity history: ${res.statusText}`);
    }

    const json = await res.json();
    return json.data;
  },

  /**
   * Retrieves granular details of a specific history event within consented scope.
   */
  async getHistoryEventDetail(
    deviceId: number,
    eventId: number,
    token: string
  ): Promise<HistoryEventDetail> {
    const res = await fetch(`${BASE_URL}/${eventId}?deviceId=${deviceId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      if (res.status === 403) {
        throw new Error('Access denied: History details are restricted to Parent accounts.');
      }
      throw new Error(`Failed to fetch history details: ${res.statusText}`);
    }

    const json = await res.json();
    return json.data;
  },

  /**
   * Retrieves distinct applications recorded for the specified device.
   */
  async getDistinctApplications(deviceId: number, token: string): Promise<string[]> {
    const res = await fetch(`${BASE_URL}/applications?deviceId=${deviceId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      if (res.status === 403) {
        throw new Error('Access denied: Application list is restricted to Parent accounts.');
      }
      throw new Error(`Failed to fetch applications: ${res.statusText}`);
    }

    const json = await res.json();
    return json.data || [];
  },
};
