<script setup lang="ts">
/**
 * IMPORTS
 */
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Close,
  Connection,
  Grid,
  Setting,
  TrendCharts,
  VideoCameraFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth/auth'
import { csrfStore } from './stores/auth/csrf'
import { api } from '@/services/api'
import { ref, watch } from 'vue'

/**
 * CONST
 */
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const menuOpen = ref(false)
watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)
const navigation = [
  { to: '/web/cameras', label: 'Câmeras', icon: VideoCameraFilled },
  { to: '/web/settings', label: 'Configurações', icon: Setting },
  { to: '/web/dashboard', label: 'Dashboard', icon: Grid },
  { to: '/web/internet', label: 'Internet', icon: Connection },
  { to: '/web/disc', label: 'Disco', icon: TrendCharts },
]
const isLoggingOut = ref(false)
const logoutError = ref('')

/**
 * FUNCTIONS
 */
async function logout() {
  if (isLoggingOut.value) return
  isLoggingOut.value = true
  logoutError.value = ''
  try {
    const csrf = csrfStore()
    const token = await csrf.refresh()
    await api.post('/logout', undefined, {
      headers: { [token.headerName]: token.token },
    })
    authStore.isAuthenticated = false
    csrf.$patch({ csrf: '' })
  } catch {
    logoutError.value = 'Não foi possível encerrar a sessão. Tente novamente.'
    isLoggingOut.value = false
    return
  }

  try {
    await router.replace({ name: 'login' })
  } catch {
    logoutError.value = 'Sessão encerrada. Recarregue a página para acessar o login.'
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-bar">
      <RouterLink class="brand" :to="authStore.isAuthenticated ? '/web/cameras' : '/login'"
        >Monitor</RouterLink
      >
      <button
        v-if="authStore.isAuthenticated"
        class="menu-toggle"
        type="button"
        :aria-expanded="menuOpen"
        aria-controls="app-navigation"
        @click="menuOpen = !menuOpen"
      >
        {{ menuOpen ? 'Fechar menu' : 'Menu' }}
      </button>
    </div>
    <nav
      v-if="authStore.isAuthenticated"
      id="app-navigation"
      class="app-navigation"
      :class="{ 'is-open': menuOpen }"
      aria-label="Navegação principal"
    >
      <RouterLink v-for="item in navigation" :key="item.to" :to="item.to" class="nav-link">
        <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
      </RouterLink>
      <el-button
        class="logout-button"
        @click="logout"
        :icon="Close"
        :loading="isLoggingOut"
        :disabled="isLoggingOut"
        >Sair</el-button
      >
    </nav>
  </header>
  <main class="app-content">
    <p v-if="logoutError" role="alert">{{ logoutError }}</p>
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 24px;
}
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.brand {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-heading);
  padding: 0;
}
.app-navigation {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--color-text);
}
.nav-link:hover,
.router-link-active.nav-link {
  background: var(--color-background-mute);
  color: #168569;
}
.menu-toggle {
  display: none;
  background: var(--color-background-soft);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 10px 16px;
  font: inherit;
  cursor: pointer;
  min-height: 44px;
}
.app-content {
  min-width: 0;
}
@media (max-width: 767px) {
  .app-header {
    display: block;
    margin-bottom: 20px;
    padding-bottom: 12px;
  }
  .menu-toggle {
    display: block;
  }
  .app-navigation {
    display: none;
    padding-top: 14px;
  }
  .app-navigation.is-open {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .nav-link {
    min-height: 44px;
    font-size: 14px;
    padding: 10px;
  }
  .logout-button {
    margin: 0;
    min-height: 44px;
  }
}
</style>
