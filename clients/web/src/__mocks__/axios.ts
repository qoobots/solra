// Simple mock for axios - must NOT import from vitest (alias resolution happens before vitest runtime)
const noop = () => {}
const fn = () => {
  const mockFn: any = (...args: any[]) => { mockFn.mock.calls.push(args); return mockFn.mock.implementation?.(...args) }
  mockFn.mock = { calls: [] as any[], implementation: null as any }
  mockFn.mockResolvedValue = (val: any) => { mockFn.mock.implementation = () => Promise.resolve(val) }
  mockFn.mockRejectedValue = (val: any) => { mockFn.mock.implementation = () => Promise.reject(val) }
  mockFn.mockResolvedValueOnce = (val: any) => {
    const prev = mockFn.mock.implementation
    mockFn.mock.implementation = () => { mockFn.mock.implementation = prev; return Promise.resolve(val) }
  }
  mockFn.mockRejectedValueOnce = (val: any) => {
    const prev = mockFn.mock.implementation
    mockFn.mock.implementation = () => { mockFn.mock.implementation = prev; return Promise.reject(val) }
  }
  return mockFn
}

const mockInstance = {
  get: fn(),
  post: fn(),
  put: fn(),
  delete: fn(),
  patch: fn(),
  interceptors: {
    request: { use: noop, eject: noop, clear: noop },
    response: { use: noop, eject: noop, clear: noop },
  },
  defaults: { headers: { common: {} } },
}

const axiosMock = {
  create: () => mockInstance,
  get: fn(),
  post: fn(),
  put: fn(),
  delete: fn(),
  defaults: { headers: { common: {} } },
}

export default axiosMock
export { mockInstance as __mockInstance }
