<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElSwitch,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import type { CameraForm } from '@/services/camera/camera.api'

defineProps<{ loading: boolean }>()
const emit = defineEmits<{ save: [camera: CameraForm] }>()
const defaults = (): CameraForm => ({
  nome: '',
  descricao: '',
  enderecoip: '',
  portaHttp: 80,
  portaRtsp: 554,
  usuario: '',
  senha: '',
  status: true,
})
const form = reactive(defaults())
const formRef = ref<FormInstance>()
const rules: FormRules = {
  nome: [
    { required: true, whitespace: true, message: 'Informe o nome.', trigger: 'blur' },
    {
      pattern: /^(?!\s*\/)/,
      message: 'O nome não pode começar com "/".',
      trigger: ['blur', 'change'],
    },
  ],
  enderecoip: [
    { required: true, whitespace: true, message: 'Informe o endereço IP.', trigger: 'blur' },
  ],
  usuario: [{ required: true, whitespace: true, message: 'Informe o usuário.', trigger: 'blur' }],
  senha: [{ required: true, whitespace: true, message: 'Informe a senha.', trigger: 'blur' }],
  portaHttp: [
    {
      required: true,
      type: 'number',
      min: 1,
      max: 65535,
      message: 'Porta entre 1 e 65535.',
      trigger: 'change',
    },
  ],
  portaRtsp: [
    {
      required: true,
      type: 'number',
      min: 1,
      max: 65535,
      message: 'Porta entre 1 e 65535.',
      trigger: 'change',
    },
  ],
}
async function submit() {
  if (await formRef.value?.validate().catch(() => false)) emit('save', { ...form })
}
function reset() {
  Object.assign(form, defaults())
  formRef.value?.clearValidate()
}
defineExpose({ reset })
</script>

<template>
  <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="loading" @submit.prevent="submit">
    <ElFormItem label="Nome (path rtsp ex.: camera1)" prop="nome">
      <ElInput v-model="form.nome" :maxlength="150" />
    </ElFormItem>
    <ElFormItem label="Descrição" prop="descricao">
      <ElInput v-model="form.descricao" type="textarea" :maxlength="1000" />
    </ElFormItem>
    <ElFormItem label="Endereço IP ou localhost" prop="enderecoip">
      <ElInput v-model="form.enderecoip" :maxlength="45" placeholder="192.168.1.100 ou localhost" />
    </ElFormItem>
    <ElFormItem label="Porta HTTP" prop="portaHttp">
      <ElInputNumber v-model="form.portaHttp" :min="1" :max="65535" :precision="0" />
    </ElFormItem>
    <ElFormItem label="Porta RTSP" prop="portaRtsp">
      <ElInputNumber v-model="form.portaRtsp" :min="1" :max="65535" :precision="0" />
    </ElFormItem>
    <ElFormItem label="Usuário" prop="usuario">
      <ElInput v-model="form.usuario" :maxlength="100" autocomplete="off" />
    </ElFormItem>
    <ElFormItem label="Senha" prop="senha">
      <ElInput v-model="form.senha" type="password" show-password :maxlength="255" autocomplete="new-password" />
    </ElFormItem>
    <ElFormItem label="Status" prop="status">
      <ElSwitch v-model="form.status" active-text="Ativa" inactive-text="Inativa" />
    </ElFormItem>
    <ElButton type="primary" native-type="submit" :loading="loading">Cadastrar câmera</ElButton>
  </ElForm>
</template>
