import { api } from '@/services/api'
import { csrfStore } from '@/stores/auth/csrf'

export interface CameraForm {
  nome: string
  descricao: string
  enderecoip: string
  portaHttp: number
  portaRtsp: number
  usuario: string
  senha: string
  status: boolean
}
export interface Camera extends Omit<CameraForm, 'senha'> {
  id: number
  senhaCadastrada: boolean
}
export interface CameraPage {
  content: Camera[]
  totalElements: number
  page: number
  size: number
}
export interface CameraConnection {
  id: number
  nome: string
  state: 'UNKNOWN' | 'DISABLED' | 'ONLINE' | 'OFFLINE'
  checkedAt: string | null
}

export async function listCameraConnections(signal: AbortSignal) {
  const { data } = await api.get<CameraConnection[]>('/cameras/connections', { signal })
  return data
}

export async function checkCameraConnection(id: number, signal: AbortSignal) {
  const { data } = await api.post<CameraConnection>(`/cameras/${id}/connection/check`, null, {
    headers: await headers(),
    signal,
  })
  return data
}
async function headers() {
  const token = await csrfStore().refresh()
  return { [token.headerName]: token.token }
}
export async function listCameras(page: number, size: number, signal: AbortSignal) {
  const { data } = await api.get<CameraPage>('/cameras', { params: { page, size }, signal })
  return data
}
export async function getCamera(id: number, signal: AbortSignal) {
  const { data } = await api.get<Camera>('/cameras/' + id, { signal })
  return data
}
export async function createCamera(camera: CameraForm, signal: AbortSignal) {
  const { data } = await api.post<Camera>('/cameras', camera, { headers: await headers(), signal })
  return data
}
export async function deleteCamera(id: number, signal: AbortSignal) {
  await api.delete('/cameras/' + id, { headers: await headers(), signal })
}
