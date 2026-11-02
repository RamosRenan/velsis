<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { ElButton, ElTable, ElTableColumn } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/table/style/css'
import { testPing, type PingResult } from '@/services/monitoring/internet/ping/ping.api'

const loading = ref(false)
const result = ref<PingResult[]>([])
const error = ref('')
const messages: Record<PingResult['status'], string> = {
  SUCCESS: 'Resposta ICMP recebida.',
  NO_REPLY: 'Nenhuma resposta ICMP confirmada. O destino pode estar indisponível ou bloquear ping.',
  TIMEOUT: 'O teste excedeu o limite de 5 segundos.',
  ERROR:
    'Não foi possível executar o ping no servidor. Verifique a disponibilidade e as permissões do comando ping.',
}
let controller: AbortController | undefined

async function ping() {
  if (loading.value) return
  const request = new AbortController()
  controller = request
  loading.value = true
  result.value = []
  error.value = ''
  try {
    const response = await testPing(request.signal)
    if (!request.signal.aborted) result.value = response
  } catch {
    if (!request.signal.aborted)
      error.value =
        'Não foi possível obter o resultado do ping. Verifique sua sessão e a conexão com o servidor.'
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}

onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <section>
    <h2>Teste de ping</h2>
    <p>
      Dez tentativas de ping ICMP do servidor até 8.8.8.8. Aguarde a conclusão; cada tentativa tem
      limite de 5 segundos.
    </p>
    <ElButton type="primary" :loading="loading" :disabled="loading" @click="ping">
      {{ loading ? 'Executando 10 pings…' : 'Testar 10 pings' }}
    </ElButton>
    <template v-if="result.length">
      <p role="status">
        {{ result.length }} testes concluídos ·
        {{ result.filter((item) => item.status === 'SUCCESS').length }} respostas confirmadas.
      </p>
      <ElTable :data="result">
        <ElTableColumn type="index" label="#" width="50" />
        <ElTableColumn prop="host" label="Destino" />
        <ElTableColumn label="Status" min-width="240">
          <template #default="{ row }">{{ messages[row.status as PingResult['status']] }}</template>
        </ElTableColumn>
        <ElTableColumn prop="durationMs" label="Duração (ms)" />
        <ElTableColumn label="Data/hora" min-width="170">
          <template #default="{ row }">{{
            new Date(row.timestamp).toLocaleString('pt-BR')
          }}</template>
        </ElTableColumn>
      </ElTable>
    </template>
    <p v-if="error" role="alert">{{ error }}</p>
  </section>
</template>
