<template>
  <el-select
    :model-value="modelValue"
    @update:model-value="onChange"
    filterable
    clearable
    :disabled="disabled"
    :placeholder="placeholder"
    style="width: 100%"
  >
    <el-option
      v-for="u in userList"
      :key="u.userId"
      :value="u.userId"
      :label="u.nickName + '（' + u.userName + '）'"
    >
      <span>{{ u.nickName }}（{{ u.userName }}）</span>
      <span class="user-select-dept">{{ u.deptName || '' }}</span>
    </el-option>
  </el-select>
</template>

<script setup name="UserSelect">
import { useUserMap } from '@/utils/userCache'

defineProps({
  modelValue: { type: [Number, String], default: null },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入姓名或工号筛选' }
})
const emit = defineEmits(['update:modelValue', 'select'])

const { userList, userMap } = useUserMap()

function onChange(val) {
  emit('update:modelValue', val)
  emit('select', userMap.value[val] || null)
}
</script>

<style scoped>
.user-select-dept {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
