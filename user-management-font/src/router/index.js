import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import OrganizationsView from '../views/OrganizationsView.vue'
import UsersView from '../views/UsersView.vue'
import RolePermissionView from '../views/RolePermissionView.vue'
import SecurityView from '../views/SecurityView.vue'

const routes = [
  { path: '/login', component: LoginView },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: DashboardView },
      { path: 'organizations', component: OrganizationsView },
      { path: 'users', component: UsersView },
      { path: 'role-permission', component: RolePermissionView },
      { path: 'security', component: SecurityView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }
  const token = localStorage.getItem('token')
  if (!token) {
    return '/login'
  }
  return true
})

export default router
