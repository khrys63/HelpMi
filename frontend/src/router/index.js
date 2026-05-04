import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    {
      path: '/pending-org',
      name: 'pending-org',
      component: () => import('../views/PendingOrgView.vue')
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('../views/ProjectsView.vue')
    },
    {
      path: '/projects/:projectId',
      name: 'project-detail',
      component: () => import('../views/ProjectDetailView.vue')
    },
    {
      path: '/projects/:projectId/tickets/new',
      name: 'create-ticket',
      component: () => import('../views/CreateTicketView.vue')
    },
    {
      path: '/projects/:projectId/tickets/:ticketId',
      name: 'ticket-detail',
      component: () => import('../views/TicketDetailView.vue')
    },
    {
      path: '/admin/config',
      name: 'admin-config',
      meta: { requiresAdmin: true },
      component: () => import('../views/AdminConfigView.vue')
    },
    {
      path: '/admin/organizations',
      name: 'admin-organizations',
      meta: { requiresAdmin: true },
      component: () => import('../views/AdminOrganizationsView.vue')
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      meta: { requiresAdmin: true },
      component: () => import('../views/AdminUsersView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue')
    }
  ]
})

router.beforeEach((to) => {
  if (to.name === 'pending-org') return true
  const auth = useAuthStore()
  if (!auth.user) return true

  const isAdmin = auth.user.role === 'ADMIN'
  const hasOrg = auth.user.organizations?.length > 0

  if (to.meta.requiresAdmin && !isAdmin) {
    return hasOrg ? { name: 'projects' } : { name: 'pending-org' }
  }

  if (!isAdmin && !hasOrg) {
    return { name: 'pending-org' }
  }

  return true
})

export default router
