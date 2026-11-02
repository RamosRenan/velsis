### Usuário e senha default.
> username: admin / passwd: admin
### WebSocket de disco

Endpoint: `ws://localhost:8080/ws/monitor/disc` (use `wss://` com HTTPS).
Requer login previo pela API; o navegador envia o cookie de sessao automaticamente.
A origem permitida por padrao e `http://localhost:5173`.

Cada mensagem e um objeto JSON no formato de `DiscMeasurementRecord`:
```json
{"totalBytes":1000000000,"freeByte":500000000,"timestamp":1793443200000}
```
Os espacos sao medidos em bytes; `freeByte` representa o espaco nao alocado.
O timestamp e Unix em milissegundos. A primeira medicao e enviada ao conectar;
as seguintes sao enviadas a cada segundo enquanto houver clientes conectados.

Configuracao opcional em `application.yml`:
```yaml
monitoring:
  disc:
    path: "." # Disco que contem este caminho existente
    interval-ms: 1000
    allowed-origins: "http://localhost:5173"
```

Exemplo Vue 3 (apos autenticar com credentials: 'include'):
```vue
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const disc = ref(null)
const connected = ref(false)
let socket

onMounted(() => {
  socket = new WebSocket('ws://localhost:8080/ws/monitor/disc')
  socket.onopen = () => { connected.value = true }
  socket.onmessage = ({ data }) => { disc.value = JSON.parse(data) }
  socket.onclose = () => { connected.value = false }
  socket.onerror = () => { connected.value = false }
})
onBeforeUnmount(() => socket?.close())
</script>

<template>
  <p v-if="!connected">Desconectado</p>
  <div v-if="disc">
    <p>Total: {{ disc.totalBytes }} bytes</p>
    <p>Livre: {{ disc.freeByte }} bytes</p>
    <p>Atualizado: {{ new Date(disc.timestamp).toLocaleString() }}</p>
  </div>
</template>
```
ffmpeg -re `
-f lavfi -i "testsrc2=size=1280x720:rate=25" `
-f lavfi -i "sine=frequency=440:sample_rate=48000" `
-c:v libx264 `
-profile:v baseline `
-preset ultrafast `
-tune zerolatency `
-pix_fmt yuv420p `
-c:a libopus `
-ar 48000 `
-ac 2 `
-b:a 128k `
-rtsp_transport tcp `
-f rtsp `
rtsp://admin:admin@localhost:8554/camera1

ffmpeg -re -f lavfi -i "testsrc2=size=1280x720:rate=25" \
-f lavfi -i "sine=frequency=440:sample_rate=48000" -c:v libx264 \
-profile:v baseline \
-preset ultrafast \
-tune zerolatency \
-pix_fmt yuv420p \
-c:a libopus \
-ar 48000 \
-ac 2 \
-b:a 128k \
-rtsp_transport tcp \
-f rtsp "rtsp://admin:admin@127.0.0.1:8554/camera2"