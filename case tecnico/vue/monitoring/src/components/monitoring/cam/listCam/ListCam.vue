<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'
import {
  ElAlert,
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElMessageBox,
  ElNotification,
} from 'element-plus'
import 'element-plus/dist/index.css'
import CameraTable from '../registry/CameraTable.vue'
import {
  deleteCamera,
  getCamera,
  listCameras,
  listCameraConnections,
  checkCameraConnection,
  type Camera,
  type CameraConnection,
} from '@/services/camera/camera.api'

const cameras = ref<Camera[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const acting = ref(false)
const error = ref('')
const selected = ref<Camera>()
const dialog = ref(false)
const lifetime = new AbortController()
let listRequest: AbortController | undefined
const connections = ref<CameraConnection[]>([])
const connectionError = ref('')
const checking = ref(false)
const checkingId = ref<number>()
let connectionRequest: Promise<void> | undefined
let pollTimer: ReturnType<typeof setTimeout> | undefined
let notification: ReturnType<typeof ElNotification.warning> | undefined
let notifiedIds = new Set<number>()

const busy = computed(() => loading.value || acting.value || checking.value)
const inoperativeCameras = computed(() =>
  connections.value.filter((camera) => camera.state === 'OFFLINE'),
)
const connectionsById = computed(
  () => new Map(connections.value.map((camera) => [camera.id, camera])),
)

function operation(camera: Camera) {
  if (!camera.status) return 'Desabilitada'
  if (checkingId.value === camera.id) return 'Verificando ping e frame…'
  if (connectionError.value) return 'Consulta indisponível'
  const state = connectionsById.value.get(camera.id)?.state ?? 'UNKNOWN'
  return {
    UNKNOWN: 'Aguardando verificação',
    DISABLED: 'Desabilitada',
    ONLINE: 'Operante',
    OFFLINE: 'Inoperante',
  }[state]
}

function checkedAt(id: number) {
  const value = connectionsById.value.get(id)?.checkedAt
  return value ? new Date(value).toLocaleString('pt-BR') : 'Ainda não verificada'
}

function failure(cause: unknown, fallback: string) {
  if (lifetime.signal.aborted) return
  error.value =
    isAxiosError(cause) && cause.response?.status === 401
      ? 'Sua sessão expirou. Faça login novamente.'
      : fallback
}

function notifyInoperativeCameras() {
  const current = new Set(inoperativeCameras.value.map((camera) => camera.id))
  if (current.size === notifiedIds.size && [...current].every((id) => notifiedIds.has(id))) return
  notifiedIds = current
  notification?.close()
  notification = undefined
  if (!current.size) return
  notification = ElNotification.warning({
    title: 'Câmeras inoperantes',
    message: `Verifique: ${inoperativeCameras.value.map((camera) => camera.nome).join(', ')}`,
    duration: 0,
  })
}

function loadConnections(): Promise<void> {
  if (connectionRequest) return connectionRequest
  connectionRequest = (async () => {
    try {
      const data = await listCameraConnections(lifetime.signal)
      if (lifetime.signal.aborted) return
      connections.value = data
      connectionError.value = ''
      notifyInoperativeCameras()
    } catch (cause) {
      if (!lifetime.signal.aborted)
        connectionError.value =
          isAxiosError(cause) && cause.response?.status === 401
            ? 'Sua sessão expirou. Faça login novamente.'
            : 'Não foi possível consultar a operação das câmeras. Os últimos resultados podem estar desatualizados.'
    } finally {
      connectionRequest = undefined
    }
  })()
  return connectionRequest
}

async function pollConnections() {
  if (!checking.value) await loadConnections()
  if (!lifetime.signal.aborted) pollTimer = setTimeout(pollConnections, 5000)
}

async function checkPage() {
  if (busy.value) return
  checking.value = true
  connectionError.value = ''
  try {
    await connectionRequest
    for (const camera of cameras.value.filter((item) => item.status)) {
      if (lifetime.signal.aborted) return
      checkingId.value = camera.id
      const result = await checkCameraConnection(camera.id, lifetime.signal)
      if (lifetime.signal.aborted) return
      connections.value = [...connections.value.filter((item) => item.id !== result.id), result]
      notifyInoperativeCameras()
    }
    await loadConnections()
  } catch (cause) {
    if (!lifetime.signal.aborted)
      connectionError.value =
        isAxiosError(cause) && cause.response?.status === 401
          ? 'Sua sessão expirou. Faça login novamente.'
          : 'Não foi possível concluir a verificação das câmeras. Tente novamente.'
  } finally {
    checkingId.value = undefined
    checking.value = false
  }
}

async function load() {
  listRequest?.abort()
  const request = new AbortController()
  listRequest = request
  loading.value = true
  error.value = ''
  try {
    const data = await listCameras(page.value - 1, size.value, request.signal)
    if (request.signal.aborted) return
    total.value = data.totalElements
    const lastPage = Math.max(1, Math.ceil(total.value / size.value))
    if (page.value > lastPage) {
      page.value = lastPage
      await load()
      return
    }
    cameras.value = data.content
    await loadConnections()
  } catch (cause) {
    if (!request.signal.aborted) failure(cause, 'Não foi possível carregar as câmeras.')
  } finally {
    if (listRequest === request) loading.value = false
  }
}

async function view(id: number) {
  if (busy.value) return
  acting.value = true
  error.value = ''
  try {
    selected.value = await getCamera(id, lifetime.signal)
    if (!lifetime.signal.aborted) dialog.value = true
  } catch (cause) {
    failure(cause, 'Não foi possível carregar os detalhes da câmera.')
  } finally {
    acting.value = false
  }
}

async function remove(camera: Camera) {
  if (busy.value) return
  acting.value = true
  error.value = ''
  try {
    await ElMessageBox.confirm(
      `Excluir a câmera "${camera.nome}" e seus registros de monitoramento?`,
      'Excluir câmera',
      { type: 'warning', confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar' },
    )
    if (lifetime.signal.aborted) return
    await deleteCamera(camera.id, lifetime.signal)
    if (!lifetime.signal.aborted) await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close')
      failure(cause, 'Não foi possível excluir a câmera.')
  } finally {
    acting.value = false
  }
}

function changePage(value: number) {
  page.value = value
  void load()
}

function changeSize(value: number) {
  size.value = value
  page.value = 1
  void load()
}

onMounted(() => {
  void load()
  void pollConnections()
})

onBeforeUnmount(() => {
  clearTimeout(pollTimer)
  notification?.close()
  lifetime.abort()
  listRequest?.abort()
  if (acting.value) ElMessageBox.close()
})
</script>

<template>
  <section aria-label="Câmeras cadastradas">
    <h1>Câmeras cadastradas</h1>
    <div class="list-actions">
      <ElButton :loading="loading" :disabled="busy" @click="load">Atualizar lista</ElButton>
      <ElButton
        :loading="checking"
        :disabled="busy || !cameras.some((camera) => camera.status)"
        @click="checkPage"
      >
        Verificar câmeras desta página
      </ElButton>
    </div>
    <p>
      Operante: ping bem-sucedido e pelo menos um frame recebido. Resultados atualizados a cada 5
      segundos.
    </p>
    <ElAlert
      v-if="inoperativeCameras.length"
      class="inoperative-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="
        connectionError
          ? 'Último resultado: câmeras inoperantes (consulta indisponível)'
          : 'Existem câmeras inoperantes'
      "
    >
      {{ inoperativeCameras.map((camera) => camera.nome).join(', ') }}
    </ElAlert>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="connectionError" role="alert">{{ connectionError }}</p>
    <p v-if="checking" role="status">Verificando ping e frame das câmeras desta página…</p>
    <p v-if="loading" role="status">Carregando câmeras…</p>
    <CameraTable
      :cameras="cameras"
      :total="total"
      :page="page"
      :size="size"
      :busy="busy"
      @view="view"
      @remove="remove"
      @page="changePage"
      @size="changeSize"
    >
      <template #connection="{ camera }">
        <strong>{{ operation(camera) }}</strong>
        <div>Última verificação: {{ checkedAt(camera.id) }}</div>
      </template>
    </CameraTable>
    <ElDialog v-model="dialog" title="Detalhes da câmera" destroy-on-close>
      <ElDescriptions v-if="selected" :column="1" border>
        <ElDescriptionsItem label="Nome">{{ selected.nome }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Descrição">{{ selected.descricao || '—' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Endereço IP">{{ selected.enderecoip }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Porta HTTP">{{ selected.portaHttp }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Porta RTSP">{{ selected.portaRtsp }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Usuário">{{ selected.usuario }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Senha">{{
          selected.senhaCadastrada ? '••••••••' : '—'
        }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Cadastro">{{
          selected.status ? 'Ativa' : 'Inativa'
        }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Operação">{{ operation(selected) }}</ElDescriptionsItem>
        <ElDescriptionsItem label="Última verificação">{{
          checkedAt(selected.id)
        }}</ElDescriptionsItem>
      </ElDescriptions>
    </ElDialog>
  </section>
</template>

<style scoped>
.list-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.list-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
@media (max-width: 767px) {
  .list-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
.inoperative-alert {
  margin: 16px 0;
}
</style>
