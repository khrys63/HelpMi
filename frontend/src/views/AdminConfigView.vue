<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100">{{ $t('admin.config.title') }}</h1>
    </div>

    <!-- Onglets -->
    <div class="flex gap-1 mb-6 border-b border-gray-200 dark:border-gray-700 flex-wrap">
      <button v-for="tab in tabs" :key="tab" @click="switchTab(tab)"
        :class="['px-4 py-2 text-sm font-medium rounded-t-lg border-b-2 transition-colors',
          activeTab === tab
            ? 'border-blue-600 text-blue-600'
            : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200']">
        {{ $t('admin.config.tabs.' + tab) }}
      </button>
    </div>

    <!-- Contenu -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-semibold text-gray-800 dark:text-gray-200">{{ $t('admin.config.tabs.' + activeTab) }}</h2>
        <button @click="openCreate" class="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700">
          + {{ $t('admin.config.modal_add') }}
        </button>
      </div>

      <table class="w-full text-sm">
        <thead>
          <tr class="text-left text-xs text-gray-500 dark:text-gray-400 uppercase border-b dark:border-gray-700">
            <template v-if="isConfigTab">
              <th class="pb-2 pr-4">{{ $t('admin.config.col_code') }}</th>
              <th class="pb-2 pr-4">{{ $t('admin.config.col_label') }}</th>
              <th v-if="isLinkTypeTab" class="pb-2 pr-4">{{ $t('admin.config.col_inverse_label') }}</th>
              <th class="pb-2 pr-4">{{ $t('admin.config.col_color') }}</th>
              <th class="pb-2 pr-4">{{ $t('admin.config.col_active') }}</th>
              <th class="pb-2 pr-4">{{ $t('admin.config.col_position') }}</th>
            </template>
            <template v-else-if="isLabelTab">
              <th class="pb-2 pr-4">{{ $t('admin.config.col_name') }}</th>
              <th class="pb-2 pr-4">{{ $t('admin.config.col_color') }}</th>
            </template>
            <th class="pb-2"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr v-for="item in displayItems" :key="item.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
            <template v-if="isConfigTab">
              <td class="py-2.5 pr-4 font-mono text-gray-500 dark:text-gray-400 text-xs">{{ item.code }}</td>
              <td class="py-2.5 pr-4 font-medium text-gray-800 dark:text-gray-200">{{ item.label }}</td>
              <td v-if="isLinkTypeTab" class="py-2.5 pr-4 text-gray-500 dark:text-gray-400 italic">{{ item.inverseLabel || '—' }}</td>
              <td class="py-2.5 pr-4">
                <span :class="['inline-block w-4 h-4 rounded-full', dotClass(item.color)]"></span>
                <span class="ml-1 text-gray-500 text-xs">{{ item.color }}</span>
              </td>
              <td class="py-2.5 pr-4">
                <span :class="item.active ? 'text-green-600' : 'text-gray-400'">{{ item.active ? $t('common.yes') : $t('common.no') }}</span>
              </td>
              <td class="py-2.5 pr-4 text-gray-500">{{ item.position }}</td>
            </template>
          <template v-else-if="isLabelTab">
              <td class="py-2.5 pr-4 font-medium text-gray-800 dark:text-gray-200">{{ item.name }}</td>
              <td class="py-2.5 pr-4">
                <span v-if="item.color" :class="['inline-block w-4 h-4 rounded-full mr-1', dotClass(item.color)]"></span>
                <span class="text-gray-500 dark:text-gray-400 text-xs">{{ item.color || '—' }}</span>
              </td>
            </template>
            <td class="py-2.5 text-right space-x-2">
              <button @click="openEdit(item)" class="text-blue-500 hover:text-blue-700 text-xs">{{ $t('common.edit') }}</button>
              <button @click="confirmDelete(item)" class="text-red-400 hover:text-red-600 text-xs">{{ $t('common.delete') }}</button>
            </td>
          </tr>
          <tr v-if="displayItems.length === 0">
            <td :colspan="isLinkTypeTab ? 7 : isConfigTab ? 6 : 3" class="py-6 text-center text-gray-400 dark:text-gray-500 italic">{{ $t('admin.config.no_values') }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal création / édition -->
    <div v-if="modal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-4 dark:text-gray-100">{{ editingItem ? $t('admin.config.modal_edit') : $t('admin.config.modal_add') }} — {{ $t('admin.config.tabs.' + activeTab) }}</h2>

        <!-- Formulaire config value (STATUS, PRIORITY, TYPE, LINK_TYPE) -->
        <div v-if="isConfigTab" class="space-y-3">
          <div v-if="!editingItem">
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('admin.config.field_code') }}</label>
            <input v-model="form.code" placeholder="EX: MON_STATUT"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm uppercase dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
            <p class="text-xs text-gray-400 dark:text-gray-500 mt-0.5">{{ $t('admin.config.field_code_hint') }}</p>
          </div>
          <!-- Labels multilingues LINK_TYPE -->
          <div v-if="isLinkTypeTab" class="bg-gray-50 dark:bg-gray-700/50 border border-gray-200 dark:border-gray-600 rounded-lg p-3 space-y-3">
            <div>
              <p class="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5">{{ $t('admin.config.field_label_direct') }}
                <span class="font-normal text-gray-400 dark:text-gray-500">{{ $t('admin.config.field_label_direct_hint') }}</span>
              </p>
              <div class="space-y-1.5">
                <div v-for="lc in SUPPORTED_LOCALES" :key="'d'+lc" class="flex items-center gap-2">
                  <span class="text-[10px] font-mono font-semibold w-5 text-center text-gray-400 dark:text-gray-500 uppercase shrink-0">{{ lc }}</span>
                  <input v-model="form.labels[lc]"
                    :class="['flex-1 border rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100',
                      lc === 'fr' ? 'border-gray-300 dark:border-gray-500' : 'border-dashed border-gray-200 dark:border-gray-600']" />
                </div>
              </div>
            </div>
            <div>
              <p class="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5">{{ $t('admin.config.field_label_inverse') }}
                <span class="font-normal text-gray-400 dark:text-gray-500">{{ $t('admin.config.field_label_inverse_hint') }}</span>
              </p>
              <div class="space-y-1.5">
                <div v-for="lc in SUPPORTED_LOCALES" :key="'i'+lc" class="flex items-center gap-2">
                  <span class="text-[10px] font-mono font-semibold w-5 text-center text-gray-400 dark:text-gray-500 uppercase shrink-0">{{ lc }}</span>
                  <input v-model="form.inverseLabels[lc]"
                    :class="['flex-1 border rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100',
                      lc === 'fr' ? 'border-gray-300 dark:border-gray-500' : 'border-dashed border-gray-200 dark:border-gray-600']" />
                </div>
              </div>
            </div>
          </div>

          <!-- Labels multilingues autres catégories -->
          <div v-else>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5">{{ $t('admin.config.field_label') }}</label>
            <div class="space-y-1.5">
              <div v-for="lc in SUPPORTED_LOCALES" :key="lc" class="flex items-center gap-2">
                <span class="text-[10px] font-mono font-semibold w-5 text-center text-gray-400 dark:text-gray-500 uppercase shrink-0">{{ lc }}</span>
                <input v-model="form.labels[lc]"
                  :class="['flex-1 border rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100',
                    lc === 'fr' ? 'border-gray-300 dark:border-gray-500' : 'border-dashed border-gray-200 dark:border-gray-600']" />
              </div>
            </div>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('admin.config.field_color') }}</label>
            <ColorPicker v-model="form.color" />
          </div>
          <div class="flex gap-4">
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="form.active" class="rounded" /> {{ $t('admin.config.field_active') }}
            </label>
            <div class="flex items-center gap-2">
              <label class="text-xs text-gray-600">{{ $t('admin.config.field_position') }}</label>
              <input type="number" v-model.number="form.position" min="0"
                class="w-16 border rounded px-2 py-1 text-sm" />
            </div>
          </div>
        </div>

        <!-- Formulaire étiquette -->
        <div v-else-if="isLabelTab" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('admin.config.field_label_name') }}</label>
            <input v-model="form.name" :placeholder="$t('admin.config.field_label_name_placeholder')"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('admin.config.field_color') }}</label>
            <ColorPicker v-model="form.color" />
          </div>
        </div>

        <p v-if="formError" class="text-sm text-red-600 mt-3">{{ formError }}</p>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="closeModal" class="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 px-4 py-2">{{ $t('common.cancel') }}</button>
          <button @click="save" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? $t('common.saving') : (editingItem ? $t('common.save') : $t('common.create')) }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal confirmation suppression -->
    <div v-if="deleteTarget" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-sm">
        <h2 class="text-lg font-bold text-red-700 dark:text-red-400 mb-2">{{ $t('admin.config.delete_title') }}</h2>
        <p class="text-sm text-gray-700 dark:text-gray-300 mb-4">
          {{ $t('admin.config.delete_body', { name: deleteTarget.label || deleteTarget.name }) }}
        </p>
        <p v-if="deleteError" class="text-sm text-red-600 mb-3">{{ deleteError }}</p>
        <div class="flex justify-end gap-2">
          <button @click="deleteTarget = null; deleteError = ''" class="text-sm text-gray-500 dark:text-gray-400 px-4 py-2">{{ $t('common.cancel') }}</button>
          <button @click="doDelete" :disabled="deleting"
            class="bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50">
            {{ deleting ? $t('common.saving') : $t('common.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h } from 'vue'
import api from '../services/api.js'
import { useConfigStore, COLOR_OPTIONS, COLORS, SUPPORTED_LOCALES } from '../stores/config.js'

const ColorPicker = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('div', { class: 'flex gap-2 flex-wrap' },
      COLOR_OPTIONS.map(c =>
        h('button', {
          key: c,
          type: 'button',
          title: c,
          class: [
            'w-7 h-7 rounded-full border-2 transition-all',
            COLORS[c]?.dot ?? 'bg-gray-300',
            props.modelValue === c
              ? 'border-gray-800 dark:border-white scale-110 ring-2 ring-offset-1 ring-gray-400 dark:ring-offset-gray-800'
              : 'border-transparent hover:scale-105'
          ],
          onClick: () => emit('update:modelValue', c)
        })
      )
    )
  }
}

const config = useConfigStore()

const tabs = ['STATUS', 'PRIORITY', 'TYPE', 'LINK_TYPE', 'PROJECT_ROLE', 'LABEL']
const activeTab = ref('STATUS')

const isConfigTab   = computed(() => ['STATUS', 'PRIORITY', 'TYPE', 'LINK_TYPE', 'PROJECT_ROLE'].includes(activeTab.value))
const isLinkTypeTab = computed(() => activeTab.value === 'LINK_TYPE')
const isLabelTab    = computed(() => activeTab.value === 'LABEL')

// Local data for LABEL tab
const labelItems  = ref([])

async function loadLabels() {
  const { data } = await api.get('/admin/labels')
  labelItems.value = data
}

const displayItems = computed(() => {
  if (isLabelTab.value)  return labelItems.value
  switch (activeTab.value) {
    case 'STATUS':    return config.statuses
    case 'PRIORITY':  return config.priorities
    case 'TYPE':      return config.types
    case 'LINK_TYPE': return config.linkTypes
    case 'PROJECT_ROLE': return config.projectRoles
    default: return []
  }
})

async function switchTab(key) {
  activeTab.value = key
  if (key === 'LABEL' && labelItems.value.length === 0) await loadLabels()
}

function dotClass(color) {
  return COLORS[color]?.dot ?? 'bg-gray-300'
}

// --- Modal ---
const modal      = ref(false)
const editingItem = ref(null)
const form       = ref({})
const formError  = ref('')
const saving     = ref(false)

function emptyLabels() { return Object.fromEntries(SUPPORTED_LOCALES.map(lc => [lc, ''])) }

function openCreate() {
  editingItem.value = null
  formError.value = ''
  if (isConfigTab.value) {
    form.value = { code: '', labels: emptyLabels(), inverseLabels: emptyLabels(), color: 'blue', active: true, position: displayItems.value.length + 1 }
  } else {
    form.value = { name: '', color: 'blue' }
  }
  modal.value = true
}

function openEdit(item) {
  editingItem.value = item
  formError.value = ''
  if (isConfigTab.value) {
    form.value = {
      code: item.code,
      labels: Object.fromEntries(SUPPORTED_LOCALES.map(lc => [lc, item.labels?.[lc] || ''])),
      inverseLabels: Object.fromEntries(SUPPORTED_LOCALES.map(lc => [lc, item.inverseLabels?.[lc] || ''])),
      color: item.color || 'gray',
      active: item.active,
      position: item.position
    }
  } else {
    form.value = { name: item.name, color: item.color || 'blue' }
  }
  modal.value = true
}

function closeModal() { modal.value = false }

function configPayload() {
  const f = form.value
  return {
    code: f.code,
    label:          f.labels.fr || '',
    labelEn:        f.labels.en || null,
    labelBg:        f.labels.bg || null,
    inverseLabel:   f.inverseLabels.fr || null,
    inverseLabelEn: f.inverseLabels.en || null,
    inverseLabelBg: f.inverseLabels.bg || null,
    color:    f.color,
    active:   f.active,
    position: f.position
  }
}

async function save() {
  formError.value = ''
  saving.value = true
  try {
    if (isConfigTab.value) {
      const cat = activeTab.value
      const payload = configPayload()
      if (editingItem.value) {
        const { data } = await api.put(`/admin/config/${cat}/${editingItem.value.id}`, payload)
        replaceInList(displayItems.value, data)
      } else {
        const { data } = await api.post(`/admin/config/${cat}`, payload)
        displayItems.value.push(data)
      }
      await config.load()
    } else {
      if (editingItem.value) {
        const { data } = await api.put(`/admin/labels/${editingItem.value.id}`, form.value)
        replaceInList(labelItems.value, data)
      } else {
        const { data } = await api.post('/admin/labels', form.value)
        labelItems.value.push(data)
      }
    }
    modal.value = false
  } catch (e) {
    formError.value = e.response?.data?.detail || 'Erreur'
  } finally {
    saving.value = false
  }
}

// --- Suppression ---
const deleteTarget = ref(null)
const deleteError  = ref('')
const deleting     = ref(false)

function confirmDelete(item) {
  deleteTarget.value = item
  deleteError.value = ''
}

async function doDelete() {
  deleting.value = true
  deleteError.value = ''
  try {
    if (isConfigTab.value) {
      await api.delete(`/admin/config/${activeTab.value}/${deleteTarget.value.id}`)
      await config.load()
    } else {
      await api.delete(`/admin/labels/${deleteTarget.value.id}`)
      labelItems.value = labelItems.value.filter(l => l.id !== deleteTarget.value.id)
    }
    deleteTarget.value = null
  } catch (e) {
    deleteError.value = e.response?.data?.detail || 'Impossible de supprimer'
  } finally {
    deleting.value = false
  }
}

function replaceInList(list, updated) {
  const idx = list.findIndex(i => i.id === updated.id)
  if (idx !== -1) list.splice(idx, 1, updated)
}
</script>
