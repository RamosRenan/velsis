import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/services/api'

export const csrfStore = defineStore('csrf', () => {
  const csrf = ref('')

  async function refresh() {
    const { data } = await api.get<{ token: string; headerName: string }>('/public/csrf')
    if (!data.token || !data.headerName) {
      throw new Error('Resposta CSRF inválida')
    }
    csrf.value = data.token
    return data
  }

  return { csrf, refresh }
})
