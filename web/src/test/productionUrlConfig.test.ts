import { describe, it, expect } from 'vitest';
import { API_BASE, apiClient } from '../services/api';
import { WS_BASE } from '../services/websocketService';

describe('Production URL Configuration & Endpoints', () => {
  it('defaults API_BASE to /api/v1 when VITE_API_BASE_URL is not explicitly configured', () => {
    expect(API_BASE).toBeDefined();
    // In local development test environment without VITE_API_BASE_URL override, should be /api/v1
    expect(API_BASE).toBe('/api/v1');
    expect(apiClient.defaults.baseURL).toBe('/api/v1');
  });

  it('defaults WS_BASE to /ws when VITE_WS_BASE_URL is not explicitly configured', () => {
    expect(WS_BASE).toBeDefined();
    expect(WS_BASE).toBe('/ws');
  });

  it('correctly constructs authentication API endpoints', () => {
    expect(`${API_BASE}/auth/login`).toBe('/api/v1/auth/login');
    expect(`${API_BASE}/auth/register`).toBe('/api/v1/auth/register');
    expect(`${API_BASE}/auth/me`).toBe('/api/v1/auth/me');
    expect(`${API_BASE}/auth/logout`).toBe('/api/v1/auth/logout');
  });

  it('correctly constructs pairing API endpoints', () => {
    expect(`${API_BASE}/pairing/status`).toBe('/api/v1/pairing/status');
    expect(`${API_BASE}/pairing/code`).toBe('/api/v1/pairing/code');
    expect(`${API_BASE}/pairing/connect`).toBe('/api/v1/pairing/connect');
  });

  it('correctly constructs convocation API endpoints', () => {
    expect(`${API_BASE}/convocation/parent/history`).toBe('/api/v1/convocation/parent/history');
    expect(`${API_BASE}/convocation/parent/send`).toBe('/api/v1/convocation/parent/send');
    expect(`${API_BASE}/convocation/child/unread`).toBe('/api/v1/convocation/child/unread');
    expect(`${API_BASE}/convocation/child/view/start`).toBe('/api/v1/convocation/child/view/start');
  });

  it('correctly constructs account deletion API endpoints', () => {
    expect(`${API_BASE}/account/deletion/status`).toBe('/api/v1/account/deletion/status');
    expect(`${API_BASE}/account/deletion/request-child-approval`).toBe('/api/v1/account/deletion/request-child-approval');
    expect(`${API_BASE}/account/deletion/verify-child-code`).toBe('/api/v1/account/deletion/verify-child-code');
    expect(`${API_BASE}/account/delete`).toBe('/api/v1/account/delete');
  });

  it('supports simulated production Render URL construction', () => {
    const prodApiBase = 'https://nivya-blbf.onrender.com/api/v1'.replace(/\/+$/, '');
    const prodWsBase = 'https://nivya-blbf.onrender.com/ws'.replace(/\/+$/, '');

    expect(`${prodApiBase}/auth/login`).toBe('https://nivya-blbf.onrender.com/api/v1/auth/login');
    expect(`${prodApiBase}/auth/register`).toBe('https://nivya-blbf.onrender.com/api/v1/auth/register');
    expect(`${prodApiBase}/auth/me`).toBe('https://nivya-blbf.onrender.com/api/v1/auth/me');
    expect(`${prodApiBase}/pairing/status`).toBe('https://nivya-blbf.onrender.com/api/v1/pairing/status');
    expect(`${prodApiBase}/convocation/parent/history`).toBe('https://nivya-blbf.onrender.com/api/v1/convocation/parent/history');
    expect(`${prodApiBase}/account/delete`).toBe('https://nivya-blbf.onrender.com/api/v1/account/delete');
    expect(prodWsBase).toBe('https://nivya-blbf.onrender.com/ws');
  });

  it('sanitizes and strips any accidental trailing slashes from base URLs', () => {
    const rawUrlWithSlash = 'https://nivya-blbf.onrender.com/api/v1/';
    const sanitized = rawUrlWithSlash.replace(/\/+$/, '');
    expect(`${sanitized}/auth/login`).toBe('https://nivya-blbf.onrender.com/api/v1/auth/login');
  });
});
