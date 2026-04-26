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
      component: () => import('../views/AdminConfigView.vue')
    },
    {
      path: '/admin/organizations',
      name: 'admin-organizations',
      component: () => import('../views/AdminOrganizationsView.vue')
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/AdminUsersView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue')
    }
  ]
})

// Redirect users without an org to the pending screen (except ADMIN and /pending-org itself)
router.beforeEach((to) => {
  if (to.name === 'pending-org') return true
  const auth = useAuthStore()
  if (!auth.user) return true
  if (auth.user.role !== 'ADMIN' && !auth.user.organizationId) {
    return { name: 'pending-org' }
  }
  return true
})

export default router
