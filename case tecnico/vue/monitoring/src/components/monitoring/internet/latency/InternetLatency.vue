<script setup lang="ts">
import { computed } from 'vue'
import { VisAxis, VisLine, VisScatter, VisXYContainer } from '@unovis/vue'
import type { LatencyMeasurement } from '@/services/monitoring/internet/latency/latency.api'

const props = defineProps<{ samples: LatencyMeasurement[] }>()
const latest = computed(() => props.samples.at(-1))
const timestamp = (item: LatencyMeasurement) => item.timestamp
const latency = (item: LatencyMeasurement) => item.latency ?? Number.NaN
const time = (value: number) => new Date(value).toLocaleTimeString('pt-BR')
</script>

<template>
  <section>
    <h2>Latência</h2>
    <p>Teste TCP do servidor até 8.8.8.8:443.</p>
    <p v-if="latest">
      Última leitura: {{ latest.latency === null ? 'Falha na conexão' : `${latest.latency} ms` }}
    </p>
    <VisXYContainer
      v-if="samples.some((item) => item.latency !== null)"
      :data="samples"
      :height="240"
    >
      <VisLine :x="timestamp" :y="latency" />
      <VisScatter :x="timestamp" :y="latency" />
      <VisAxis type="x" label="Hora de recebimento" :tick-format="time" :num-ticks="3" />
      <VisAxis type="y" label="Latência (ms)" />
    </VisXYContainer>
    <p v-else>Aguardando medições de latência válidas.</p>
  </section>
</template>
