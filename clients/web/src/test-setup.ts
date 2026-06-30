import { vi } from 'vitest'

// Global localStorage mock
const localStorageStore: Record<string, string> = {}
const lsMock = {
  getItem: vi.fn((key: string) => localStorageStore[key] ?? null),
  setItem: vi.fn((key: string, value: string) => { localStorageStore[key] = value }),
  removeItem: vi.fn((key: string) => { delete localStorageStore[key] }),
  clear: vi.fn(() => { Object.keys(localStorageStore).forEach(k => delete localStorageStore[k]) }),
}
Object.defineProperty(global, 'localStorage', { value: lsMock, writable: true })

// Global window.location mock
Object.defineProperty(global, 'window', {
  value: {
    location: {
      hash: '',
      href: '',
      assign: vi.fn(),
      replace: vi.fn(),
    },
  },
  writable: true,
})
