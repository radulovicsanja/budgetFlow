export type TransactionType = 'INCOME' | 'EXPENSE'

export type Role = 'USER' | 'ADMIN'

export interface User {
  id: number
  username: string
  email: string
  role?: Role
  createdAt?: string
}

export interface CategoryType {
  id: number
  name: string
  description?: string
}

export interface Category {
  id: number
  name: string
  isDefault?: boolean
  type: CategoryType
  user?: User
}

export interface Budget {
  id: number
  month: string
  totalAmount: number
  additionalIncome: number
  user?: User
}

export interface BudgetCategory {
  id: number
  percentage: number
  allocatedAmount: number
  category: Category
  budget?: Budget
}

export interface Transaction {
  id: number
  amount: number
  type: TransactionType
  description?: string
  date: string
  /** Null/undefined za INCOME */
  category?: Category | null
}

export interface UserReport {
  id?: number
  userId?: number
  totalIncome: number
  totalExpenses: number
  totalSavings: number
  categoryName?: string
  amount?: number
  categoryBreakdown?: string
}

export interface Notification {
  id: number
  title: string
  message: string
  type: 'BILL_REMINDER' | 'BUDGET_WARNING' | 'INFO'
  dueDate?: string
  read: boolean
  createdAt?: string
}

export interface CsvImportResult {
  importedCount: number
  failedCount: number
  errors: string[]
}

export interface MonthTrend {
  month: string
  income: number
  expenses: number
  savings: number
}

export interface MonthCompare {
  currentMonth: string
  previousMonth: string
  currentIncome: number
  previousIncome: number
  incomeChangePercent: number
  currentExpenses: number
  previousExpenses: number
  expensesChangePercent: number
  currentSavings: number
  previousSavings: number
  savingsChangePercent: number
}

export interface SavingsGoal {
  id: number
  title: string
  targetAmount: number
  currentAmount: number
  deadline: string
  note?: string
  progressPercent: number
  completed: boolean
}

export interface RecurringTransaction {
  id: number
  amount: number
  type: TransactionType
  description?: string
  categoryId?: number | null
  categoryName?: string
  dayOfMonth: number
  active: boolean
  nextRunDate: string
}
