<script setup lang="ts">
import { ElButton, ElPagination, ElTable, ElTableColumn } from 'element-plus'
import { useRouter } from 'vue-router'
import type { Camera } from '@/services/camera/camera.api'

const router = useRouter()

defineProps<{ cameras: Camera[]; total: number; page: number; size: number; busy: boolean }>()
defineEmits<{
  view: [id: number]
  remove: [camera: Camera]
  page: [page: number]
  size: [size: number]
}>()
</script>

<template>
  <div class="camera-cards">
    <p v-if="!cameras.length">Nenhuma câmera cadastrada.</p>
    <article v-for="camera in cameras" :key="camera.id" class="camera-card">
      <h3>{{ camera.nome }}</h3>
      <p v-if="camera.descricao">{{ camera.descricao }}</p>
      <dl>
        <div>
          <dt>Endereço IP</dt>
          <dd>{{ camera.enderecoip }}</dd>
        </div>
        <div>
          <dt>Portas HTTP / RTSP</dt>
          <dd>{{ camera.portaHttp }} / {{ camera.portaRtsp }}</dd>
        </div>
        <div>
          <dt>Usuário</dt>
          <dd>{{ camera.usuario }}</dd>
        </div>
        <div class="card-status">
          <dt>{{ $slots.connection ? 'Status' : 'Cadastro' }}</dt>
          <dd>
            <slot name="connection" :camera="camera">{{
              camera.status ? 'Ativa' : 'Inativa'
            }}</slot>
          </dd>
        </div>
      </dl>
      <div class="card-actions">
        <ElButton
          type="primary"
          :disabled="busy"
          @click="router.push({ path: '/web/monitor/cam', query: { camera: camera.nome } })"
          >Acessar</ElButton
        >
        <ElButton :disabled="busy" @click="$emit('view', camera.id)">Detalhes</ElButton>
        <ElButton type="danger" plain :disabled="busy" @click="$emit('remove', camera)"
          >Excluir</ElButton
        >
      </div>
    </article>
  </div>
  <ElTable
    class="camera-desktop-table"
    :data="cameras"
    row-key="id"
    empty-text="Nenhuma câmera cadastrada."
  >
    <ElTableColumn prop="nome" label="Nome" min-width="140" />
    <ElTableColumn prop="descricao" label="Descrição" min-width="180" show-overflow-tooltip />
    <ElTableColumn prop="enderecoip" label="Endereço IP" min-width="140" />
    <ElTableColumn prop="portaHttp" label="Porta HTTP" width="110" />
    <ElTableColumn prop="portaRtsp" label="Porta RTSP" width="110" />
    <ElTableColumn prop="usuario" label="Usuário" min-width="120" />
    <ElTableColumn
      :label="$slots.connection ? 'Status' : 'Cadastro'"
      :min-width="$slots.connection ? 210 : 100"
    >
      <template #default="{ row }">
        <slot name="connection" :camera="row as Camera">
          {{ row.status ? 'Ativa' : 'Inativa' }}
        </slot>
      </template>
    </ElTableColumn>
    <ElTableColumn label="Ações" width="300" fixed="right">
      <template #default="{ row }">
        <div class="camera-actions">
          <ElButton :disabled="busy" @click="$emit('view', row.id)">Visualizar</ElButton>
          <ElButton
            type="primary"
            :disabled="busy"
            @click="router.push({ path: '/web/monitor/cam', query: { camera: row.nome } })"
            >Acessar</ElButton
          >
          <ElButton type="danger" :disabled="busy" @click="$emit('remove', row as Camera)"
            >Excluir</ElButton
          >
        </div>
      </template>
    </ElTableColumn>
  </ElTable>
  <ElPagination
    :current-page="page"
    :page-size="size"
    :total="total"
    :pager-count="5"
    :page-sizes="[5, 10, 20, 50]"
    :disabled="busy"
    layout="total, sizes, prev, pager, next"
    @current-change="$emit('page', $event)"
    @size-change="$emit('size', $event)"
  />
</template>

<style scoped>
.camera-cards {
  display: none;
}
@media (max-width: 767px) {
  .camera-desktop-table {
    display: none;
  }
  .camera-cards {
    display: grid;
    gap: 16px;
    margin-top: 16px;
  }
  .camera-card {
    padding: 16px;
    border: 1px solid var(--color-border);
    border-radius: 12px;
    background: var(--color-background-soft);
    min-width: 0;
    overflow-wrap: anywhere;
  }
  .camera-card h3 {
    font-size: 17px;
    font-weight: 600;
    margin-bottom: 8px;
  }
  .camera-card dl {
    display: grid;
    gap: 10px;
    margin: 16px 0;
  }
  .camera-card dl > div {
    display: flex;
    justify-content: space-between;
    gap: 16px;
  }
  .camera-card dt {
    flex-shrink: 0;
    font-size: 13px;
  }
  .camera-card dd {
    text-align: right;
    min-width: 0;
  }
  .camera-card dl > .card-status {
    display: block;
  }
  .card-status dd {
    text-align: left;
    margin-top: 4px;
  }
  .card-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .card-actions :deep(.el-button) {
    margin: 0;
  }
  .card-actions :deep(.el-button:first-child) {
    grid-column: 1 / -1;
  }
}
.camera-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.camera-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
