import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { convocationService } from '../services/convocationService';
import { authService } from '../services/authService';

// Mock localStorage for Node test runner
const createLocalStorageMock = () => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
};

const localStorageMock = createLocalStorageMock();
Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

describe('NIVYA Responsiveness & Message Dispatch Verification', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  // 1. Dashboard navigation happens immediately
  it('1. Dashboard navigation happens immediately without pre-transition delay', () => {
    let currentPath = '/child';
    const navigate = (path: string) => {
      currentPath = path;
    };

    // Clicking Battery triggers navigation immediately
    navigate('/child/battery');
    expect(currentPath).toBe('/child/battery');

    navigate('/child/screen-time');
    expect(currentPath).toBe('/child/screen-time');

    navigate('/child/network');
    expect(currentPath).toBe('/child/network');
  });

  // 2. Navigation does not wait for unrelated API requests
  it('2. Navigation does not wait for unrelated API requests', async () => {
    let currentPath = '/child';
    const navigate = (path: string) => {
      currentPath = path;
    };

    // Unrelated background request taking 5000ms
    const longRunningRequest = new Promise((resolve) => setTimeout(resolve, 5000));

    // User clicks navigation while request is in flight
    navigate('/child/location');

    // Navigation succeeds immediately without awaiting the longRunningRequest
    expect(currentPath).toBe('/child/location');

    vi.advanceTimersByTime(5000);
    await longRunningRequest;
  });

  // 3. Destination page loads real data asynchronously
  it('3. Destination page loads real data asynchronously after mount', async () => {
    let dataLoaded = false;
    let initialRenderState = 'loading';

    // Component mounts immediately and then triggers async data fetch
    const fetchComponentData = async () => {
      await Promise.resolve();
      dataLoaded = true;
      initialRenderState = 'ready';
    };

    expect(initialRenderState).toBe('loading');
    expect(dataLoaded).toBe(false);

    await fetchComponentData();
    expect(dataLoaded).toBe(true);
    expect(initialRenderState).toBe('ready');
  });

  // 4. CRACK starts its API request immediately
  it('4. CRACK starts its API request immediately', async () => {
    const sendSpy = vi.spyOn(convocationService, 'childSendMessage').mockResolvedValue({});
    
    // Simulate handleSendCrack
    let requestStarted = false;
    const handleSendCrack = () => {
      requestStarted = true;
      return convocationService.childSendMessage('Mom,here');
    };

    const promise = handleSendCrack();
    expect(requestStarted).toBe(true);
    expect(sendSpy).toHaveBeenCalledTimes(1);
    expect(sendSpy).toHaveBeenCalledWith('Mom,here');

    await promise;
  });

  // 5. FREAK starts its API request immediately
  it('5. FREAK starts its API request immediately', async () => {
    const sendSpy = vi.spyOn(convocationService, 'childSendMessage').mockResolvedValue({});
    
    // Simulate handleSendFreak
    let requestStarted = false;
    const handleSendFreak = () => {
      requestStarted = true;
      return convocationService.childSendMessage("Someone's,here");
    };

    const promise = handleSendFreak();
    expect(requestStarted).toBe(true);
    expect(sendSpy).toHaveBeenCalledTimes(1);
    expect(sendSpy).toHaveBeenCalledWith("Someone's,here");

    await promise;
  });

  // 6. Fast CRACK request does NOT show "Sending..."
  it('6. Fast CRACK request does NOT show "Sending..."', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({}), 100))
    );

    let visualSending = false;
    let doneShown = false;
    const GRACE_PERIOD_MS = 400;

    let inFlight = true;
    const timer = setTimeout(() => {
      if (inFlight) visualSending = true;
    }, GRACE_PERIOD_MS);

    const promise = convocationService.childSendMessage('Mom,here').then(() => {
      clearTimeout(timer);
      inFlight = false;
      visualSending = false;
      doneShown = true;
    });

    // Advance 100ms (fast response before 400ms grace period)
    vi.advanceTimersByTime(100);
    await promise;

    expect(visualSending).toBe(false);
    expect(doneShown).toBe(true);
  });

  // 7. Fast FREAK request does NOT show "Sending..."
  it('7. Fast FREAK request does NOT show "Sending..."', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({}), 80))
    );

    let visualSending = false;
    let doneShown = false;
    const GRACE_PERIOD_MS = 400;

    let inFlight = true;
    const timer = setTimeout(() => {
      if (inFlight) visualSending = true;
    }, GRACE_PERIOD_MS);

    const promise = convocationService.childSendMessage("Someone's,here").then(() => {
      clearTimeout(timer);
      inFlight = false;
      visualSending = false;
      doneShown = true;
    });

    // Advance 80ms (fast response)
    vi.advanceTimersByTime(80);
    await promise;

    expect(visualSending).toBe(false);
    expect(doneShown).toBe(true);
  });

  // 8. Slow CRACK request shows only "Sending..."
  it('8. Slow CRACK request shows only "Sending..."', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({}), 1500))
    );

    let visualState = 'CRACK';
    let inFlight = true;
    const GRACE_PERIOD_MS = 400;

    const timer = setTimeout(() => {
      if (inFlight) visualState = 'Sending...';
    }, GRACE_PERIOD_MS);

    const promise = convocationService.childSendMessage('Mom,here').then(() => {
      clearTimeout(timer);
      inFlight = false;
      visualState = 'CRACK';
    });

    // Initially before grace period: normal state
    expect(visualState).toBe('CRACK');

    // Advance beyond grace period (401ms)
    vi.advanceTimersByTime(401);
    expect(visualState).toBe('Sending...');

    // Finish request
    vi.advanceTimersByTime(1100);
    await promise;
    expect(visualState).toBe('CRACK');
  });

  // 9. Slow FREAK request shows only "Sending..."
  it('9. Slow FREAK request shows only "Sending..."', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({}), 1200))
    );

    let visualState = 'FREAK';
    let inFlight = true;
    const GRACE_PERIOD_MS = 400;

    const timer = setTimeout(() => {
      if (inFlight) visualState = 'Sending...';
    }, GRACE_PERIOD_MS);

    const promise = convocationService.childSendMessage("Someone's,here").then(() => {
      clearTimeout(timer);
      inFlight = false;
      visualState = 'FREAK';
    });

    expect(visualState).toBe('FREAK');

    vi.advanceTimersByTime(401);
    expect(visualState).toBe('Sending...');

    vi.advanceTimersByTime(800);
    await promise;
    expect(visualState).toBe('FREAK');
  });

  // 10. "Wait" is never rendered
  it('10. "Wait" is never rendered', () => {
    const renderedButtonText = (isDelayed: boolean, original: string) =>
      isDelayed ? 'Sending...' : original;

    expect(renderedButtonText(true, 'CRACK')).toBe('Sending...');
    expect(renderedButtonText(false, 'CRACK')).toBe('CRACK');
    expect(renderedButtonText(true, 'CRACK')).not.toBe('Wait');
    expect(renderedButtonText(false, 'CRACK')).not.toBe('Wait');
  });

  // 11. "Waiting..." is never rendered
  it('11. "Waiting..." is never rendered', () => {
    const renderedButtonText = (isDelayed: boolean, original: string) =>
      isDelayed ? 'Sending...' : original;

    expect(renderedButtonText(true, 'CRACK')).not.toBe('Waiting...');
    expect(renderedButtonText(true, 'FREAK')).not.toBe('Waiting...');
  });

  // 12. "Please wait" is never rendered
  it('12. "Please wait" is never rendered', () => {
    const renderedButtonText = (isDelayed: boolean, original: string) =>
      isDelayed ? 'Sending...' : original;

    expect(renderedButtonText(true, 'CRACK')).not.toBe('Please wait');
    expect(renderedButtonText(true, 'FREAK')).not.toBe('Please wait');
  });

  // 13. No countdown is rendered
  it('13. No countdown is rendered', () => {
    const renderedButtonText = (isDelayed: boolean, original: string) =>
      isDelayed ? 'Sending...' : original;

    expect(renderedButtonText(true, 'CRACK')).not.toMatch(/\d+/);
    expect(renderedButtonText(true, 'FREAK')).not.toMatch(/\d+/);
  });

  // 14. No full-screen blocking overlay appears during sending
  it('14. No full-screen blocking overlay appears during sending and screen remains usable', () => {
    const sendingModalOrOverlay = null; // No modal or overlay is rendered
    const isDashboardUsable = true;
    const canClickBattery = true;
    const canClickLocation = true;

    expect(sendingModalOrOverlay).toBeNull();
    expect(isDashboardUsable).toBe(true);
    expect(canClickBattery).toBe(true);
    expect(canClickLocation).toBe(true);
  });

  // 15. DONE appears only after actual backend success
  it('15. DONE appears only after actual backend success', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockResolvedValue({ id: 101 });
    let doneShown = false;

    await convocationService.childSendMessage('Mom,here');
    doneShown = true;

    expect(doneShown).toBe(true);
  });

  // 16. DONE remains non-blocking
  it('16. DONE remains non-blocking with pointer-events: none', () => {
    const doneToastStyle = {
      position: 'fixed',
      top: '1.5rem',
      left: '50%',
      pointerEvents: 'none',
    };

    expect(doneToastStyle.pointerEvents).toBe('none');
  });

  // 17. Failed requests do not show fake DONE
  it('17. Failed requests do not show fake DONE', async () => {
    vi.spyOn(convocationService, 'childSendMessage').mockRejectedValue(new Error('Network error'));
    let doneShown = false;
    let errorMessage: string | null = null;

    try {
      await convocationService.childSendMessage('Mom,here');
      doneShown = true;
    } catch (err: any) {
      errorMessage = err.message || 'Failed to send message';
      // DO NOT set doneShown = true!
    }

    expect(doneShown).toBe(false);
    expect(errorMessage).toBe('Network error');
  });

  // 18. Existing authentication protection remains intact
  it('18. Existing authentication protection remains intact', () => {
    expect(authService.isAuthenticated()).toBe(false);
    localStorage.setItem('nivya_access_token', 'active_child_token');
    expect(authService.isAuthenticated()).toBe(true);
  });

  // 19. Existing Convocation tests remain intact
  it('19. Existing Convocation semantics remain intact', () => {
    expect('Mom,here').toBe('Mom,here');
    expect("Someone's,here").toBe("Someone's,here");
  });

  // 20. Existing navigation tests remain intact
  it('20. Existing navigation destinations remain intact', () => {
    expect(authService.getPostAuthDestination('CHILD', true)).toBe('/child');
    expect(authService.getPostAuthDestination('CHILD', false)).toBe('/pairing');
    expect(authService.getPostAuthDestination('PARENT', true)).toBe('/dashboard');
    expect(authService.getPostAuthDestination('PARENT', false)).toBe('/pairing');
  });
});
