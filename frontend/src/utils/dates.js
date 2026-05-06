const LOCALE_BCP47 = { fr: 'fr-FR', en: 'en-GB', bg: 'bg-BG' }

function bcp47(locale) {
  return LOCALE_BCP47[locale] ?? 'fr-FR'
}

export function formatDate(dt, locale = 'fr') {
  if (!dt) return '-'
  return new Date(dt).toLocaleString(bcp47(locale), { dateStyle: 'short', timeStyle: 'short' })
}

export function formatDateOnly(iso, locale = 'fr') {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString(bcp47(locale), { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function relativeTime(dt, locale = 'fr') {
  if (!dt) return '-'
  const diffMs = Date.now() - new Date(dt).getTime()
  const diffMins = Math.round(diffMs / 60000)
  const diffHours = Math.round(diffMs / 3600000)
  const diffDays = Math.round(diffMs / 86400000)
  const rtf = new Intl.RelativeTimeFormat(bcp47(locale), { numeric: 'auto' })
  if (diffMins < 1)  return rtf.format(0, 'minute')
  if (diffMins < 60) return rtf.format(-diffMins, 'minute')
  if (diffHours < 24) return rtf.format(-diffHours, 'hour')
  if (diffDays < 30)  return rtf.format(-diffDays, 'day')
  return formatDate(dt, locale)
}
