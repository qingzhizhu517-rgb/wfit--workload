/** 页面只展示后端核验结果，资格、指针与版本以服务端门禁为准。 */
export function settlementLabel(row) {
  if (row?.settlementEvidenceStatus === 'VERIFIED') {
    return row.settlementMode === 'ISOLATED_VERIFICATION_ONLY' ? '隔离验收' : '依据已固化'
  }
  if (row?.settlementEvidenceStatus === 'PREVIEW_INELIGIBLE') return '资格待确认'
  if (row?.settlementEvidenceStatus === 'STALE') return '需重新结算'
  return row?.settlementRequired ? '结算依据待完善' : '历史依据缺失'
}

export function settlementTagType(row) {
  return row?.settlementEvidenceStatus === 'VERIFIED' && row.settlementMode === 'FORMAL' ? 'success' : 'warning'
}

export function settlementHint(row) {
  const reasons = [row?.eligibilityReason, row?.settlementMissingReason].filter(Boolean)
  if (row?.settlementMode === 'ISOLATED_VERIFICATION_ONLY') reasons.unshift('仅用于隔离验收，未指定正式启用学期。')
  if (row?.settlementEvidenceStatus !== 'VERIFIED' && !reasons.length) reasons.push('历史结算依据未保存，无法确认当时的人员性质与封顶阈值。')
  return reasons.join('；') || '采用本次结算保存的人员、政策、明细及金额依据。'
}

export function settlementSubmitBlocked(row) {
  return row?.settlementRequired === true && row.settlementSubmittable !== true
}
