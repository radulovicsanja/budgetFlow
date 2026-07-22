import type { CategoryType, TransactionType } from './types'

export function transactionTypeLabel(type: TransactionType | string): string {
  if (type === 'INCOME') return 'Prihod'
  if (type === 'EXPENSE') return 'Trošak'
  return String(type)
}

/** Naziv tipa kategorije na srpskom. */
export function categoryTypeLabel(
  type?: Pick<CategoryType, 'name' | 'description'> | string | null,
): string {
  if (!type) return '—'
  const code = typeof type === 'string' ? type : type.name
  const fromDb = typeof type === 'object' ? type.description?.trim() : ''
  if (fromDb) return fromDb

  const key = String(code || '').trim().toUpperCase()
  switch (key) {
    case 'ESSENTIAL':
      return 'Osnovne potrebe'
    case 'OPTIONAL':
      return 'Želje / dodatni troškovi'
    case 'SAVINGS':
      return 'Štednja'
    default:
      return String(code || '—')
  }
}
