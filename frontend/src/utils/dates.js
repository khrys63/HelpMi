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
