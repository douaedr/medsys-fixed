export const colors = {
  primary: '#2563eb',
  primaryDark: '#1e40af',
  primaryLight: '#dbeafe',
  success: '#059669',
  successLight: '#d1fae5',
  warning: '#d97706',
  warningLight: '#fef3c7',
  danger: '#dc2626',
  dangerLight: '#fee2e2',
  gray: '#6b7280',
  grayLight: '#f3f4f6',
  dark: '#0f172a',
  border: '#e5e7eb',
  white: '#ffffff',
  bg: '#f8fafc',
}

export const radius = { sm: 8, md: 14, lg: 20, xl: 24 }

export const shadow = {
  sm: { shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 3, elevation: 2 },
  md: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.08, shadowRadius: 10, elevation: 4 },
}

// Objet `theme` unifié pour les nouveaux écrans
export const theme = {
  primary: colors.primary,
  primaryDark: colors.primaryDark,
  success: colors.success,
  danger: colors.danger,
  warning: colors.warning,
  gray: colors.gray,
  dark: colors.dark,
  border: colors.border,
  bg: colors.bg,
  shadow,
  radius,
}

