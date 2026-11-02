<script setup lang="ts">
defineProps<{ timestamp?: number }>()
</script>

<template>
  <section>
    <h2>Data/hora</h2>
    <p v-if="timestamp !== undefined">
      Última medição recebida:
      <time :datetime="new Date(timestamp).toISOString()">{{
        new Date(timestamp).toLocaleString('pt-BR')
      }}</time>
    </p>
    <p v-else>Aguardando a primeira medição.</p>
  </section>
</template>
