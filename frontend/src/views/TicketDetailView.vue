<template>
  <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
  <div v-else class="space-y-6">
    <!-- Breadcrumb + actions -->
    <div class="space-y-2">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <router-link to="/projects" class="hover:text-blue-600 dark:hover:text-blue-400">{{ $t('projects.title') }}</router-link>
          <span>/</span>
          <router-link :to="`/projects/${ticket.projectId}`" class="hover:text-blue-600 dark:hover:text-blue-400">{{ ticket.projectKey }}</router-link>
          <span>/</span>
          <span class="text-gray-700 dark:text-gray-300 font-mono">{{ ticket.reference }}</span>
        </div>
        <div class="flex items-center gap-2">

          <!-- Bouton statut + transitions -->
          <div class="relative">
            <div v-if="showStatusMenu" @click="showStatusMenu = false" class="fixed inset-0 z-10"></div>
            <button @click.stop="showStatusMenu = !showStatusMenu"
              class="inline-flex items-center gap-1.5 text-sm font-medium border rounded-lg px-3 py-1.5 transition-colors"
              :class="statusButtonClass">
              {{ currentStatusLabel }}
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
              </svg>
            </button>
            <div v-if="showStatusMenu && availableTransitions.length"
              class="absolute right-0 top-full mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg z-20 py-1 min-w-36 overflow-hidden">
              <button v-for="t in availableTransitions" :key="t.status"
                @click.stop="applyTransition(t.status)"
                class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                {{ t.label }}
              </button>
            </div>
          </div>

          <button @click="openMoveForm"
            class="inline-flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8 7h12m0 0-4-4m4 4-4 4m0 6H4m0 0 4 4m-4-4 4-4"/>
            </svg>
            {{ $t('tickets.move') }}
          </button>
          <button @click="cloneTicket" :disabled="cloning"
            class="inline-flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            {{ cloning ? $t('tickets.cloning') : $t('tickets.clone') }}
          </button>

          <button @click="showHistory = true"
            class="inline-flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {{ $t('tickets.history') }}
          </button>
        </div>
      </div>

      <!-- Panneau déplacement -->
      <div v-if="showMoveForm" class="flex items-center gap-3 bg-gray-50 dark:bg-gray-700/50 border border-gray-200 dark:border-gray-600 rounded-xl px-4 py-3">
        <span class="text-sm text-gray-600 dark:text-gray-300 shrink-0">{{ $t('tickets.move_to') }}</span>
        <select v-model="moveTargetProjectId" class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100">
          <option v-for="p in moveProjects" :key="p.id" :value="p.id">{{ p.key }} — {{ p.name }}</option>
        </select>
        <button @click="confirmMove" :disabled="!moveTargetProjectId || moving"
          class="bg-blue-600 text-white text-sm px-4 py-1.5 rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium shrink-0">
          {{ moving ? $t('tickets.moving') : $t('common.confirm') }}
        </button>
        <button @click="showMoveForm = false" class="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 shrink-0">{{ $t('common.cancel') }}</button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Contenu principal -->
      <div class="lg:col-span-2 space-y-5">
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">

          <!-- Titre éditable -->
          <div class="mb-4 group">
            <div v-if="!editingTitle" @click="startEditTitle"
              class="text-xl font-bold text-gray-900 dark:text-gray-100 cursor-text rounded px-1 -mx-1 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
              {{ ticket.title }}
            </div>
            <div v-else class="space-y-2">
              <input v-model="editTitle" ref="titleInput"
                @keydown.enter="saveTitle" @keydown.escape="cancelTitle"
                class="w-full text-xl font-bold text-gray-900 dark:text-gray-100 border border-blue-400 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700" />
              <div class="flex gap-2">
                <button @click="saveTitle" :disabled="saving"
                  class="bg-blue-600 text-white text-xs px-3 py-1 rounded-md hover:bg-blue-700 disabled:opacity-50">
                  {{ saving ? $t('common.saving') : $t('common.save') }}
                </button>
                <button @click="cancelTitle" class="text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 px-3 py-1">{{ $t('common.cancel') }}</button>
              </div>
            </div>
          </div>

          <!-- Description éditable -->
          <div class="group">
            <div v-if="!editingDescription" @click="startEditDescription"
              class="cursor-text rounded px-1 -mx-1 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors min-h-[2rem]">
              <p v-if="ticket.description" class="text-gray-700 dark:text-gray-300 text-sm whitespace-pre-wrap">{{ ticket.description }}</p>
              <p v-else class="text-gray-400 dark:text-gray-500 text-sm italic">{{ $t('tickets.click_to_add_desc') }}</p>
            </div>
            <div v-else class="space-y-2">
              <textarea v-model="editDescription" ref="descriptionInput" rows="6"
                @keydown.escape="cancelDescription"
                class="w-full text-sm text-gray-700 dark:text-gray-300 border border-blue-400 rounded-lg px-3 py-2 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700" />
              <div class="flex gap-2">
                <button @click="saveDescription" :disabled="saving"
                  class="bg-blue-600 text-white text-xs px-3 py-1 rounded-md hover:bg-blue-700 disabled:opacity-50">
                  {{ saving ? $t('common.saving') : $t('common.save') }}
                </button>
                <button @click="cancelDescription" class="text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 px-3 py-1">{{ $t('common.cancel') }}</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pièces jointes -->
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="font-semibold text-gray-800 dark:text-gray-200 mb-3">{{ $t('tickets.attachments', { count: ticket.attachments?.length || 0 }) }}</h2>
          <ul v-if="ticket.attachments?.length" class="space-y-2 mb-4">
            <li v-for="a in ticket.attachments" :key="a.id"
              class="flex items-center justify-between text-sm bg-gray-50 dark:bg-gray-700/50 rounded-lg px-3 py-2">
              <a :href="a.downloadUrl" target="_blank" class="text-blue-600 dark:text-blue-400 hover:underline truncate">{{ a.fileName }}</a>
              <span class="text-gray-400 dark:text-gray-500 text-xs shrink-0 ml-2">{{ formatSize(a.size) }}</span>
            </li>
          </ul>
          <label class="cursor-pointer inline-flex items-center gap-2 text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
            <input type="file" class="hidden" @change="uploadFile" :disabled="uploading" />
            {{ uploading ? $t('tickets.uploading') : $t('tickets.add_file') }}
          </label>
        </div>

        <!-- Liens -->
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="font-semibold text-gray-800 dark:text-gray-200">{{ $t('tickets.links', { count: ticket.links?.length || 0 }) }}</h2>
            <button @click="showLinkForm = !showLinkForm"
              class="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 font-medium">
              {{ showLinkForm ? $t('common.cancel') : $t('tickets.add_link') }}
            </button>
          </div>

          <!-- Formulaire ajout -->
          <div v-if="showLinkForm" class="mb-4 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg space-y-3">
            <div class="relative">
              <input v-model="linkSearch" @input="onLinkSearch"
                :placeholder="$t('tickets.link_search_placeholder')"
                autocomplete="off"
                class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
              <ul v-if="linkResults.length"
                class="absolute top-full left-0 right-0 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg shadow-lg z-20 max-h-52 overflow-y-auto">
                <li v-for="t in linkResults" :key="t.id">
                  <button @click="selectLinkTarget(t)"
                    class="w-full text-left px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-700 flex items-center gap-2 text-sm">
                    <span class="font-mono text-xs text-gray-400 dark:text-gray-500 shrink-0">{{ t.reference }}</span>
                    <span class="truncate text-gray-800 dark:text-gray-200">{{ t.title }}</span>
                    <span class="ml-auto text-xs text-gray-400 dark:text-gray-500 shrink-0">{{ t.projectKey }}</span>
                  </button>
                </li>
              </ul>
            </div>

            <div v-if="linkTarget" class="flex items-center gap-2 text-sm bg-blue-50 dark:bg-blue-900/30 rounded-lg px-3 py-1.5">
              <span class="font-mono text-xs text-blue-500 dark:text-blue-400">{{ linkTarget.reference }}</span>
              <span class="text-gray-700 dark:text-gray-300 truncate">{{ linkTarget.title }}</span>
            </div>

            <div class="flex gap-2">
              <select v-model="linkType" class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100">
                <option v-for="lt in config.linkTypes" :key="lt.code" :value="lt.code">{{ lt.label }}</option>
              </select>
              <button @click="addLink" :disabled="!linkTarget || linkAdding"
                class="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                {{ linkAdding ? $t('common.saving') : $t('tickets.link_action') }}
              </button>
            </div>
          </div>

          <!-- Liste des liens -->
          <div v-if="ticket.links?.length" class="space-y-1.5">
            <div v-for="link in ticket.links" :key="link.id"
              class="flex items-center gap-3 text-sm py-1">
              <span class="text-xs text-gray-400 dark:text-gray-500 w-24 shrink-0 text-right italic">
                {{ linkLabel(link.linkType, link.direction) }}
              </span>
              <router-link
                :to="`/projects/${link.linkedTicket.projectId}/tickets/${link.linkedTicket.id}`"
                class="flex items-center gap-1.5 flex-1 min-w-0 hover:text-blue-600 dark:hover:text-blue-400 dark:text-gray-300">
                <span class="font-mono text-xs text-gray-400 dark:text-gray-500 shrink-0">{{ link.linkedTicket.reference }}</span>
                <span class="truncate">{{ link.linkedTicket.title }}</span>
              </router-link>
              <button @click="removeLink(link.id)"
                class="text-gray-300 dark:text-gray-600 hover:text-red-500 text-base leading-none shrink-0">✕</button>
            </div>
          </div>
          <p v-else-if="!showLinkForm" class="text-sm text-gray-400 dark:text-gray-500 italic">{{ $t('tickets.no_links') }}</p>
        </div>

        <!-- Commentaires -->
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="font-semibold text-gray-800 dark:text-gray-200 mb-4">{{ $t('tickets.comments', { count: ticket.comments?.length || 0 }) }}</h2>
          <div v-if="ticket.comments?.length" class="space-y-4 mb-5">
            <div v-for="c in ticket.comments" :key="c.id" class="flex gap-3">
              <div class="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300 flex items-center justify-center text-xs font-bold shrink-0">
                {{ (c.author?.firstName?.[0] || '?') + (c.author?.lastName?.[0] || '') }}
              </div>
              <div class="flex-1">
                <div class="flex items-baseline gap-2 mb-1">
                  <span class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ c.author?.firstName }} {{ c.author?.lastName }}</span>
                  <span class="text-xs text-gray-400 dark:text-gray-500">{{ formatDate(c.createdAt, locale.value) }}</span>
                  <span v-if="c.edited" class="text-xs text-gray-400 dark:text-gray-500">{{ $t('tickets.comment_edited') }}</span>
                </div>
                <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ c.body }}</p>
              </div>
            </div>
          </div>
          <div class="flex gap-3">
            <textarea v-model="newComment" rows="2" :placeholder="$t('tickets.comment_placeholder')"
              class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-2 text-sm resize-none focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
            <button @click="addComment" :disabled="!newComment.trim() || commentSaving"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 self-end">
              {{ $t('common.send') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="space-y-4">
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 space-y-4">

          <!-- Statut -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-2">{{ $t('tickets.status_label') }}</p>
            <StatusBadge :status="ticket.status" />
          </div>

          <!-- Priorité -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-2">{{ $t('tickets.priority_label') }}</p>
            <select v-model="ticket.priority" @change="updateField('priority', ticket.priority)"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="p in config.priorities" :key="p.code" :value="p.code">{{ p.label }}</option>
            </select>
          </div>

          <!-- Type -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-2">{{ $t('tickets.type_label') }}</p>
            <select v-model="ticket.type" @change="updateField('type', ticket.type)"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="t in config.types" :key="t.code" :value="t.code">{{ t.label }}</option>
            </select>
          </div>

          <!-- Échéance -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-1">{{ $t('tickets.due_date_label') }}</p>
            <div class="flex items-center gap-1">
              <input type="date" v-model="dueDateInput" @change="saveDueDate"
                class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100" />
              <button v-if="dueDateInput" @click="clearDueDate"
                class="text-gray-300 dark:text-gray-600 hover:text-red-500 text-base leading-none shrink-0 px-1">✕</button>
            </div>
          </div>

          <!-- Reporter -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-1">{{ $t('tickets.reporter_label') }}</p>
            <p class="text-sm text-gray-700 dark:text-gray-300">{{ ticket.reporter?.firstName }} {{ ticket.reporter?.lastName }}</p>
          </div>

          <!-- Assigné -->
          <div>
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-1">{{ $t('tickets.assignee_label') }}</p>
            <select v-model="assigneeId" @change="reassign" class="w-full border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option value="">{{ $t('tickets.field_assignee_none') }}</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.firstName }} {{ u.lastName }}</option>
            </select>
            <button v-if="canAssignToSelf" @click="assignToSelf"
              class="mt-1 text-xs text-blue-600 dark:text-blue-400 hover:underline">
              {{ $t('tickets.assign_to_me') }}
            </button>
          </div>

          <!-- Clients -->
          <div class="pt-2 border-t dark:border-gray-700">
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-2">{{ $t('tickets.clients_label') }}</p>
            <div class="flex flex-wrap gap-1 mb-2">
              <span v-for="c in ticket.clients" :key="c.id"
                class="inline-flex items-center gap-1 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 text-xs px-2 py-0.5 rounded-full">
                {{ c.name }}
                <button @click="removeClient(c.id)" class="text-indigo-400 hover:text-indigo-700 leading-none">✕</button>
              </span>
              <span v-if="!ticket.clients?.length" class="text-xs text-gray-400 dark:text-gray-500 italic">{{ $t('tickets.clients_none') }}</span>
            </div>
            <div class="relative">
              <select @change="addClient($event.target.value); $event.target.value = ''"
                class="w-full border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 dark:bg-gray-700">
                <option value="">{{ $t('tickets.add_client') }}</option>
                <option v-for="c in availableClients" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
          </div>

          <!-- Étiquettes -->
          <div class="pt-2 border-t dark:border-gray-700">
            <p class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase mb-2">{{ $t('tickets.labels_label') }}</p>
            <div class="flex flex-wrap gap-1 mb-2">
              <span v-for="l in ticket.labels" :key="l.id"
                :class="['inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full', labelBadgeClass(l.color)]">
                {{ l.name }}
                <button @click="removeLabel(l.id)" class="opacity-60 hover:opacity-100 leading-none">✕</button>
              </span>
              <span v-if="!ticket.labels?.length" class="text-xs text-gray-400 dark:text-gray-500 italic">{{ $t('tickets.labels_none') }}</span>
            </div>
            <div class="relative">
              <input v-model="labelInput" @input="onLabelInput" @keydown.enter.prevent="confirmLabelInput"
                @blur="hideSuggestionsDelayed"
                :placeholder="$t('tickets.label_search_placeholder')"
                class="w-full border dark:border-gray-600 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
              <ul v-if="labelSuggestions.length"
                class="absolute top-full left-0 right-0 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg shadow-lg z-20 max-h-40 overflow-y-auto">
                <li v-for="s in labelSuggestions" :key="s.id ?? s.name">
                  <button @mousedown.prevent="selectLabel(s)"
                    class="w-full text-left px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-700 flex items-center gap-2 text-sm">
                    <span v-if="!s.create" :class="['w-2.5 h-2.5 rounded-full shrink-0', dotClass(s.color)]"></span>
                    <span :class="s.create ? 'text-blue-600 dark:text-blue-400 italic' : 'text-gray-800 dark:text-gray-200'">
                      {{ s.create ? $t('tickets.label_create', { name: s.name }) : s.name }}
                    </span>
                  </button>
                </li>
              </ul>
            </div>
          </div>

          <!-- Dates -->
          <div class="text-xs text-gray-400 dark:text-gray-500 space-y-1 pt-2 border-t dark:border-gray-700">
            <p>{{ $t('tickets.dates_created', { date: formatDate(ticket.createdAt, locale.value) }) }}</p>
            <p>{{ $t('tickets.dates_updated', { date: formatDate(ticket.updatedAt, locale.value) }) }}</p>
            <p v-if="ticket.closedAt">{{ $t('tickets.dates_closed', { date: formatDate(ticket.closedAt, locale.value) }) }}</p>
          </div>

          <!-- Suppression (admin uniquement) -->
          <div v-if="isAdmin" class="pt-2 border-t dark:border-gray-700">
            <div v-if="!confirmingDelete">
              <button @click="confirmingDelete = true"
                class="w-full text-sm text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg px-3 py-1.5 text-left transition-colors">
                {{ $t('tickets.delete_ticket') }}
              </button>
            </div>
            <div v-else class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3 space-y-3">
              <p class="text-sm font-medium text-red-800 dark:text-red-300">{{ $t('tickets.delete_confirm_title', { ref: ticket.reference }) }}</p>
              <p class="text-xs text-red-600 dark:text-red-400">
                {{ $t('tickets.delete_confirm_body', { ref: ticket.reference }) }}
              </p>
              <div class="flex gap-2">
                <button @click="deleteTicket" :disabled="deleting"
                  class="flex-1 bg-red-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-red-700 disabled:opacity-50 font-medium">
                  {{ deleting ? $t('tickets.deleting') : $t('common.confirm') }}
                </button>
                <button @click="confirmingDelete = false"
                  class="flex-1 text-xs text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 px-3 py-1.5 border dark:border-gray-600 rounded-md">
                  {{ $t('common.cancel') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <TicketHistoryPanel
    :open="showHistory"
    :project-id="projectId"
    :ticket-id="ticketId"
    @close="showHistory = false"
  />
</template>

<script setup>
import { ref, nextTick, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ticketsApi, usersApi, attachmentsApi, commentsApi, linksApi, labelsApi, projectsApi } from '../services/api.js'
import { useAuthStore } from '../stores/auth.js'
import { useConfigStore, COLORS } from '../stores/config.js'
import { useToastStore } from '../stores/toast.js'
import { formatDate } from '../utils/dates.js'
import StatusBadge from '../components/tickets/StatusBadge.vue'
import TicketHistoryPanel from '../components/tickets/TicketHistoryPanel.vue'

const { t, locale } = useI18n()

const TRANSITIONS = computed(() => ({
  OPEN:        [{ status: 'IN_PROGRESS', label: t('status.transitions.OPEN_to_IN_PROGRESS') }, { status: 'STAND_BY', label: t('status.transitions.OPEN_to_STAND_BY') }, { status: 'CANCELLED', label: t('status.transitions.OPEN_to_CANCELLED') }],
  IN_PROGRESS: [{ status: 'RESOLVED',   label: t('status.transitions.IN_PROGRESS_to_RESOLVED') }, { status: 'STAND_BY', label: t('status.transitions.IN_PROGRESS_to_STAND_BY') }, { status: 'CANCELLED', label: t('status.transitions.IN_PROGRESS_to_CANCELLED') }],
  STAND_BY:    [{ status: 'IN_PROGRESS', label: t('status.transitions.STAND_BY_to_IN_PROGRESS') }, { status: 'OPEN', label: t('status.transitions.STAND_BY_to_OPEN') }, { status: 'CANCELLED', label: t('status.transitions.STAND_BY_to_CANCELLED') }],
  RESOLVED:    [{ status: 'CLOSED',     label: t('status.transitions.RESOLVED_to_CLOSED') }, { status: 'OPEN', label: t('status.transitions.RESOLVED_to_OPEN') }],
  CLOSED:      [{ status: 'OPEN',       label: t('status.transitions.CLOSED_to_OPEN') }],
  CANCELLED:   [{ status: 'OPEN',       label: t('status.transitions.CANCELLED_to_OPEN') }],
}))

const STATUS_BUTTON_COLORS = {
  blue:   'bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-200',
  yellow: 'bg-amber-100 text-amber-800 border-amber-200 hover:bg-amber-200',
  green:  'bg-green-100 text-green-800 border-green-200 hover:bg-green-200',
  gray:   'bg-gray-100 text-gray-700 border-gray-200 hover:bg-gray-200',
  red:    'bg-red-100 text-red-700 border-red-200 hover:bg-red-200',
  purple: 'bg-purple-100 text-purple-800 border-purple-200 hover:bg-purple-200',
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const config = useConfigStore()
const { projectId, ticketId } = route.params

const toast = useToastStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const showStatusMenu = ref(false)
const availableTransitions = computed(() => TRANSITIONS.value[ticket.value.status] ?? [])
const currentStatusLabel = computed(() => config.getStatus(ticket.value.status)?.label ?? ticket.value.status)
const statusButtonClass = computed(() => {
  const color = config.getStatus(ticket.value.status)?.color
  return STATUS_BUTTON_COLORS[color] ?? STATUS_BUTTON_COLORS.gray
})

const showHistory = ref(false)

const ticket = ref({})
const loading = ref(true)
const users = ref([])
const assigneeId = ref('')
const newComment = ref('')
const commentSaving = ref(false)
const uploading = ref(false)
const saving = ref(false)
const confirmingDelete = ref(false)
const deleting = ref(false)
const cloning = ref(false)
const dueDateInput = ref('')
const showMoveForm = ref(false)
const moveTargetProjectId = ref('')
const moveProjects = ref([])
const moving = ref(false)

// liens
const showLinkForm = ref(false)
const linkSearch = ref('')
const linkResults = ref([])
const linkTarget = ref(null)
const linkType = ref(config.linkTypes[0]?.code ?? '')
const linkAdding = ref(false)
let linkSearchTimer = null

function linkLabel(type, direction) {
  const lt = config.getLinkType(type)
  if (!lt) return type
  return direction === 'INCOMING' ? (lt.inverseLabel || lt.label) : lt.label
}

// édition titre
const editingTitle = ref(false)
const editTitle = ref('')
const titleInput = ref(null)

// édition description
const editingDescription = ref(false)
const editDescription = ref('')
const descriptionInput = ref(null)

onMounted(async () => {
  const [{ data: t }, { data: u }] = await Promise.all([
    ticketsApi.get(projectId, ticketId),
    usersApi.assignable(projectId)
  ])
  ticket.value = t
  users.value = u
  assigneeId.value = t.assignee?.id || ''
  dueDateInput.value = t.dueDate || ''
  loading.value = false
})

// --- Titre ---
function startEditTitle() {
  editTitle.value = ticket.value.title
  editingTitle.value = true
  nextTick(() => titleInput.value?.focus())
}
function cancelTitle() {
  editingTitle.value = false
}
async function saveTitle() {
  if (!editTitle.value.trim()) return
  saving.value = true
  const { data } = await ticketsApi.update(projectId, ticketId, { title: editTitle.value.trim() })
  ticket.value.title = data.title
  ticket.value.updatedAt = data.updatedAt
  editingTitle.value = false
  saving.value = false
}

// --- Description ---
function startEditDescription() {
  editDescription.value = ticket.value.description || ''
  editingDescription.value = true
  nextTick(() => descriptionInput.value?.focus())
}
function cancelDescription() {
  editingDescription.value = false
}
async function saveDescription() {
  saving.value = true
  const { data } = await ticketsApi.update(projectId, ticketId, { description: editDescription.value })
  ticket.value.description = data.description
  ticket.value.updatedAt = data.updatedAt
  editingDescription.value = false
  saving.value = false
}

// --- Champs sidebar ---
async function updateField(field, value) {
  const { data } = await ticketsApi.update(projectId, ticketId, { [field]: value })
  ticket.value.updatedAt = data.updatedAt
}

async function applyTransition(newStatus) {
  showStatusMenu.value = false
  const { data } = await ticketsApi.changeStatus(projectId, ticketId, newStatus)
  ticket.value.status = data.ticket.status
  ticket.value.updatedAt = data.ticket.updatedAt
  ticket.value.closedAt = data.ticket.closedAt
  if (data.nextTicketReference) {
    const typeLabel = config.types.find(t => t.code === ticket.value.type)?.label ?? ticket.value.type
    toast.add(`Ticket ${typeLabel} fermé — ${data.nextTicketReference} recréé automatiquement`, 'success')
  }
}

const canAssignToSelf = computed(() =>
  auth.user && assigneeId.value !== auth.user.id &&
  users.value.some(u => u.id === auth.user.id)
)

async function assignToSelf() {
  assigneeId.value = auth.user.id
  await reassign()
}

async function reassign() {
  const { data } = await ticketsApi.setAssignee(projectId, ticketId, assigneeId.value || null)
  ticket.value.assignee = data.assignee
  ticket.value.updatedAt = data.updatedAt
}

// --- Commentaires ---
async function addComment() {
  commentSaving.value = true
  const { data } = await commentsApi.add(ticketId, newComment.value)
  ticket.value.comments.push(data)
  newComment.value = ''
  commentSaving.value = false
}

// --- Pièces jointes ---
async function uploadFile(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    const { data } = await attachmentsApi.upload(ticketId, file)
    ticket.value.attachments.push(data)
  } catch (err) {
    const msg = err?.response?.data?.detail || err?.response?.data?.message || 'Erreur lors de l\'envoi du fichier'
    toast.add(msg, 'error')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function onLinkSearch() {
  clearTimeout(linkSearchTimer)
  linkTarget.value = null
  linkResults.value = []
  if (linkSearch.value.length < 2) return
  linkSearchTimer = setTimeout(async () => {
    const { data } = await linksApi.search(linkSearch.value, ticketId)
    linkResults.value = data
  }, 300)
}

function selectLinkTarget(t) {
  linkTarget.value = t
  linkSearch.value = `${t.reference} — ${t.title}`
  linkResults.value = []
}

async function addLink() {
  if (!linkTarget.value) return
  linkAdding.value = true
  const { data } = await linksApi.create(ticketId, linkTarget.value.id, linkType.value)
  ticket.value.links.push(data)
  showLinkForm.value = false
  linkSearch.value = ''
  linkTarget.value = null
  linkType.value = config.linkTypes[0]?.code ?? ''
  linkAdding.value = false
}

async function removeLink(linkId) {
  await linksApi.remove(linkId)
  ticket.value.links = ticket.value.links.filter(l => l.id !== linkId)
}

async function saveDueDate() {
  const { data } = await ticketsApi.setDueDate(projectId, ticketId, dueDateInput.value || null)
  ticket.value.dueDate = data.dueDate
  ticket.value.updatedAt = data.updatedAt
}

async function clearDueDate() {
  dueDateInput.value = ''
  const { data } = await ticketsApi.setDueDate(projectId, ticketId, null)
  ticket.value.dueDate = data.dueDate
  ticket.value.updatedAt = data.updatedAt
}

async function openMoveForm() {
  const { data } = await projectsApi.list()
  moveProjects.value = data.filter(p => p.id !== ticket.value.projectId)
  moveTargetProjectId.value = moveProjects.value[0]?.id ?? ''
  showMoveForm.value = true
}

async function confirmMove() {
  if (!moveTargetProjectId.value) return
  moving.value = true
  const sourceRef = ticket.value.reference
  const { data } = await ticketsApi.move(projectId, ticketId, moveTargetProjectId.value)
  toast.add(`${sourceRef} déplacé vers ${data.projectKey} — nouvelle référence ${data.reference}`, 'info')
  router.push(`/projects/${data.projectId}/tickets/${data.id}`)
}

async function cloneTicket() {
  cloning.value = true
  const sourceRef = ticket.value.reference
  const { data } = await ticketsApi.clone(projectId, ticketId)
  toast.add(`Redirigé vers le clone de ${sourceRef} — ${data.reference}`, 'info')
  router.push(`/projects/${data.projectId}/tickets/${data.id}`)
}

async function deleteTicket() {
  deleting.value = true
  await ticketsApi.remove(projectId, ticketId)
  router.push(`/projects/${projectId}`)
}

// --- Clients ---
const availableClients = computed(() =>
  config.clients.filter(c => c.active && !ticket.value.clients?.some(tc => tc.id === c.id))
)

async function addClient(clientId) {
  if (!clientId) return
  const ids = [...(ticket.value.clients || []).map(c => c.id), clientId]
  const { data } = await ticketsApi.setClients(projectId, ticketId, ids)
  ticket.value.clients = data
}

async function removeClient(clientId) {
  const ids = ticket.value.clients.map(c => c.id).filter(id => id !== clientId)
  const { data } = await ticketsApi.setClients(projectId, ticketId, ids)
  ticket.value.clients = data
}

// --- Étiquettes ---
const labelInput = ref('')
const labelSuggestions = ref([])
let labelSearchTimer = null

function dotClass(color) {
  return COLORS[color]?.dot ?? 'bg-gray-300'
}

function labelBadgeClass(color) {
  return COLORS[color]?.badge ?? 'bg-gray-100 text-gray-700'
}

function onLabelInput() {
  clearTimeout(labelSearchTimer)
  const q = labelInput.value.trim()
  if (!q) { labelSuggestions.value = []; return }
  labelSearchTimer = setTimeout(async () => {
    const { data } = await labelsApi.search(q)
    const suggestions = data.filter(l => !ticket.value.labels?.some(tl => tl.id === l.id))
    const exactMatch = data.some(l => l.name.toLowerCase() === q.toLowerCase())
    if (!exactMatch) suggestions.push({ id: null, name: q, color: null, create: true })
    labelSuggestions.value = suggestions
  }, 250)
}

function confirmLabelInput() {
  if (labelSuggestions.value.length === 1) selectLabel(labelSuggestions.value[0])
}

function hideSuggestionsDelayed() {
  setTimeout(() => { labelSuggestions.value = [] }, 150)
}

async function selectLabel(suggestion) {
  let label
  if (suggestion.create) {
    const { data } = await labelsApi.findOrCreate(suggestion.name)
    label = data
  } else {
    label = suggestion
  }
  const ids = [...(ticket.value.labels || []).map(l => l.id), label.id]
  const { data } = await ticketsApi.setLabels(projectId, ticketId, ids)
  ticket.value.labels = data
  labelInput.value = ''
  labelSuggestions.value = []
}

async function removeLabel(labelId) {
  const ids = ticket.value.labels.map(l => l.id).filter(id => id !== labelId)
  const { data } = await ticketsApi.setLabels(projectId, ticketId, ids)
  ticket.value.labels = data
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' o'
  if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' Ko'
  return (bytes / 1024 / 1024).toFixed(1) + ' Mo'
}
</script>
