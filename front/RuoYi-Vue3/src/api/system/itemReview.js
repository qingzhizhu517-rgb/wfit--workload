import request from '@/utils/request'
export const listItemReviews = params => request({ url: '/system/auditRecord/list', method: 'get', params })
export const getItemReview = id => request({ url: `/system/auditRecord/${id}`, method: 'get' })
export const reviewItem = (id, action, data) => request({ url: `/system/auditRecord/${id}/${action.toLowerCase()}`, method: 'post', data, headers: { repeatSubmit: false } })
export const reviewBatch = (action, data) => request({ url: `/system/auditRecord/batch/${action.toLowerCase()}`, method: 'post', data, headers: { repeatSubmit: false } })
export const addOfflineNote = (summaryId, data, key) => request({ url: `/system/offlineAdjustmentNote/${summaryId}`, method: 'post', data, headers: { 'Idempotency-Key': key, repeatSubmit: false } })
