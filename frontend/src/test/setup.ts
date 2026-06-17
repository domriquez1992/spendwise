import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// globals is disabled in vite.config, so register cleanup explicitly.
afterEach(() => {
  cleanup()
})
