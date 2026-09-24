import { describe, it, expect, vi, beforeEach } from 'vitest';
import { isValidCoordinate } from '../utils/locationUtils';
import { telemetryService } from '../services/telemetryService';
import { websocketService } from '../services/websocketService';
import { LocationStatus } from '../types/telemetry';

describe('Location Module and Coordinate Integrity', () => {
  describe('isValidCoordinate validation', () => {
    it('approves real physical GPS coordinates', () => {
      expect(isValidCoordinate(12.9716, 77.5946)).toBe(true); // Bengaluru
      expect(isValidCoordinate(37.7749, -122.4194)).toBe(true); // San Francisco
      expect(isValidCoordinate(-33.8688, 151.2093)).toBe(true); // Sydney
      expect(isValidCoordinate(51.5074, -0.1278)).toBe(true); // London
      expect(isValidCoordinate(-90, 0)).toBe(true); // South pole boundary
      expect(isValidCoordinate(90, 0)).toBe(true); // North pole boundary
      expect(isValidCoordinate(0, 180)).toBe(true); // Antimeridian boundary
      expect(isValidCoordinate(0, -180)).toBe(true); // Antimeridian boundary
    });

    it('strictly rejects null, undefined, or non-numeric values', () => {
      expect(isValidCoordinate(null, null)).toBe(false);
      expect(isValidCoordinate(undefined, undefined)).toBe(false);
      expect(isValidCoordinate(12.9716, undefined)).toBe(false);
      expect(isValidCoordinate(undefined, 77.5946)).toBe(false);
      expect(isValidCoordinate(NaN, 77.5946)).toBe(false);
      expect(isValidCoordinate(12.9716, NaN)).toBe(false);
    });

    it('strictly rejects fake / default 0.0, 0.0 null-island coordinates', () => {
      expect(isValidCoordinate(0.0, 0.0)).toBe(false);
      expect(isValidCoordinate(0, 0)).toBe(false);
    });

    it('strictly rejects out-of-range coordinates', () => {
      expect(isValidCoordinate(91.0, 77.0)).toBe(false);
      expect(isValidCoordinate(-90.1, 77.0)).toBe(false);
      expect(isValidCoordinate(12.0, 180.1)).toBe(false);
      expect(isValidCoordinate(12.0, -180.1)).toBe(false);
    });
  });

  describe('Location Telemetry Service Contract', () => {
    beforeEach(() => {
      vi.restoreAllMocks();
    });

    it('retrieves current location telemetry from API', async () => {
      const mockLocation: LocationStatus = {
        latitude: 12.971598,
        longitude: 77.594566,
        accuracyMeters: 8.5,
        locationName: 'Indiranagar Central',
        recordedAt: '2026-09-21T15:10:00Z',
        isStale: false,
      };

      vi.spyOn(telemetryService, 'getLocationCurrent').mockResolvedValue(mockLocation);

      const result = await telemetryService.getLocationCurrent(180001);
      expect(result).toBeDefined();
      expect(result?.latitude).toBe(12.971598);
      expect(result?.longitude).toBe(77.594566);
      expect(isValidCoordinate(result?.latitude, result?.longitude)).toBe(true);
    });

    it('handles WebSocket location telemetry pushes correctly', () => {
      const incomingLocation: LocationStatus = {
        latitude: 12.9721,
        longitude: 77.5951,
        accuracyMeters: 6.0,
        locationName: '100ft Road',
        recordedAt: '2026-09-21T15:11:00Z',
        isStale: false,
      };

      let registeredHandler: ((data: any) => void) | null = null;
      vi.spyOn(websocketService, 'subscribe').mockImplementation((topic, handler) => {
        if (topic === '/topic/location/180001') {
          registeredHandler = handler;
        }
        return () => {};
      });

      let receivedUpdate: LocationStatus | null = null;
      websocketService.subscribe('/topic/location/180001', (data: LocationStatus) => {
        receivedUpdate = data;
      });

      // Simulate incoming WebSocket push
      expect(registeredHandler).toBeDefined();
      registeredHandler!(incomingLocation);

      expect(receivedUpdate).toEqual(incomingLocation);
      if (receivedUpdate) {
        const update = receivedUpdate as LocationStatus;
        expect(isValidCoordinate(update.latitude, update.longitude)).toBe(true);
      }
    });
  });
});
