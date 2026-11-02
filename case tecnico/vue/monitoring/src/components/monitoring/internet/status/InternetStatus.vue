<script setup lang="ts">
import { computed } from 'vue'
import type {
  ConnectionStatus,
  LatencyMeasurement,
} from '@/services/monitoring/internet/latency/latency.api'

const props = defineProps<{
  connection: ConnectionStatus
  latest?: LatencyMeasurement
  stale: boolean
  error: string
}>()
const labels: Record<ConnectionStatus, string> = {
  connecting: 'Conectando',
  connected: 'Conectado',
  disconnected: 'Desconectado',
  error: 'Falha na conexão',
}
const status = computed(() => {
  if (props.connection !== 'connected' || props.stale || props.error) return 'Sem informação atual'
  if (!props.latest) return 'Aguardando medição'
  return props.latest.latency === null ? 'Falha no teste de conexão TCP' : 'Conexão TCP disponível'
})
</script>

<template>
  <section>
    <h2>Status</h2>
    <p role="status">{{ status }}</p>
    <p>Monitoramento: {{ labels[connection] }}</p>
    <p v-if="stale">Nenhuma medição recebida nos últimos 15 segundos.</p>
    <p v-if="error" role="alert">{{ error }}</p>
  </section>
</template>
