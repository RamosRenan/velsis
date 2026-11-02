<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElButton } from 'element-plus'
import Hls from 'hls.js'
import { isAxiosError } from 'axios'
import { api } from '@/services/api'
import { csrfStore } from '@/stores/auth/csrf'

const props = defineProps<{ camera: string }>()
const video = ref<HTMLVideoElement>()
const message = ref('')
const busy = ref(false)
const failed = ref(false)
const csrf = csrfStore()
const endpoint = '/cameras/by-name/' + encodeURIComponent(props.camera) + '/hls'
let hls: Hls | undefined
let controller: AbortController | undefined
let generation = 0

function cleanup() {
  generation++
  controller?.abort()
  hls?.destroy()
  hls = undefined
  if (video.value) {
    video.value.pause()
    video.value.removeAttribute('src')
    video.value.load()
  }
}

function failure(error: unknown) {
  if (isAxiosError(error)) {
    if (error.response?.status === 401) return 'Sua sessão expirou. Faça login novamente.'
    if (error.response?.status === 403)
      return 'Acesso negado pelo servidor. Atualize a sessão e tente novamente.'
    if (error.response?.status === 404) return 'Câmera não encontrada.'
    if (error.response?.status === 409) return 'Esta câmera está inativa.'
    if (error.response?.status === 503)
      return 'Transmissão indisponível. Verifique a câmera e tente novamente.'
    return error.response
      ? `O servidor retornou HTTP ${error.response.status}. Tente novamente.`
      : 'Não foi possível conectar ao backend. Verifique se ele está em execução.'
  }
  return error instanceof Error ? error.message : 'Não foi possível reproduzir o vídeo.'
}

async function startStream(signal: AbortSignal) {
  const token = await csrf.refresh()
  signal.throwIfAborted()
  const { data } = await api.post<{ playlist: string }>(endpoint + '/start', null, {
    signal,
    timeout: 10000,
    headers: { [token.headerName]: token.token },
  })
  const backend = new URL(api.getUri({ url: endpoint + '/start' }), window.location.origin)
  if (typeof data.playlist !== 'string' || !data.playlist.startsWith('/api/')) {
    throw new Error('O backend retornou uma playlist inválida.')
  }
  const playlist = new URL(data.playlist, backend)
  if (playlist.origin !== backend.origin) throw new Error('Origem da playlist inválida.')
  return playlist.href
}

function delay(signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    signal.throwIfAborted()
    const abort = () => {
      clearTimeout(timer)
      reject(signal.reason)
    }
    const timer = setTimeout(() => {
      signal.removeEventListener('abort', abort)
      resolve()
    }, 1000)
    signal.addEventListener('abort', abort, { once: true })
  })
}

async function waitForPlaylist(playlist: string, signal: AbortSignal) {
  const deadline = Date.now() + 45000
  while (Date.now() < deadline) {
    const response = await api.get<string>(playlist, {
      signal,
      timeout: 5000,
      responseType: 'text',
      validateStatus: (status) => status === 200 || status === 404,
    })
    if (
      response.status === 200 &&
      response.data.startsWith('#EXTM3U') &&
      response.data.includes('#EXTINF:')
    )
      return
    await delay(signal)
  }
  throw new Error(
    'A câmera não enviou vídeo em 45 segundos. Verifique a conexão e tente novamente.',
  )
}

async function play(current: number) {
  try {
    await video.value?.play()
  } catch {
    if (current === generation) message.value = 'Vídeo carregado. Pressione Play para iniciar.'
  }
}

async function start() {
  cleanup()
  const current = generation
  controller = new AbortController()
  const signal = controller.signal
  busy.value = true
  failed.value = false
  message.value = 'Conectando à câmera…'
  let stage = 'Não foi possível iniciar a câmera'
  try {
    const playlist = await startStream(signal)
    if (current !== generation) return
    stage = 'Não foi possível carregar o vídeo'
    message.value = 'Câmera iniciada. Aguardando o vídeo…'
    const element = video.value
    if (!element || (!Hls.isSupported() && !element.canPlayType('application/vnd.apple.mpegurl'))) {
      throw new Error('Este navegador não suporta a reprodução deste vídeo.')
    }
    await waitForPlaylist(playlist, signal)
    if (current !== generation) return
    message.value = 'Carregando vídeo…'
    if (Hls.isSupported()) {
      hls = new Hls({
        xhrSetup: (xhr) => {
          xhr.withCredentials = true
        },
      })
      hls.on(Hls.Events.MANIFEST_PARSED, () => void play(current))
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal || current !== generation) return
        failed.value = true
        message.value = 'Transmissão interrompida. Clique em Reconectar para tentar novamente.'
        hls?.destroy()
        hls = undefined
      })
      hls.attachMedia(element)
      hls.loadSource(playlist)
    } else {
      element.src = playlist
      await play(current)
    }
  } catch (error) {
    if (current !== generation) return
    failed.value = true
    message.value = `${stage}. ${failure(error)}`
  } finally {
    if (current === generation) busy.value = false
  }
}

function stop() {
  // The server process is shared by viewers; leaving/stopping this player must
  // not terminate the transmission for other viewers.
  cleanup()
  busy.value = false
  failed.value = false
  message.value = 'Reprodução parada.'
}

function onPlaying() {
  if (!failed.value) message.value = 'Ao vivo. Ative o áudio nos controles do vídeo.'
}

function onVideoError() {
  if (!video.value?.error || failed.value) return
  failed.value = true
  message.value = 'Não foi possível reproduzir o vídeo. Tente reconectar.'
}

onMounted(start)
onBeforeUnmount(cleanup)
</script>

<template>
  <section class="camera-stream" :aria-label="'Vídeo da câmera ' + camera">
    <div class="actions">
      <ElButton type="primary" :disabled="busy" @click="start">Reconectar</ElButton>
      <ElButton @click="stop">Parar reprodução</ElButton>
    </div>
    <p role="status" aria-live="polite" :class="{ error: failed }">{{ message }}</p>
    <video
      ref="video"
      controls
      autoplay
      muted
      playsinline
      crossorigin="use-credentials"
      :aria-label="'Transmissão da câmera ' + camera"
      @playing="onPlaying"
      @error="onVideoError"
    />
  </section>
</template>

<style scoped>
.camera-stream {
  width: 100%;
  max-width: 1100px;
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.camera-stream > p {
  margin: 12px 0;
}
@media (max-width: 767px) {
  .actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .actions :deep(.el-button) {
    width: 100%;
  }
}
.error {
  color: #c45656;
}
video {
  display: block;
  width: 100%;
  object-fit: contain;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 10px;
}
</style>
