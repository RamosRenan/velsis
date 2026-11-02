import { api } from '@/services/api'

export interface LatencyMeasurement {
  latency: number | null
  timestamp: number
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

export function connectLatency(
  onMeasurement: (measurement: LatencyMeasurement) => void,
  onStatus: (status: ConnectionStatus) => void,
  onError: (message: string) => void,
) {
  const url = new URL('/ws/monitor/internet', api.defaults.baseURL)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  const socket = new WebSocket(url)
  onStatus('connecting')
  socket.onopen = () => onStatus('connected')
  socket.onerror = () => onStatus('error')
  socket.onclose = () => onStatus('disconnected')
  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data !== -1 && (!data || !Number.isFinite(data.latency) || data.latency < 0)) {
        throw new Error('Invalid measurement')
      }
      onMeasurement({ latency: data === -1 ? null : data.latency, timestamp: Date.now() })
      onError('')
    } catch {
      onError('Medição de latência inválida recebida.')
    }
  }
  return () => {
    socket.onopen = socket.onmessage = socket.onerror = socket.onclose = null
    if (socket.readyState < WebSocket.CLOSING) socket.close(1000, 'Monitoramento encerrado')
  }
}
