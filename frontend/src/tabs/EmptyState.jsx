/**
 * 적재된 스토리가 0건일 때의 안내. 문구는 state 에 따라 갈린다 — app-ui.md.
 * 세 문구 모두 「데이터 셋업으로 이동」 버튼을 동반한다.
 */
export function emptyMessage(status) {
  if (!status) return { title: '상태를 불러오는 중입니다', detail: '' }
  if (status.state === 'RUNNING') {
    const done = status.completed + status.excluded
    return {
      title: '데이터 셋업이 진행 중입니다',
      detail: `${done} / ${status.target} 처리됨`,
    }
  }
  if (status.state === 'IDLE') {
    return { title: '데이터 셋업을 먼저 완료하세요', detail: '수집·분석이 끝나면 여기에 분포가 그려집니다' }
  }
  return {
    title: '셋업이 실행됐지만 적재된 스토리가 0건입니다',
    detail: '파이프라인 구현을 채우면 분석 결과가 쌓입니다',
  }
}

export default function EmptyState({ status, onGoSetup }) {
  const { title, detail } = emptyMessage(status)
  return (
    <div className="empty">
      <h3>{title}</h3>
      <p>{detail}</p>
      <button className="btn btn-primary" onClick={onGoSetup}>데이터 셋업으로 이동</button>
    </div>
  )
}
