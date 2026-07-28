<template>
  <div class="markdown-editor">
    <MdEditor v-model="content" @change="$emit('update:modelValue', content)" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const content = ref(props.modelValue || '')

watch(() => props.modelValue, (v) => { content.value = v || '' })
</script>
