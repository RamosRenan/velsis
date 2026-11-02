import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  connectLatency,
  type ConnectionStatus,
  type LatencyMeasurement,
} from '@/services/monitoring/internet/latency/latency.api'

export function useLatency() {
  const samples = ref<LatencyMeasurement[]>([])
  const connection = ref<ConnectionStatus>('connecting')
  const error = ref('')
  const stale = ref(false)
  let disconnect: (() => void) | undefined
  let timeout: ReturnType<typeof setTimeout> | undefined
  const waitForMeasurement = () => {
    clearTimeout(timeout)
    stale.value = false
    timeout = setTimeout(() => {
      stale.value = true
    }, 15_000)
  }

  onMounted(() => {
    try {
      disconnect = connectLatency(
        (measurement) => {
          samples.value = [...samples.value, measurement].slice(-300)
          waitForMeasurement()
        },
        (status) => {
          connection.value = status
          clearTimeout(timeout)
          if (status === 'connected') waitForMeasurement()
        },
        (message) => {
          error.value = message
        },
      )
    } catch {
      connection.value = 'error'
    }
  })
  onBeforeUnmount(() => {
    clearTimeout(timeout)
    disconnect?.()
  })

  return { samples, latest: computed(() => samples.value.at(-1)), connection, error, stale }
}
