<script setup>
import { ref, onBeforeUnmount } from 'vue';
import Hls from 'hls.js';

const mode = ref('hls');
const video = ref(null);
const message = ref('Escolha HLS ou WebRTC para visualizar a câmera 1.');
const iframe = ref('');
const busy = ref(false);
const failed = ref(false);
let hls = null;
let controller = null;
let generation = 0;

function cleanup() {
  generation++;
  controller?.abort();
  hls?.destroy();
  hls = null;
  if (video.value) {
    video.value.pause();
    video.value.removeAttribute('src');
    video.value.load();
  }
  iframe.value = '';
}

async function request(url, signal, options = {}) {
  let response;
  try {
    response = await fetch(url, { ...options, signal: AbortSignal.any([signal, AbortSignal.timeout(10000)]) });
  } catch (error) {
    signal.throwIfAborted();
    throw new Error('Não foi possível acessar o Spring. Verifique o backend e BACKEND_URL.');
  }
  if (!response.ok) {
    const body = await response.text();
    let detail = '';
    try { const data = JSON.parse(body); detail = data.message || data.error || ''; }
    catch {
      if (response.status === 403 && body.trim() === 'Invalid CORS request') {
        detail = `Origem ${window.location.origin} bloqueada pelo CORS do Spring. Verifique as origens permitidas e reinicie o backend`;
      }
    }
    throw new Error(`Spring retornou HTTP ${response.status}${detail ? `: ${detail}` : ''}. Consulte o log do backend.`);
  }
  return response;
}

async function waitForPlaylist(url, signal) {
  const deadline = Date.now() + 45000;
  while (Date.now() < deadline) {
    const response = await fetch(url, { cache: 'no-store', signal: AbortSignal.any([signal, AbortSignal.timeout(5000)]) });
    if (response.ok) {
      const playlist = await response.text();
      if (playlist.startsWith('#EXTM3U') && playlist.includes('#EXTINF:')) return;
    } else if (response.status !== 404) {
      throw new Error(`Falha ao consultar a playlist (HTTP ${response.status}). Verifique o Spring.`);
    }
    await new Promise(resolve => setTimeout(resolve, 1000));
    signal.throwIfAborted();
  }
  throw new Error('O Spring não gerou o vídeo em 45 segundos. Verifique a fonte RTSP, o FFmpeg e backend/runtime-hls/camera-1/ffmpeg.log.');
}

async function play(current) {
  try { await video.value.play(); }
  catch { if (current === generation) message.value = 'Vídeo carregado. Pressione Play no player para iniciar.'; }
}

async function start(nextMode) {
  cleanup();
  const current = generation;
  controller = new AbortController();
  const signal = controller.signal;
  busy.value = true;
  failed.value = false;
  mode.value = nextMode;
  message.value = nextMode === 'hls' ? 'Iniciando HLS e aguardando os segmentos do Spring…' : 'Provisionando WebRTC…';
  try {
    if (nextMode === 'webrtc') {
      const response = await request('/api/cameras/1/webrtc/provision', signal, { method: 'POST' });
      const data = await response.json();
      if (current !== generation) return;
      const url = new URL(data.playerUrl);
      if (!['http:', 'https:'].includes(url.protocol)) throw new Error('URL WebRTC inválida.');
      iframe.value = url.href;
      message.value = 'Player WebRTC aberto. A conexão e os erros de vídeo aparecem no próprio player.';
      return;
    }
    const response = await request('/api/cameras/1/hls/start', signal, { method: 'POST' });
    const data = await response.json();
    if (current !== generation) return;
    if (typeof data.playlist !== 'string' || !data.playlist.startsWith('/api/')) throw new Error('O Spring retornou uma playlist inválida.');
    await waitForPlaylist(data.playlist, signal);
    if (current !== generation) return;
    message.value = 'Carregando vídeo HLS…';
    if (Hls.isSupported()) {
      hls = new Hls();
      hls.on(Hls.Events.MANIFEST_PARSED, () => play(current));
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal || current !== generation) return;
        failed.value = true;
        message.value = `Transmissão interrompida (${data.details}). Verifique a câmera e o Spring e clique em Iniciar HLS para reconectar.`;
        hls?.destroy();
        hls = null;
      });
      hls.attachMedia(video.value);
      hls.loadSource(data.playlist);
    } else if (video.value.canPlayType('application/vnd.apple.mpegurl')) {
      video.value.src = data.playlist;
      await play(current);
    } else throw new Error('Este navegador não suporta HLS. Utilize um navegador atualizado.');
  } catch (error) {
    if (current !== generation) return;
    failed.value = true;
    message.value = error.message;
  } finally { if (current === generation) busy.value = false; }
}

async function stop() {
  const previousMode = mode.value;
  cleanup();
  const current = generation;
  controller = new AbortController();
  busy.value = true;
  failed.value = false;
  try {
    if (previousMode === 'hls') await request('/api/cameras/1/hls/stop', controller.signal, { method: 'POST' });
    if (current === generation) message.value = 'Reprodução parada.';
  } catch (error) {
    if (current !== generation) return;
    failed.value = true;
    message.value = `Player parado localmente. ${error.message}`;
  } finally { if (current === generation) busy.value = false; }
}

function onPlaying() {
  if (!failed.value) message.value = 'Reproduzindo HLS via Spring. Ative o áudio nos controles do player.';
}
function onVideoError() {
  if (!video.value?.error || failed.value) return;
  failed.value = true;
  message.value = 'O navegador não conseguiu reproduzir o vídeo. Verifique os codecs e o log do FFmpeg.';
}
onBeforeUnmount(cleanup);
</script>

<template>
  <main>
    <h1>Câmera ao vivo — RTSP → Spring</h1>
    <p>HLS entrega vídeo pelo Spring com alguns segundos de atraso. WebRTC usa o gateway para menor latência.</p>
    <div class="buttons">
      <button :disabled="busy" @click="start('hls')">Iniciar HLS</button>
      <button :disabled="busy" @click="start('webrtc')">Iniciar WebRTC</button>
      <button @click="stop">Parar</button>
    </div>
    <p role="status" aria-live="polite" :class="{ error: failed }">{{ message }}</p>
    <video v-show="mode === 'hls'" ref="video" controls autoplay muted playsinline @playing="onPlaying"
      @error="onVideoError"></video>
    <iframe v-if="mode === 'webrtc' && iframe" :src="iframe" title="Vídeo ao vivo da câmera 1 via WebRTC"
      allow="autoplay; fullscreen" allowfullscreen></iframe>
  </main>
</template>

<style>
body {
  font: 16px system-ui;
  background: #101929;
  color: #f5f7fa;
  margin: 0;
}

main {
  max-width: 950px;
  margin: 35px auto;
  padding: 24px;
}

.buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

button {
  cursor: pointer;
  padding: 12px 18px;
  border: 0;
  border-radius: 7px;
  background: #287de8;
  color: white;
}

button:disabled {
  opacity: .5;
  cursor: wait;
}

.error {
  color: #ffb4b4;
}

video,
iframe {
  width: 100%;
  aspect-ratio: 16/9;
  background: #000;
  border: 0;
  border-radius: 10px;
  margin-top: 12px;
}
</style>
