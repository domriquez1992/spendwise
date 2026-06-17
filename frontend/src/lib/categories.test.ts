import { describe, expect, it } from 'vitest'
import { CATEGORIES } from '../types/api'
import { CATEGORY_COLORS, CATEGORY_LABELS } from './categories'

describe('category metadata', () => {
  it('has a label and colour for every category', () => {
    for (const category of CATEGORIES) {
      expect(CATEGORY_LABELS[category]).toBeTruthy()
      expect(CATEGORY_COLORS[category]).toMatch(/^var\(--color-cat-/)
    }
  })
})
