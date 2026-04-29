import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../services/api.js'
import { i18n } from '../i18n.js'

export const COLORS = {
  blue:   { badge: 'bg-blue-100 text-blue-800',   dot: 'bg-blue-500' },
  yellow: { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500' },
  green:  { badge: 'bg-green-100 text-green-800',  dot: 'bg-green-500' },
  gray:   { badge: 'bg-gray-100 text-gray-700',    dot: 'bg-gray-400' },
  red:    { badge: 'bg-red-100 text-red-700',      dot: 'bg-red-500' },
  orange: { badge: 'bg-orange-100 text-orange-700', dot: 'bg-orange-500' },
  purple: { badge: 'bg-purple-100 text-purple-700', dot: 'bg-purple-500' },
  pink:   { badge: 'bg-pink-100 text-pink-700',   dot: 'bg-pink-500' },
  teal:   { badge: 'bg-teal-100 text-teal-700',   dot: 'bg-teal-500' },
  indigo: { badge: 'bg-indigo-100 text-indigo-700', dot: 'bg-indigo-600' },
}

export const COLOR_OPTIONS = Object.keys(COLORS)

export const SUPPORTED_LOCALES = ['fr', 'en', 'bg']

function localizeItem(item) {
  const locale = i18n.global.locale.value
  const label = item.labels?.[locale] || item.labels?.fr || item.label || ''
  const inverseLabel = item.inverseLabels?.[locale] || item.inverseLabels?.fr || item.inverseLabel || undefined
  return { ...item, label, inverseLabel }
}

export const useConfigStore = defineStore('config', () => {
  const statuses     = ref([])
  const priorities   = ref([])
  const types        = ref([])
  const linkTypes    = ref([])
  const projectRoles = ref([])
  const clients      = ref([])
  const loaded       = ref(false)

  function localizeAll() {
    statuses.value     = statuses.value.map(localizeItem)
    priorities.value   = priorities.value.map(localizeItem)
    types.value        = types.value.map(localizeItem)
    linkTypes.value    = linkTypes.value.map(localizeItem)
    projectRoles.value = projectRoles.value.map(localizeItem)
  }

  async function load() {
    const [{ data: config }, { data: clientList }] = await Promise.all([
      api.get('/admin/config'),
      api.get('/admin/clients')
    ])
    statuses.value     = config.STATUS       || []
    priorities.value   = config.PRIORITY     || []
    types.value        = config.TYPE         || []
    linkTypes.value    = config.LINK_TYPE    || []
    projectRoles.value = config.PROJECT_ROLE || []
    clients.value      = clientList
    loaded.value = true
    localizeAll()
  }

  function getStatus(code)   { return statuses.value.find(s => s.code === code) }
  function getPriority(code) { return priorities.value.find(p => p.code === code) }
  function getType(code)     { return types.value.find(t => t.code === code) }
  function getLinkType(code) { return linkTypes.value.find(l => l.code === code) }

  return { statuses, priorities, types, linkTypes, projectRoles, clients, loaded,
           load, localizeAll, getStatus, getPriority, getType, getLinkType }
})
