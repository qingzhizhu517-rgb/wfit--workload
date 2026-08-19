<template>
  <el-tag
    v-if="current"
    :type="current.type"
    disable-transitions
  >
    {{ current.label }}
  </el-tag>
  <span v-else>{{ display }}</span>
</template>

<script setup name="BizTag">
import { computed } from 'vue'

const props = defineProps({
  value: { type: [String, Number], default: null },
  map: { type: Object, required: true }
})

const current = computed(() => props.map?.[props.value])
const display = computed(() => (props.value === null || props.value === undefined || props.value === '') ? '-' : props.value)
</script>
