import Dashboard from '@/components/dashborad/Dashboard.vue'
import Login from '@/components/login/Login.vue'
import Monitor from '@/components/monitoring/cam/monitor/Monitor.vue'
import ListCam from '@/components/monitoring/cam/listCam/ListCam.vue'
import Disc from '@/components/monitoring/hardware/disc/Disc.vue'
import GeneralConfiguration from '@/components/settings/GeneralConfiguration.vue'
import { useAuthStore } from '@/stores/auth/auth'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/web/internet',
      name: 'internet',
      component: () => import('@/components/monitoring/internet/Intenet.vue'),
      meta: { requiresAuth: true },
      props: { titleLogin: 'Internet' },
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      props: { titleLogin: 'Login Monitor' },
    },
    {
      path: '/',
      name: 'home',
      redirect: '/login',
    },
    {
      path: '/web/settings',
      name: 'settings',
      component: GeneralConfiguration,
      props: { titleLogin: 'Configurações gerais' },
      meta: { requiresAuth: true },
    },
    {
      path: '/web/dashboard',
      name: 'dashboard',
      component: Dashboard,
      props: { titleLogin: 'Dashboard' },
      meta: { requiresAuth: true },
    },
    {
      path: '/web/disc',
      name: 'disc',
      component: Disc,
      props: { titleLogin: 'Monitoramento de disco' },
      meta: { requiresAuth: true },
    },
    {
      path: '/web/monitor/cam',
      name: 'Monitor Cam',
      component: Monitor,
      props: { titleLogin: 'Monitoramento Camera' },
      meta: { requiresAuth: true },
    },
    {
      path: '/web/cameras',
      name: 'cameras',
      component: ListCam,
      props: { titleLogin: 'Câmeras cadastradas' },
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  await authStore.checkSession()

  if (to.name === 'login' || to.name === 'home') {
    return authStore.isAuthenticated ? { name: 'settings' } : true
  }

  if (!authStore.isAuthenticated) {
    return { name: 'login' }
  }

  return true
})

export default router
