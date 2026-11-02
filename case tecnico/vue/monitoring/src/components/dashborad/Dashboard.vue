<script setup lang="ts">
import Disco from '@/components/monitoring/hardware/disc/Disc.vue'
import Internet from '@/components/monitoring/internet/Intenet.vue'
defineProps<{
  titleLogin: string
}>()
</script>

<template>
  <h1>{{ titleLogin }}</h1>
  <Internet />
  <Disco />
</template>

<style scoped></style>
