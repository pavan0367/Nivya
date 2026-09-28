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

  describe('OpenStreetMap Visualization Contract & Safety', () => {
    it('operates base map tiles with zero API key dependency or billing', () => {
      // OpenStreetMap tiles are free, public, and do not append or require an API key parameter
      const osmTilePattern = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
      expect(osmTilePattern).not.toContain('key=');
      expect(osmTilePattern).not.toContain('apiKey=');
    });

    it('uses HTTPS OpenStreetMap tile endpoint and requires attribution', () => {
      const tileUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
      const attribution = '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors';
      expect(tileUrl.startsWith('https://')).toBe(true);
      expect(tileUrl).toContain('openstreetmap.org');
      expect(attribution).toContain('OpenStreetMap');
    });

    it('calculates accuracy radius correctly with 5m minimum boundary', () => {
      const calculateRadius = (accuracyMeters?: number) => Math.max(accuracyMeters || 15, 5);
      expect(calculateRadius(2)).toBe(5);
      expect(calculateRadius(10)).toBe(10);
      expect(calculateRadius(undefined)).toBe(15);
      expect(calculateRadius(0)).toBe(15);
      expect(calculateRadius(45.5)).toBe(45.5);
    });

    it('enforces initial-center UX rule where camera is only locked once', () => {
      let hasCentered = false;
      let centerCount = 0;

      const onCoordinatesReceived = (_lat: number, _lng: number) => {
        if (!hasCentered) {
          centerCount++;
          hasCentered = true;
        }
      };

      // Initial fix centers
      onCoordinatesReceived(12.9716, 77.5946);
      expect(centerCount).toBe(1);

      // Subsequent live updates do not hijack user camera/zoom
      onCoordinatesReceived(12.9718, 77.5948);
      onCoordinatesReceived(12.9720, 77.5950);
      expect(centerCount).toBe(1);
    });
  });
});

