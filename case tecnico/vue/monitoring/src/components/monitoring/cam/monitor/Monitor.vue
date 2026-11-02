<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import CameraStream from './stream/CameraStream.vue'
const route = useRoute()
const camera = computed(() => (typeof route.query.camera === 'string' ? route.query.camera : ''))
</script>

<template>
  <header class="monitor-heading">
    <RouterLink to="/web/cameras">← Voltar às câmeras</RouterLink>
    <h1>Monitor ao vivo</h1>
    <p v-if="camera" class="camera-name">{{ camera }}</p>
  </header>

  <CameraStream v-if="camera" :key="camera" :camera="camera" />
  <p v-else>Selecione uma câmera na lista para visualizar o vídeo.</p>
</template>

<style scoped>
.monitor-heading {
  margin-bottom: 20px;
}
.monitor-heading a {
  display: inline-block;
  padding: 8px 0;
  margin-bottom: 8px;
}
.monitor-heading h1 {
  margin-bottom: 6px;
}
.camera-name {
  overflow-wrap: anywhere;
  font-size: 14px;
  color: var(--color-text);
}
</style>
