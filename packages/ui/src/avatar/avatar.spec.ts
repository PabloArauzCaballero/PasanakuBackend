import { describe, expect, it } from 'vitest'
import { inicialesDe } from './avatar'

describe('inicialesDe', () => {
  it('primera y última', () => expect(inicialesDe('María Elena Quispe')).toBe('MQ'))
  it('un solo nombre', () => expect(inicialesDe('Vos')).toBe('VO'))
  it('vacío', () => expect(inicialesDe('  ')).toBe('?'))
})
