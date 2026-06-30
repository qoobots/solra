import { describe, it, expect } from 'vitest'

describe('basic smoke test', () => {
  it('should pass without any imports', () => {
    expect(1 + 1).toBe(2)
  })
})
