import { vi } from 'vitest'

// Create a mock axios-like instance for store tests
export function createMockApi() {
  return {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
      response: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
    },
    defaults: {
      headers: { common: {} },
    },
  }
}

export const mockApi = createMockApi()
