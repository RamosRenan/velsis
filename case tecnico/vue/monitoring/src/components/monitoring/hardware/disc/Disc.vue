<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import {
  connectDisc,
  type DiscMeasurement as Measurement,
} from '@/services/monitoring/hardware/disc/disc.api'
import { ElCol, ElRow } from 'element-plus'
import {
  VisAxis,
  VisDonut,
  VisLine,
  VisScatter,
  VisSingleContainer,
  VisXYContainer,
} from '@unovis/vue'
import 'element-plus/es/components/row/style/css'
import 'element-plus/es/components/col/style/css'

const gib = 1024 ** 3
const samples = ref<Measurement[]>([])
const status = ref('Aguardando conex?o?')
let disconnect: (() => void) | undefined

onMounted(() => {
  try {
    disconnect = connectDisc(
      (measurement) => {
        samples.value = [...samples.value, measurement]
          .sort((a, b) => Date.parse(a.timestamp) - Date.parse(b.timestamp))
          .slice(-300)
      },
      (message) => {
        status.value = message
      },
    )
  } catch {
    status.value = 'N?o foi poss?vel conectar ao monitoramento de disco.'
  }
})

onBeforeUnmount(() => disconnect?.())

const latest = computed(() => samples.value.at(-1))
const usage = (item: Measurement) => (1 - item.freeBytes / item.totalBytes) * 100
const timestamp = (item: Measurement) => Date.parse(item.timestamp)
const number = (value: number) => value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })
const dateTime = (value: number) =>
  new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
const charts = [
  { title: 'Espaço total', unit: 'GiB', y: (item: Measurement) => item.totalBytes / gib },
  { title: 'Espaço livre', unit: 'GiB', y: (item: Measurement) => item.freeBytes / gib },
  { title: 'Utilização por data e hora', unit: '%', y: usage },
]
const distribution = computed(() =>
  latest.value ? [usage(latest.value), 100 - usage(latest.value)] : [],
)
const value = (item: number) => item
</script>

<template>
  <section aria-label="Monitoramento de disco">
    <h1>Monitoramento de disco</h1>
    <el-divider />
    <p role="status">{{ status }}</p>
    <template v-if="latest">
      <p>
        Última medição:
        <time :datetime="latest.timestamp">
          {{ new Date(latest.timestamp).toLocaleString('pt-BR') }}
        </time>
      </p>
      <ElRow>
        <ElCol v-for="chart in charts" :key="chart.title" :xs="24" :md="12">
          <h2>{{ chart.title }}</h2>
          <p>{{ number(chart.y(latest)) }} {{ chart.unit }}</p>
          <VisXYContainer :data="samples" :height="240" :y-domain="chart.unit === '%' ? [0, 100] : undefined">
            <VisLine :x="timestamp" :y="chart.y" />
            <VisScatter :x="timestamp" :y="chart.y" />
            <VisAxis type="x" label="Data e hora" :tick-format="dateTime" :num-ticks="3" />
            <VisAxis type="y" :label="chart.unit" :tick-format="number" />
          </VisXYContainer>
        </ElCol>

        <ElCol :xs="24" :md="12">
          <h2>Percentual utilizado</h2>
          <VisSingleContainer :data="distribution" :height="240">
            <VisDonut :value="value" :central-label="number(usage(latest)) + '%'" central-sub-label="Utilizado" />
          </VisSingleContainer>
          <p>Utilizado: {{ number(usage(latest)) }}% · Livre: {{ number(100 - usage(latest)) }}%</p>
        </ElCol>
      </ElRow>
    </template>
    <p v-else role="status">Nenhuma medição válida de disco disponível.</p>
  </section>
</template>
