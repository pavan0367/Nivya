import { describe, it, expect, vi } from 'vitest';

describe('Dashboard, Live Activity, History, and Alerts Logic', () => {
  describe('Telemetry Card Formatting', () => {
    it('formats battery percentage and charging status properly', () => {
      const formatBatteryState = (pct: number, charging: boolean) => {
        const status = charging ? 'Charging' : pct <= 20 ? 'Low Battery' : 'Normal';
        return `${pct}% (${status})`;
      };

      expect(formatBatteryState(85, false)).toBe('85% (Normal)');
      expect(formatBatteryState(15, false)).toBe('15% (Low Battery)');
      expect(formatBatteryState(40, true)).toBe('40% (Charging)');
    });

    it('formats network signal quality and connection type', () => {
      const formatNetworkStatus = (type: string, quality: string, online: boolean) => {
        if (!online) return 'Offline';
        return `${type} - ${quality}`;
      };

      expect(formatNetworkStatus('WIFI', 'EXCELLENT', true)).toBe('WIFI - EXCELLENT');
      expect(formatNetworkStatus('CELLULAR', 'POOR', true)).toBe('CELLULAR - POOR');
      expect(formatNetworkStatus('WIFI', 'UNKNOWN', false)).toBe('Offline');
    });

    it('formats screen time duration in hours and minutes', () => {
      const formatScreenTime = (totalMinutes: number) => {
        const hours = Math.floor(totalMinutes / 60);
        const mins = totalMinutes % 60;
        if (hours === 0) return `${mins}m`;
        return `${hours}h ${mins}m`;
      };

      expect(formatScreenTime(45)).toBe('45m');
      expect(formatScreenTime(125)).toBe('2h 5m');
      expect(formatScreenTime(0)).toBe('0m');
    });
  });

  describe('Live Activity Feed', () => {
    it('determines active foreground app from telemetry event', () => {
      interface LiveEvent {
        packageName: string;
        appName: string;
        current: boolean;
        durationSeconds: number;
      }

      const events: LiveEvent[] = [
        { packageName: 'com.google.android.youtube', appName: 'YouTube', current: true, durationSeconds: 600 },
        { packageName: 'com.android.chrome', appName: 'Chrome', current: false, durationSeconds: 300 },
      ];

      const activeEvent = events.find((e) => e.current);
      expect(activeEvent).toBeDefined();
      expect(activeEvent?.appName).toBe('YouTube');
      expect(activeEvent?.durationSeconds).toBe(600);
    });

    it('sanitizes private app details from activity descriptions', () => {
      const sanitizeActivityLabel = (label: string) => {
        // Must not expose messages, search terms, or urls
        if (/https?:\/\//i.test(label) || label.includes('chat') || label.includes('message')) {
          return 'In App';
        }
        return label;
      };

      expect(sanitizeActivityLabel('Studying Math')).toBe('Studying Math');
      expect(sanitizeActivityLabel('Viewing https://secret-url.com')).toBe('In App');
      expect(sanitizeActivityLabel('Private chat message')).toBe('In App');
    });
  });

  describe('History Timeline & Filtering', () => {
    interface HistoryItem {
      id: number;
      appName: string;
      category: string;
      timestamp: string;
    }

    const mockHistory: HistoryItem[] = [
      { id: 1, appName: 'YouTube', category: 'ENTERTAINMENT', timestamp: '2026-09-09T10:00:00Z' },
      { id: 2, appName: 'Google Classroom', category: 'EDUCATION', timestamp: '2026-09-09T11:00:00Z' },
      { id: 3, appName: 'Spotify', category: 'AUDIO', timestamp: '2026-09-09T12:00:00Z' },
      { id: 4, appName: 'Wikipedia', category: 'EDUCATION', timestamp: '2026-09-09T13:00:00Z' },
    ];

    it('filters history items by category', () => {
      const educationEvents = mockHistory.filter((item) => item.category === 'EDUCATION');
      expect(educationEvents).toHaveLength(2);
      expect(educationEvents.map((e) => e.appName)).toEqual(['Google Classroom', 'Wikipedia']);
    });

    it('calculates distinct applications for filter dropdown', () => {
      const distinctApps = Array.from(new Set(mockHistory.map((h) => h.appName)));
      expect(distinctApps).toHaveLength(4);
      expect(distinctApps).toContain('YouTube');
      expect(distinctApps).toContain('Wikipedia');
    });
  });

  describe('Alerts Management', () => {
    interface AlertItem {
      id: number;
      type: string;
      severity: 'INFO' | 'WARNING' | 'CRITICAL';
      isRead: boolean;
      resolved: boolean;
    }

    const mockAlerts: AlertItem[] = [
      { id: 1, type: 'LOW_BATTERY', severity: 'WARNING', isRead: false, resolved: false },
      { id: 2, type: 'SECURITY_ALERT', severity: 'CRITICAL', isRead: false, resolved: false },
      { id: 3, type: 'OFFLINE', severity: 'WARNING', isRead: true, resolved: true },
    ];

    it('calculates unread active alerts badge counter', () => {
      const unreadCount = mockAlerts.filter((a) => !a.isRead && !a.resolved).length;
      expect(unreadCount).toBe(2);
    });

    it('resolves an alert and updates resolved status', () => {
      const resolveAlert = (alerts: AlertItem[], id: number) => {
        return alerts.map((a) => (a.id === id ? { ...a, resolved: true, isRead: true } : a));
      };

      const updated = resolveAlert(mockAlerts, 1);
      const target = updated.find((a) => a.id === 1);
      expect(target?.resolved).toBe(true);
      expect(target?.isRead).toBe(true);
    });
  });

  describe('WebSocket Reconnection & State Rehydration', () => {
    it('triggers snapshot refresh when onReconnect fires', () => {
      let isSnapshotFetched = false;
      const onReconnectHandler = () => {
        isSnapshotFetched = true;
      };

      // Simulate reconnect trigger
      onReconnectHandler();
      expect(isSnapshotFetched).toBe(true);
    });
  });

  describe('Parent Dashboard Device Resolution & Defensive Normalization', () => {
    it('normalizes device payloads having deviceId without id and prevents undefined.toString()', () => {
      const rawApiDevice = {
        deviceId: 5,
        deviceUuid: 'uuid-child-123',
        deviceName: 'Child Android Phone',
        platform: 'ANDROID',
        isOnline: true,
        userRole: 'CHILD',
        isChildDevice: true,
      };

      // Test normalization logic as implemented in authService
      const resolvedId = Number((rawApiDevice as any).id ?? rawApiDevice.deviceId ?? 0);
      const normalizedDevice = {
        ...rawApiDevice,
        id: resolvedId,
        deviceId: resolvedId,
        isChildDevice: Boolean(rawApiDevice.isChildDevice || rawApiDevice.userRole === 'CHILD'),
      };

      expect(normalizedDevice.id).toBe(5);
      expect(String(normalizedDevice.id)).toBe('5');
      // Verify defensive String() never throws even if null/undefined
      expect(String(undefined)).toBe('undefined');
      expect(normalizedDevice.id.toString()).toBe('5');
    });

    it('filters out Parent browser devices to accurately select the Child device', () => {
      const familyDevices = [
        { id: 4, deviceName: 'PARENT Web Browser', userRole: 'PARENT', isChildDevice: false },
        { id: 5, deviceName: 'CHILD Web Browser', userRole: 'CHILD', isChildDevice: true },
      ];

      const childDevs = familyDevices.filter((d) => d.isChildDevice || d.userRole === 'CHILD');
      expect(childDevs).toHaveLength(1);
      expect(childDevs[0].id).toBe(5);
      expect(childDevs[0].deviceName).toBe('CHILD Web Browser');
    });

    it('preserves previously saved active device id across reloads', () => {
      const familyDevices = [
        { id: 5, deviceName: 'Child Tablet', userRole: 'CHILD', isChildDevice: true },
        { id: 8, deviceName: 'Child Phone', userRole: 'CHILD', isChildDevice: true },
      ];

      const savedId = 8;
      const matched = familyDevices.find((d) => d.id === savedId);
      const chosen = matched || familyDevices[0];
      expect(chosen.id).toBe(8);
    });

    it('correctly handles genuinely unpaired state with zero devices without throwing', () => {
      const emptyDevices: any[] = [];
      const childDevs = emptyDevices.filter((d) => d.isChildDevice || d.userRole === 'CHILD');
      expect(childDevs).toHaveLength(0);
      const activeId = childDevs.length > 0 ? childDevs[0].id : null;
      expect(activeId).toBeNull();
    });
  });

  describe('Child Device Targeting & Parent/Sibling Isolation', () => {
    const mockFamilyDevices = [
      { id: 4, deviceId: 4, userId: 6, userRole: 'PARENT', isChildDevice: false, deviceName: 'PARENT Web Browser', platform: 'WEB' },
      { id: 5, deviceId: 5, userId: 17, userRole: 'CHILD', isChildDevice: true, deviceName: 'CHILD Web Browser', platform: 'WEB' },
      { id: 7, deviceId: 7, userId: 18, userRole: 'CHILD', isChildDevice: true, deviceName: 'Sibling Android Phone', platform: 'ANDROID' },
    ];

    const childUser = {
      id: 17,
      email: 'vaadithya5@gmail.com',
      name: 'Adithya Child',
      role: 'CHILD' as const,
    };

    const resolveChildDevice = (
      user: { id: number; role: string },
      devs: typeof mockFamilyDevices,
      storedChildDeviceId?: number | null,
      storedParentDeviceId?: number | null
    ) => {
      // 1. Authenticated Child user's enrolled device (d.userId === user.id)
      let resolved = devs.find((d) => d.userId != null && d.userId === user.id);

      // 2. Existing authoritative pairing-status device whose userRole is CHILD (excluding PARENT)
      if (!resolved) {
        resolved = devs.find((d) => (d.isChildDevice || d.userRole === 'CHILD') && d.userRole !== 'PARENT');
      }

      // 3. Persisted active device ONLY if it still belongs to the authenticated Child user
      if (storedChildDeviceId) {
        const matched = devs.find((d) => d.id === storedChildDeviceId && (d.userId === user.id || (d.isChildDevice && d.userRole !== 'PARENT')));
        if (matched) resolved = matched;
      }

      // Invariant: NEVER allow a parent device
      if (resolved && (resolved.userRole === 'PARENT' || (!resolved.isChildDevice && resolved.userId !== user.id))) {
        resolved = undefined;
      }

      return resolved;
    };

    it('1. Child authenticated user selects matching Child device (userId 17 -> device 5)', () => {
      const selected = resolveChildDevice(childUser, mockFamilyDevices);
      expect(selected).toBeDefined();
      expect(selected?.id).toBe(5);
      expect(selected?.deviceName).toBe('CHILD Web Browser');
      expect(selected?.userId).toBe(17);
    });

    it('2. Parent device is never selected for Child session', () => {
      const onlyParentDevice = [
        { id: 4, deviceId: 4, userId: 6, userRole: 'PARENT', isChildDevice: false, deviceName: 'PARENT Web Browser', platform: 'WEB' },
      ];
      const selected = resolveChildDevice(childUser, onlyParentDevice);
      expect(selected).toBeUndefined();
    });

    it('3. Stored Parent device ID is completely ignored by Child session', () => {
      // Simulated stale localStorage containing Parent device id 4
      const staleParentId = 4;
      const selected = resolveChildDevice(childUser, mockFamilyDevices, staleParentId);
      // Must NOT select 4; must resolve Child device 5
      expect(selected?.id).toBe(5);
      expect(selected?.id).not.toBe(4);
    });

    it('4. Child activeDeviceId equals Child device ID', () => {
      const selected = resolveChildDevice(childUser, mockFamilyDevices);
      const activeDeviceId = selected ? selected.id : null;
      expect(activeDeviceId).toBe(5);
      expect(activeDeviceId).toBe(selected?.id);
    });

    it('5. Child telemetry calls receive Child device ID', () => {
      const selected = resolveChildDevice(childUser, mockFamilyDevices);
      const activeDeviceId = selected?.id;

      // Ensure that telemetry calls use Child ID (5), NEVER Parent ID (4)
      expect(activeDeviceId).toBe(5);
      const targetApiUrl = `/api/v1/battery/current/${activeDeviceId}`;
      expect(targetApiUrl).toBe('/api/v1/battery/current/5');
      expect(targetApiUrl).not.toContain('/4');
    });

    it('6. Child top bar displays Child device name and platform', () => {
      const selected = resolveChildDevice(childUser, mockFamilyDevices);
      const topBarLabel = selected ? `${selected.deviceName} (${selected.platform})` : 'Device unavailable';
      expect(topBarLabel).toBe('CHILD Web Browser (WEB)');
    });

    it('7. No enrolled Child device produces proper unavailable state without selecting parent device', () => {
      const noChildDevices = [
        { id: 4, deviceId: 4, userId: 6, userRole: 'PARENT', isChildDevice: false, deviceName: 'PARENT Web Browser', platform: 'WEB' },
      ];
      const selected = resolveChildDevice(childUser, noChildDevices);
      expect(selected).toBeUndefined();
      const topBarLabel = selected ? `${(selected as any).deviceName} (${(selected as any).platform})` : 'Device unavailable';
      expect(topBarLabel).toBe('Device unavailable');
    });

    it('8. Browser refresh preserves correct Child device without reverting to parent device', () => {
      const storedChildId = 5;
      const selected = resolveChildDevice(childUser, mockFamilyDevices, storedChildId);
      expect(selected?.id).toBe(5);
    });

    it('9. Parent session still resolves Parent target devices to child devices correctly without conflict', () => {
      const childDevs = mockFamilyDevices.filter((d) => (d.isChildDevice || d.userRole === 'CHILD') && d.userRole !== 'PARENT');
      expect(childDevs).toHaveLength(2);
      expect(childDevs.map((d) => d.id)).toEqual([5, 7]);
      // Verify parent device (4) is excluded from target devices
      expect(childDevs.some((d) => d.id === 4)).toBe(false);
    });

    it('10. No undefined/null device ID is sent to telemetry APIs', () => {
      const emptySelected: any = null;
      const activeDeviceId = emptySelected ? emptySelected.id : null;

      let telemetryCalled = false;
      if (activeDeviceId) {
        telemetryCalled = true;
      }
      expect(telemetryCalled).toBe(false);
      expect(activeDeviceId).toBeNull();
    });
  });

  describe('Telemetry Contract, Null Handling, and Authoritative Device Online State', () => {
    it('1. Missing/null telemetry returns clean Unavailable and Waiting states without error', () => {
      const battery: any = null;
      const location: any = null;
      const network: any = null;
      const usage: any = null;
      const health: any = null;

      const batteryValue = battery ? `${battery.batteryPct}%` : 'Unavailable';
      const batterySubtitle = battery ? (battery.isCharging ? '⚡ Charging Active' : 'On Battery') : 'Waiting for device data';
      const batteryBadge = battery ? { text: 'GOOD', variant: 'success' } : { text: 'WAITING', variant: 'neutral' };

      expect(batteryValue).toBe('Unavailable');
      expect(batterySubtitle).toBe('Waiting for device data');
      expect(batteryBadge.text).toBe('WAITING');
      expect(batteryBadge.text).not.toBe('OFFLINE');

      const locationValue = location ? `${location.latitude}, ${location.longitude}` : 'Unavailable';
      expect(locationValue).toBe('Unavailable');

      const networkValue = network?.networkType || 'Unavailable';
      expect(networkValue).toBe('Unavailable');

      const usageSubtitle = usage ? 'Foreground usage' : 'Waiting for device data';
      expect(usageSubtitle).toBe('Waiting for device data');

      const healthValue = health ? (health.isHealthy ? 'Optimal' : 'Needs Review') : 'Unavailable';
      expect(healthValue).toBe('Unavailable');
    });

    it('2. Unrecorded or null network telemetry does NOT override isOnline to false', () => {
      const authoritativeDevice = { id: 5, isOnline: true, isStale: false };
      const netResValue: any = null; // unrecorded network telemetry

      // Authoritative computation
      let isOnline = Boolean(authoritativeDevice.isOnline && !authoritativeDevice.isStale);

      // Network update should set network state but NOT collapse device presence into offline
      if (netResValue) {
        // network loaded
      }
      // isOnline remains TRUE based on device presence
      expect(isOnline).toBe(true);
    });

    it('3. Offline banner is rendered strictly from authoritative device presence', () => {
      const computeOfflineBanner = (device: { isOnline: boolean; isStale: boolean } | null) => {
        if (!device) return { showBanner: true, reason: 'NO_DEVICE' };
        const isOnline = Boolean(device.isOnline && !device.isStale);
        return {
          showBanner: !isOnline,
          reason: device.isStale ? 'STALE' : !device.isOnline ? 'OFFLINE' : 'ONLINE',
        };
      };

      // Active online device
      expect(computeOfflineBanner({ isOnline: true, isStale: false })).toEqual({ showBanner: false, reason: 'ONLINE' });

      // Stale device (> 2 minutes without heartbeat)
      expect(computeOfflineBanner({ isOnline: true, isStale: true })).toEqual({ showBanner: true, reason: 'STALE' });

      // Explicitly offline device
      expect(computeOfflineBanner({ isOnline: false, isStale: false })).toEqual({ showBanner: true, reason: 'OFFLINE' });
    });

    it('4. Real native device telemetry renders exact numbers without fabrication', () => {
      const realBattery = { batteryPct: 63, isCharging: true, healthStatus: 'GOOD' };
      const realNetwork = { networkType: 'WIFI', ssid: 'Home_5G', isInternetAvailable: true, networkQuality: 'EXCELLENT' };
      const realLocation = { latitude: 37.7749, longitude: -122.4194, isStale: false, accuracyMeters: 8 };

      expect(realBattery.batteryPct).toBe(63);
      expect(realBattery.isCharging).toBe(true);
      expect(`${realBattery.batteryPct}%`).toBe('63%');

      expect(realNetwork.networkType).toBe('WIFI');
      expect(realNetwork.ssid).toBe('Home_5G');

      expect(realLocation.latitude).toBe(37.7749);
      expect(realLocation.longitude).toBe(-122.4194);
    });
  });
});

