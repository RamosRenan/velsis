import { api } from '@/services/api'

export interface DiscMeasurement {
  totalBytes: number
  freeBytes: number
  timestamp: string
}

export function connectDisc(
  onMeasurement: (measurement: DiscMeasurement) => void,
  onStatus: (status: string) => void,
) {
  const url = new URL('/ws/monitor/disc', api.defaults.baseURL)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  const socket = new WebSocket(url)
  onStatus('Conectando ao monitoramento de disco…')

  socket.onopen = () => onStatus('Conectado')
  socket.onerror = () => onStatus('Falha na conexão com o monitoramento de disco.')
  socket.onclose = () => onStatus('Monitoramento desconectado.')
  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (
        !data ||
        !Number.isSafeInteger(data.totalBytes) ||
        data.totalBytes <= 0 ||
        !Number.isSafeInteger(data.freeByte) ||
        data.freeByte < 0 ||
        data.freeByte > data.totalBytes ||
        !Number.isSafeInteger(data.timestamp) ||
        !Number.isFinite(new Date(data.timestamp).getTime())
      ) {
        throw new Error('Medição inválida')
      }
      onMeasurement({
        totalBytes: data.totalBytes,
        freeBytes: data.freeByte,
        timestamp: new Date(data.timestamp).toISOString(),
      })
      onStatus('Conectado')
    } catch {
      onStatus('Medição de disco inválida recebida.')
    }
  }

  return () => {
    socket.onopen = null
    socket.onmessage = null
    socket.onerror = null
    socket.onclose = null
    if (socket.readyState < WebSocket.CLOSING) {
      socket.close(1000, 'Página de monitoramento fechada')
    }
  }
}
