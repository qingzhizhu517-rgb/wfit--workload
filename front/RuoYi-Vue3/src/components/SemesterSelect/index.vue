<template>
  <el-select
    v-model="selectedValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :size="size"
    :style="{ width: width }"
    filterable
    @change="handleChange"
  >
    <el-option
      v-for="item in semesterOptions"
      :key="item.value"
      :label="item.label"
      :value="item.value"
    />
  </el-select>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请选择学年学期'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  size: {
    type: String,
    default: undefined
  },
  width: {
    type: String,
    default: '100%'
  },
  /** 向前推算的年数，默认 5 年 */
  pastYears: {
    type: Number,
    default: 5
  },
  /** 向后推算的年数，默认 1 年 */
  futureYears: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValue = ref(props.modelValue)

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  selectedValue.value = val
})

// 生成学期选项
const semesterOptions = computed(() => {
  const options = []
  const now = new Date()
  const currentYear = now.getFullYear()
  const startYear = currentYear - props.pastYears
  const endYear = currentYear + props.futureYears

  for (let year = startYear; year <= endYear; year++) {
    // 第一学期：秋季（当年9月 - 次年1月）
    options.push({
      label: `${year}-${year + 1}-1`,
      value: `${year}-${year + 1}-1`
    })
    // 第二学期：春季（次年2月 - 次年7月）
    options.push({
      label: `${year}-${year + 1}-2`,
      value: `${year}-${year + 1}-2`
    })
  }

  // 按倒序排列（最新的在前面）
  return options.reverse()
})

function handleChange(val) {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>
