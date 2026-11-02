import { api } from '@/services/api'

export interface PingResult {
  host: string
  status: 'SUCCESS' | 'NO_REPLY' | 'TIMEOUT' | 'ERROR'
  durationMs: number
  timestamp: number
}

export async function testPing(signal: AbortSignal): Promise<PingResult[]> {
  const { data } = await api.get<PingResult[]>('/test/ping', { signal, timeout: 65_000 })
  if (
    !Array.isArray(data) ||
    data.length !== 10 ||
    !data.every(
      (item) =>
        item &&
        typeof item.host === 'string' &&
        ['SUCCESS', 'NO_REPLY', 'TIMEOUT', 'ERROR'].includes(item.status) &&
        Number.isFinite(item.durationMs) &&
        item.durationMs >= 0 &&
        Number.isFinite(item.timestamp) &&
        Number.isFinite(new Date(item.timestamp).getTime()),
    )
  ) {
    throw new Error('Resposta de ping inválida: esperados 10 resultados')
  }
  return data
}
