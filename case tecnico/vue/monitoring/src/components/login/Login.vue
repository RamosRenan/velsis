<script setup lang="ts">
/**
 * IMPORTS
 */
import { ref } from 'vue'
import { CloseBold } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth/auth'
import { csrfStore } from '@/stores/auth/csrf'
import { useRouter } from 'vue-router'
import { api } from '@/services/api'

/**
 * CONST
 */
const props = defineProps<{
  titleLogin: string
}>()

const inputUsername = ref()
const inputPassword = ref()
const store = useAuthStore()
const router = useRouter()
const submitting = ref(false)
const loginError = ref('')
/**
 * FUNCTIONS
 */

async function csrfToken() {
  const { data } = await api.get<{ token: string }>('/public/csrf', { timeout: 5000 })
  return data
}

async function login() {
  if (submitting.value) return
  submitting.value = true
  loginError.value = ''
  try {
    const token = await csrfToken()

    const response = await api.post(
      '/public/login',
      new URLSearchParams({
        usuario: inputUsername.value,
        senha: inputPassword.value,
      }),
      {
        timeout: 10000,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-CSRF-TOKEN': token.token,
        },
        validateStatus: () => true,
      },
    )

    if (response.status < 200 || response.status >= 300) {
      loginError.value =
        response.status === 401
          ? 'Usuário ou senha inválidos.'
          : 'Não foi possível entrar. Tente novamente.'
      return
    }

    await store.checkSession()
    if (!store.isAuthenticated) {
      loginError.value = 'Não foi possível confirmar sua sessão. Tente novamente.'
      return
    }

    csrfStore().$patch({ csrf: (await csrfToken()).token })

    await router.push({ name: 'settings' })
  } catch {
    loginError.value =
      'Não foi possível conectar ao servidor. Verifique se o backend está em execução.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="login">
    <div class="form-login">
      <div class="input-group">
        <div class="greetings">
          <h1 class="green">{{ titleLogin }}</h1>
        </div>

        <el-divider />

        <el-input
          v-model="inputUsername"
          clearable
          :clear-icon="CloseBold"
          placeholder="user"
          @keyup.enter.prevent="login"
        />
        <el-input
          v-model="inputPassword"
          clearable
          :clear-icon="CloseBold"
          placeholder="passwd"
          type="password"
          show-password
          @keyup.enter.prevent="login"
        />
        <p v-if="loginError" role="alert">{{ loginError }}</p>
        <el-button type="primary" native-type="submit" :loading="submitting" :disabled="submitting"
          >Login</el-button
        >
      </div>
    </div>
  </form>
</template>

<style scoped>
.form-login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100dvh - 140px);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 1em;
  width: 100%;
  max-width: 380px;
  padding: 2rem;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-background-soft);
  box-shadow: 0 8px 24px rgb(0 0 0 / 10%);
}

.greetings {
  text-align: center;
}
@media (max-width: 767px) {
  .input-group {
    padding: 24px 20px;
  }
}
</style>
