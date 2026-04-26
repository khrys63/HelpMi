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
  move: (projectId, ticketId, targetProjectId) =>
    api.post(`/projects/${projectId}/tickets/${ticketId}/move`, { targetProjectId }),
  clone: (projectId, ticketId) => api.post(`/projects/${projectId}/tickets/${ticketId}/clone`),
  setDueDate: (projectId, ticketId, dueDate) =>
    api.patch(`/projects/${projectId}/tickets/${ticketId}/due-date`, { dueDate }),
  setClients: (projectId, ticketId, clientIds) =>
    api.put(`/projects/${projectId}/tickets/${ticketId}/clients`, { clientIds }),
  setLabels: (projectId, ticketId, labelIds) =>
    api.put(`/projects/${projectId}/tickets/${ticketId}/labels`, { labelIds }),
  remove: (projectId, ticketId) => api.delete(`/projects/${projectId}/tickets/${ticketId}`)
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
  me: () => api.get('/users/me')
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
