import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  // Atualize somente após validar as credenciais no serviço de autenticação.
  const isAuthenticated = ref(false)

  async function checkSession() {
    try {
      const response = await api.get('/auth-verify', {
        timeout: 5000,
        headers: { 'Cache-Control': 'no-cache, no-store' },
        params: { _sessionCheck: Date.now() },
        // Respostas HTTP negativas representam uma sessão não autenticada.
        validateStatus: () => true,
      })

      isAuthenticated.value = response.status === 200 &&
        String(response.headers['content-type'] ?? '').includes('application/json') &&
        response.data === true
    } catch {
      isAuthenticated.value = false
    }
  }

  return { isAuthenticated, checkSession }
})
