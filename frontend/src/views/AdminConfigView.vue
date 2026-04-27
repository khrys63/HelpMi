<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Configuration</h1>
    </div>

    <!-- Onglets -->
    <div class="flex gap-1 mb-6 border-b border-gray-200 flex-wrap">
      <button v-for="tab in tabs" :key="tab.key" @click="switchTab(tab.key)"
        :class="['px-4 py-2 text-sm font-medium rounded-t-lg border-b-2 transition-colors',
          activeTab === tab.key
            ? 'border-blue-600 text-blue-600'
            : 'border-transparent text-gray-500 hover:text-gray-700']">
        {{ tab.label }}
      </button>
    </div>

    <!-- Contenu -->
    <div class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-semibold text-gray-800">{{ currentTab.label }}</h2>
        <button @click="openCreate" class="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700">
          + Ajouter
        </button>
      </div>

      <table class="w-full text-sm">
        <thead>
          <tr class="text-left text-xs text-gray-500 uppercase border-b">
            <template v-if="isConfigTab">
              <th class="pb-2 pr-4">Code</th>
              <th class="pb-2 pr-4">Libellé direct</th>
              <th v-if="isLinkTypeTab" class="pb-2 pr-4">Libellé inverse</th>
              <th class="pb-2 pr-4">Couleur</th>
              <th class="pb-2 pr-4">Actif</th>
              <th class="pb-2 pr-4">Position</th>
            </template>
            <template v-else-if="isClientTab">
              <th class="pb-2 pr-4">Nom</th>
              <th class="pb-2 pr-4">Email contact</th>
              <th class="pb-2 pr-4">Actif</th>
            </template>
            <template v-else-if="isLabelTab">
              <th class="pb-2 pr-4">Nom</th>
              <th class="pb-2 pr-4">Couleur</th>
            </template>
            <th class="pb-2"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="item in displayItems" :key="item.id" class="hover:bg-gray-50">
            <template v-if="isConfigTab">
              <td class="py-2.5 pr-4 font-mono text-gray-500 text-xs">{{ item.code }}</td>
              <td class="py-2.5 pr-4 font-medium text-gray-800">{{ item.label }}</td>
              <td v-if="isLinkTypeTab" class="py-2.5 pr-4 text-gray-500 italic">{{ item.inverseLabel || '—' }}</td>
              <td class="py-2.5 pr-4">
                <span :class="['inline-block w-4 h-4 rounded-full', dotClass(item.color)]"></span>
                <span class="ml-1 text-gray-500 text-xs">{{ item.color }}</span>
              </td>
              <td class="py-2.5 pr-4">
                <span :class="item.active ? 'text-green-600' : 'text-gray-400'">{{ item.active ? 'Oui' : 'Non' }}</span>
              </td>
              <td class="py-2.5 pr-4 text-gray-500">{{ item.position }}</td>
            </template>
            <template v-else-if="isClientTab">
              <td class="py-2.5 pr-4 font-medium text-gray-800">{{ item.name }}</td>
              <td class="py-2.5 pr-4 text-gray-500">{{ item.contactEmail || '—' }}</td>
              <td class="py-2.5 pr-4">
                <span :class="item.active ? 'text-green-600' : 'text-gray-400'">{{ item.active ? 'Oui' : 'Non' }}</span>
              </td>
            </template>
            <template v-else-if="isLabelTab">
              <td class="py-2.5 pr-4 font-medium text-gray-800">{{ item.name }}</td>
              <td class="py-2.5 pr-4">
                <span v-if="item.color" :class="['inline-block w-4 h-4 rounded-full mr-1', dotClass(item.color)]"></span>
                <span class="text-gray-500 text-xs">{{ item.color || '—' }}</span>
              </td>
            </template>
            <td class="py-2.5 text-right space-x-2">
              <button @click="openEdit(item)" class="text-blue-500 hover:text-blue-700 text-xs">Modifier</button>
              <button @click="confirmDelete(item)" class="text-red-400 hover:text-red-600 text-xs">Supprimer</button>
            </td>
          </tr>
          <tr v-if="displayItems.length === 0">
            <td :colspan="isLinkTypeTab ? 7 : isConfigTab ? 6 : 4" class="py-6 text-center text-gray-400 italic">Aucune valeur.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal création / édition -->
    <div v-if="modal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-4">{{ editingItem ? 'Modifier' : 'Ajouter' }} — {{ currentTab.label }}</h2>

        <!-- Formulaire config value (STATUS, PRIORITY, TYPE, LINK_TYPE) -->
        <div v-if="isConfigTab" class="space-y-3">
          <div v-if="!editingItem">
            <label class="block text-xs font-medium text-gray-600 mb-1">Code *</label>
            <input v-model="form.code" placeholder="EX: MON_STATUT"
              class="w-full border rounded-lg px-3 py-2 text-sm uppercase" />
            <p class="text-xs text-gray-400 mt-0.5">Sera mis en majuscules, espaces → _</p>
          </div>
          <div v-if="isLinkTypeTab" class="bg-gray-50 border border-gray-200 rounded-lg p-3 space-y-2">
            <p class="text-xs text-gray-500 font-medium uppercase tracking-wide mb-1">Paire de libellés</p>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">Libellé direct *
                <span class="text-gray-400 font-normal">(ex : Bloque)</span>
              </label>
              <input v-model="form.label" placeholder="ex: Bloque"
                class="w-full border rounded-lg px-3 py-2 text-sm" />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">Libellé inverse *
                <span class="text-gray-400 font-normal">(ex : Bloqué par)</span>
              </label>
              <input v-model="form.inverseLabel" placeholder="ex: Bloqué par"
                class="w-full border rounded-lg px-3 py-2 text-sm" />
            </div>
          </div>
          <div v-else>
            <label class="block text-xs font-medium text-gray-600 mb-1">Libellé *</label>
            <input v-model="form.label" placeholder="Libellé affiché"
              class="w-full border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Couleur</label>
            <ColorPicker v-model="form.color" />
          </div>
          <div class="flex gap-4">
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="form.active" class="rounded" /> Actif
            </label>
            <div class="flex items-center gap-2">
              <label class="text-xs text-gray-600">Position</label>
              <input type="number" v-model.number="form.position" min="0"
                class="w-16 border rounded px-2 py-1 text-sm" />
            </div>
          </div>
        </div>

        <!-- Formulaire client -->
        <div v-else-if="isClientTab" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Nom *</label>
            <input v-model="form.name" placeholder="Nom du client"
              class="w-full border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Email contact</label>
            <input v-model="form.contactEmail" type="email" placeholder="contact@client.com"
              class="w-full border rounded-lg px-3 py-2 text-sm" />
          </div>
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" v-model="form.active" class="rounded" /> Actif
          </label>
        </div>

        <!-- Formulaire étiquette -->
        <div v-else-if="isLabelTab" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Nom *</label>
            <input v-model="form.name" placeholder="ex: urgent"
              class="w-full border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Couleur</label>
            <ColorPicker v-model="form.color" />
          </div>
        </div>

        <p v-if="formError" class="text-sm text-red-600 mt-3">{{ formError }}</p>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="closeModal" class="text-sm text-gray-500 hover:text-gray-700 px-4 py-2">Annuler</button>
          <button @click="save" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? '…' : (editingItem ? 'Enregistrer' : 'Créer') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal confirmation suppression -->
    <div v-if="deleteTarget" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-sm">
        <h2 class="text-lg font-bold text-red-700 mb-2">Supprimer ?</h2>
        <p class="text-sm text-gray-700 mb-4">
          Supprimer <strong>{{ deleteTarget.label || deleteTarget.name }}</strong> ?
          Cette action est irréversible et sera refusée si des tickets utilisent encore cette valeur.
        </p>
        <p v-if="deleteError" class="text-sm text-red-600 mb-3">{{ deleteError }}</p>
        <div class="flex justify-end gap-2">
          <button @click="deleteTarget = null; deleteError = ''" class="text-sm text-gray-500 px-4 py-2">Annuler</button>
          <button @click="doDelete" :disabled="deleting"
            class="bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50">
            {{ deleting ? '…' : 'Confirmer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import api from '../services/api.js'
import { useConfigStore, COLOR_OPTIONS, COLORS } from '../stores/config.js'

// Inline color picker component
const ColorPicker = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <div class="flex gap-2 flex-wrap">
      <button v-for="c in options" :key="c" type="button" @click="$emit('update:modelValue', c)"
        :class="['w-7 h-7 rounded-full border-2 transition-all', dotClass(c),
          modelValue === c ? 'border-gray-800 scale-110' : 'border-transparent']"
        :title="c" />
    </div>`,
  setup(props) {
    return {
      options: COLOR_OPTIONS,
      dotClass: c => COLORS[c]?.dot ?? 'bg-gray-300'
    }
  }
}

const config = useConfigStore()

const tabs = [
  { key: 'STATUS',    label: 'Statuts' },
  { key: 'PRIORITY',  label: 'Priorités' },
  { key: 'TYPE',      label: 'Types' },
  { key: 'LINK_TYPE', label: 'Types de liens' },
  { key: 'PROJECT_ROLE', label: 'Rôles projet' },
  { key: 'CLIENT',    label: 'Clients' },
  { key: 'LABEL',     label: 'Étiquettes' },
]
const activeTab = ref('STATUS')
const currentTab = computed(() => tabs.find(t => t.key === activeTab.value))

const isConfigTab   = computed(() => ['STATUS', 'PRIORITY', 'TYPE', 'LINK_TYPE', 'PROJECT_ROLE'].includes(activeTab.value))
const isLinkTypeTab = computed(() => activeTab.value === 'LINK_TYPE')
const isClientTab   = computed(() => activeTab.value === 'CLIENT')
const isLabelTab    = computed(() => activeTab.value === 'LABEL')

// Local data for CLIENT and LABEL tabs
const clientItems = ref([])
const labelItems  = ref([])

async function loadClients() {
  const { data } = await api.get('/admin/clients')
  clientItems.value = data
}
async function loadLabels() {
  const { data } = await api.get('/admin/labels')
  labelItems.value = data
}

const displayItems = computed(() => {
  if (isClientTab.value) return clientItems.value
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
  if (key === 'CLIENT' && clientItems.value.length === 0) await loadClients()
  if (key === 'LABEL'  && labelItems.value.length === 0)  await loadLabels()
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

function openCreate() {
  editingItem.value = null
  formError.value = ''
  if (isConfigTab.value) {
    form.value = { code: '', label: '', inverseLabel: '', color: 'blue', active: true, position: displayItems.value.length + 1 }
  } else if (isClientTab.value) {
    form.value = { name: '', contactEmail: '', active: true }
  } else {
    form.value = { name: '', color: 'blue' }
  }
  modal.value = true
}

function openEdit(item) {
  editingItem.value = item
  formError.value = ''
  if (isConfigTab.value) {
    form.value = { code: item.code, label: item.label, inverseLabel: item.inverseLabel || '', color: item.color || 'gray', active: item.active, position: item.position }
  } else if (isClientTab.value) {
    form.value = { name: item.name, contactEmail: item.contactEmail || '', active: item.active }
  } else {
    form.value = { name: item.name, color: item.color || 'blue' }
  }
  modal.value = true
}

function closeModal() { modal.value = false }

async function save() {
  formError.value = ''
  saving.value = true
  try {
    if (isConfigTab.value) {
      const cat = activeTab.value
      if (editingItem.value) {
        const { data } = await api.put(`/admin/config/${cat}/${editingItem.value.id}`, form.value)
        replaceInList(displayItems.value, data)
      } else {
        const { data } = await api.post(`/admin/config/${cat}`, form.value)
        displayItems.value.push(data)
      }
      await config.load()
    } else if (isClientTab.value) {
      if (editingItem.value) {
        const { data } = await api.put(`/admin/clients/${editingItem.value.id}`, form.value)
        replaceInList(clientItems.value, data)
        await config.load()
      } else {
        const { data } = await api.post('/admin/clients', form.value)
        clientItems.value.push(data)
        await config.load()
      }
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
    } else if (isClientTab.value) {
      await api.delete(`/admin/clients/${deleteTarget.value.id}`)
      clientItems.value = clientItems.value.filter(c => c.id !== deleteTarget.value.id)
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
