<script setup lang="ts">
import { ElCol, ElRow } from 'element-plus'
import 'element-plus/es/components/row/style/css'
import 'element-plus/es/components/col/style/css'
import InternetDateTime from './datetime/InternetDateTime.vue'
import InternetStatus from './status/InternetStatus.vue'
import InternetPing from './ping/InternetPing.vue'
import InternetLatency from './latency/InternetLatency.vue'
import { useLatency } from './latency/useLatency'

defineProps<{
  titleLogin?: string
}>()

const { samples, latest, connection, error, stale } = useLatency()
</script>

<template>
  <section aria-label="Monitoramento de internet">
    <h1>{{ titleLogin }}</h1>
    <el-divider />
    <ElRow>
      <ElCol :xs="24" :md="12">
        <InternetDateTime :timestamp="latest?.timestamp" />
      </ElCol>
      <ElCol :xs="24" :md="12">
        <InternetStatus :connection="connection" :latest="latest" :stale="stale" :error="error" />
      </ElCol>
      <ElCol :xs="24" :md="12">
        <InternetPing />
      </ElCol>
      <ElCol :xs="24" :md="12">
        <InternetLatency :samples="samples" />
      </ElCol>
    </ElRow>
  </section>
</template>
