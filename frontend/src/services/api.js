import axios from 'axios'
import { useAuthStore } from '../stores/auth.js'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api'
})

api.interceptors.request.use(config => {
  const auth = useAuthStore()
  const t = auth.getToken()
  if (t) config.headers.Authorization = `Bearer ${t}`
  return config
})

// Projects
export const projectsApi = {
  list: () => api.get('/projects'),
  get: id => api.get(`/projects/${id}`),
  create: data => api.post('/projects', data),
  update: (id, data) => api.put(`/projects/${id}`, data),
  remove: id => api.delete(`/projects/${id}`)
}

// Tickets
export const ticketsApi = {
  list: (projectId, params) => api.get(`/projects/${projectId}/tickets`, { params }),
  get: (projectId, ticketId) => api.get(`/projects/${projectId}/tickets/${ticketId}`),
  create: (projectId, data) => api.post(`/projects/${projectId}/tickets`, data),
  update: (projectId, ticketId, data) => api.put(`/projects/${projectId}/tickets/${ticketId}`, data),
  changeStatus: (projectId, ticketId, status) => api.patch(`/projects/${projectId}/tickets/${ticketId}/status`, { status }),
  setAssignee: (projectId, ticketId, assigneeId) =>
    api.patch(`/projects/${projectId}/tickets/${ticketId}/assignee`, { assigneeId: assigneeId || null }),
  move: (projectId, ticketId, targetProjectId) =>
    api.post(`/projects/${projectId}/tickets/${ticketId}/move`, { targetProjectId }),
  clone: (projectId, ticketId) => api.post(`/projects/${projectId}/tickets/${ticketId}/clone`),
  setDueDate: (projectId, ticketId, dueDate) =>
    api.patch(`/projects/${projectId}/tickets/${ticketId}/due-date`, { dueDate }),
  setClients: (projectId, ticketId, clientIds) =>
    api.put(`/projects/${projectId}/tickets/${ticketId}/clients`, { clientIds }),
  setLabels: (projectId, ticketId, labelIds) =>
    api.put(`/projects/${projectId}/tickets/${ticketId}/labels`, { labelIds }),
  remove: (projectId, ticketId) => api.delete(`/projects/${projectId}/tickets/${ticketId}`),
  history: (projectId, ticketId) => api.get(`/projects/${projectId}/tickets/${ticketId}/history`)
}

// Comments
export const commentsApi = {
  list: ticketId => api.get(`/tickets/${ticketId}/comments`),
  add: (ticketId, body) => api.post(`/tickets/${ticketId}/comments`, { body }),
  update: (commentId, body) => api.put(`/comments/${commentId}`, { body }),
  remove: commentId => api.delete(`/comments/${commentId}`)
}

// Attachments
export const attachmentsApi = {
  upload: (ticketId, file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/tickets/${ticketId}/attachments`, form)
  },
  download: attachmentId => `/api/attachments/${attachmentId}`,
  remove: attachmentId => api.delete(`/attachments/${attachmentId}`)
}

// Users
export const usersApi = {
  list: () => api.get('/users'),
  me: () => api.get('/users/me'),
  assignable: projectId => api.get(`/projects/${projectId}/assignable-users`),
  updateTheme: theme => api.patch('/users/me/theme', { theme }),
  updateLocale: locale => api.patch('/users/me/locale', { locale })
}

// Personal tokens
export const personalTokensApi = {
  list: () => api.get('/users/me/tokens'),
  create: data => api.post('/users/me/tokens', data),
  remove: id => api.delete(`/users/me/tokens/${id}`)
}

// Clients admin
export const clientsAdminApi = {
  list: () => api.get('/admin/clients'),
  create: data => api.post('/admin/clients', data),
  update: (id, data) => api.put(`/admin/clients/${id}`, data),
  remove: id => api.delete(`/admin/clients/${id}`)
}

// Users admin
export const adminUsersApi = {
  list: () => api.get('/admin/users'),
  update: (id, data) => api.patch(`/admin/users/${id}`, data),
  assignOrganization: (id, data) => api.put(`/admin/users/${id}/organization`, data),
  updateProjects: (id, data) => api.put(`/admin/users/${id}/projects`, data)
}

// Organisations (admin)
export const organizationsApi = {
  list: () => api.get('/admin/organizations'),
  get: id => api.get(`/admin/organizations/${id}`),
  create: data => api.post('/admin/organizations', data),
  update: (id, data) => api.put(`/admin/organizations/${id}`, data),
  remove: id => api.delete(`/admin/organizations/${id}`),
  addProject: (orgId, projectId) => api.post(`/admin/organizations/${orgId}/projects/${projectId}`),
  removeProject: (orgId, projectId) => api.delete(`/admin/organizations/${orgId}/projects/${projectId}`),
  assignUser: (orgId, userId) => api.put(`/admin/organizations/${orgId}/users/${userId}`),
  removeUser: (orgId, userId) => api.delete(`/admin/organizations/${orgId}/users/${userId}`)
}

// Labels
export const labelsApi = {
  list: () => api.get('/admin/labels'),
  search: q => api.get('/admin/labels/search', { params: { q } }),
  findOrCreate: name => api.post('/admin/labels/find-or-create', { name }),
  create: data => api.post('/admin/labels', data),
  update: (id, data) => api.put(`/admin/labels/${id}`, data),
  remove: id => api.delete(`/admin/labels/${id}`)
}

// Liens
export const linksApi = {
  search: (q, excludeId) => api.get('/tickets/search', { params: { q, excludeId } }),
  create: (ticketId, targetTicketId, linkType) =>
    api.post(`/tickets/${ticketId}/links`, { targetTicketId, linkType }),
  remove: linkId => api.delete(`/ticket-links/${linkId}`)
}

export default api
