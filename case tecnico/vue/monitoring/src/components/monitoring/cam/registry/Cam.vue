<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'
import {
    ElButton,
    ElDescriptions,
    ElDescriptionsItem,
    ElDialog,
    ElDivider,
    ElMessageBox,
} from 'element-plus'
import 'element-plus/dist/index.css'
import CameraForm from './CameraForm.vue'
import CameraTable from './CameraTable.vue'
import {
    createCamera,
    deleteCamera,
    getCamera,
    listCameras,
    type Camera,
    type CameraForm as CameraInput,
} from '@/services/camera/camera.api'

const form = ref<InstanceType<typeof CameraForm>>()
const cameras = ref<Camera[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const acting = ref(false)
const error = ref('')
const notice = ref('')
const selected = ref<Camera>()
const dialog = ref(false)
const busy = computed(() => loading.value || saving.value || acting.value)
const lifetime = new AbortController()
let listRequest: AbortController | undefined

function failure(cause: unknown, fallback: string) {
    if (lifetime.signal.aborted) return
    error.value =
        isAxiosError(cause) && cause.response?.status === 401
            ? 'Sua sessão expirou. Faça login novamente.'
            : fallback
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
    } catch (cause) {
        if (!request.signal.aborted) failure(cause, 'Não foi possível carregar as câmeras.')
    } finally {
        if (listRequest === request) loading.value = false
    }
}

async function save(camera: CameraInput) {
    if (busy.value) return
    saving.value = true
    error.value = notice.value = ''
    try {
        await createCamera(camera, lifetime.signal)
        if (lifetime.signal.aborted) return
        form.value?.reset()
        notice.value = 'Câmera cadastrada.'
        page.value = 1
        await load()
    } catch (cause) {
        failure(
            cause,
            isAxiosError(cause) && cause.response?.status === 400
                ? 'Dados inválidos. Confira o endereço IP, as portas e os campos obrigatórios.'
                : 'Não foi possível cadastrar a câmera.',
        )
    } finally {
        saving.value = false
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
    error.value = notice.value = ''
    try {
        await ElMessageBox.confirm(
            'Excluir a câmera "' + camera.nome + '" e seus registros de monitoramento?',
            'Excluir câmera',
            { type: 'warning', confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar' },
        )
        if (lifetime.signal.aborted) return
        await deleteCamera(camera.id, lifetime.signal)
        if (lifetime.signal.aborted) return
        notice.value = 'Câmera excluída.'
        await load()
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
onMounted(load)
onBeforeUnmount(() => {
    lifetime.abort()
    listRequest?.abort()
    if (acting.value) ElMessageBox.close()
})
</script>

<template>
    <section aria-label="Cadastro de câmeras">
        <h1>Câmeras</h1>
        <h2>Cadastrar câmera</h2>
        <CameraForm ref="form" :loading="busy" @save="save" />
        <p v-if="error" role="alert">{{ error }}</p>
        <p v-if="notice" role="status">{{ notice }}</p>
        <ElDivider />
        <h2>Câmeras cadastradas</h2>
        <ElButton :loading="loading" :disabled="busy" @click="load">Atualizar lista</ElButton>
        <p v-if="loading" role="status">Carregando câmeras…</p>
        <CameraTable :cameras="cameras" :total="total" :page="page" :size="size" :busy="busy" @view="view"
            @remove="remove" @page="changePage" @size="changeSize" />
        <ElDialog v-model="dialog" title="Detalhes da câmera" destroy-on-close>
            <ElDescriptions v-if="selected" :column="1" border>
                <ElDescriptionsItem label="Nome">{{ selected.nome }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Descrição" :rowspan="5">{{ selected.descricao || '—' }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Endereço IP">{{ selected.enderecoip }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Porta HTTP">{{ selected.portaHttp }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Porta RTSP">{{ selected.portaRtsp }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Usuário">{{ selected.usuario }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Senha">{{
                    selected.senhaCadastrada ? '••••••••' : '—'
                    }}</ElDescriptionsItem>
                <ElDescriptionsItem label="Status">{{
                    selected.status ? 'Ativa' : 'Inativa'
                    }}</ElDescriptionsItem>
            </ElDescriptions>
        </ElDialog>
    </section>
</template>
