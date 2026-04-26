import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
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
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue')
    }
  ]
})

export default router
