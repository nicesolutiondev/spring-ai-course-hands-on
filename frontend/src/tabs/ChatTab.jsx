import { useEffect, useRef, useState } from 'react'
import { askChat } from '../api.js'

const PLAN_NAMES = ['키워드 완전 일치', '전문 검색', '원문 청크 벡터', '요약 벡터']

/** 프론트가 세션 시작 시 UUID 를 만들어 유지하고 매 요청에 싣는다. 서버가 발급하지 않는다. */
function newConversationId() {
  if (crypto.randomUUID) return crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}

export default function ChatTab() {
  const [conversationId, setConversationId] = useState(newConversationId)
  const [question, setQuestion] = useState('')
  // 턴을 쌓는다. 대화 기억은 conversationId 로 서버가 들고 있으므로 화면도 같이 누적해야
  // 후속 질문("방금 그 이슈")이 무엇을 가리키는지 눈으로 따라갈 수 있다.
  const [turns, setTurns] = useState([])
  const [busy, setBusy] = useState(false)
  const endRef = useRef(null)

  const last = turns[turns.length - 1]

  /** 대화를 새로 시작한다. 서버의 기억은 conversationId 로 갈리므로 id 를 바꾸면 끊긴다. */
  function newConversation() {
    if (busy) return
    setConversationId(newConversationId())
    setTurns([])
    setQuestion('')
  }

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [turns])

  async function submit(e) {
    e.preventDefault()
    const q = question.trim()
    if (!q || busy) return

    const at = turns.length
    setQuestion(''); setBusy(true)
    setTurns((prev) => [...prev, { q, plans: null, evidence: null, answer: '' }])

    const patch = (fn) => setTurns((prev) => prev.map((t, i) => (i === at ? fn(t) : t)))

    try {
      // 점진적 렌더링 — 가장 오래 걸리는 생성 구간에서 빈 화면을 보지 않는다.
      await askChat({
        conversationId,
        question: q,
        onEvent(name, data) {
          if (name === 'plans') patch((t) => ({ ...t, plans: data }))
          else if (name === 'evidence') patch((t) => ({ ...t, evidence: data }))
          else if (name === 'token') patch((t) => ({ ...t, answer: t.answer + data.text }))
        },
      })
    } catch (err) {
      patch((t) => ({ ...t, answer: t.answer + `\n\n(오류: ${err.message})` }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="chat">
      {/* 백엔드가 검색 과정에서 이미 계산하는 값이라 응답 DTO 에 필드 몇 개를 더하면 된다 */}
      <aside className="plans">
        <button className="btn" onClick={newConversation} disabled={busy}
                style={{ width: '100%', marginBottom: 14 }}>+ 새 대화</button>
        <h4>직전 질의 검색 계획</h4>
        {PLAN_NAMES.map((name, i) => {
          const hit = last?.plans?.plans?.find((p) => p.plan === i + 1)
          const hits = hit?.hits ?? 0
          return (
            <div className={`plan ${hits === 0 ? 'zero' : ''}`} key={i}>
              <span className="n">{i + 1}</span>
              <span>{hit?.name ?? name}</span>
              <span className="hits">{last?.plans ? `${hits}건` : '—'}</span>
              {/* 계획이 실제로 실행한 조건. 모델의 설명과 달리 값 그대로다 */}
              {hit?.condition && <span className="cond">{hit.condition}</span>}
            </div>
          )
        })}
        {last?.plans && (
          <div className="merged">
            중복 제외 {last.plans.merged}건 → LLM 적합성 판단 → 최종 {last.evidence?.selected ?? '—'}건
          </div>
        )}
      </aside>

      <section className="thread">
        <div className="messages">
          {turns.length === 0 && (
            <div className="empty">
              <h3>수집된 기술 이슈에 대해 무엇이든 물어보세요</h3>
              <p>검색 계획별 히트 수와 근거가 답변과 함께 표시됩니다</p>
            </div>
          )}

          {turns.map((t, i) => (
            <div key={i} style={{ marginBottom: 18 }}>
              <div className="ask">{t.q}</div>
              <div className="answer">
                <div className="answer-head">
                  <span className="logo">Y</span>
                  {t.evidence ? `근거 ${t.evidence.selected}건을 찾았습니다`
                    : busy && i === turns.length - 1 ? '생각하는 중…' : '답변'}
                </div>
                <div style={{ whiteSpace: 'pre-wrap' }}>{t.answer}</div>

                {t.evidence?.evidence?.length > 0 && (
                  <>
                    <div className="sub" style={{ margin: '18px 0 6px' }}>근거</div>
                    {t.evidence.evidence.map((e) => <EvidenceCard key={e.storyId} e={e} />)}
                  </>
                )}
              </div>
            </div>
          ))}
          <div ref={endRef} />
        </div>

        {/* 입력창은 항상 살아 있다. 데이터가 없으면 서버가 안내 문구를 스트림으로 흘린다. */}
        <div className="composer">
          <form onSubmit={submit}>
            <input value={question} onChange={(e) => setQuestion(e.target.value)}
                   onKeyDown={(e) => {
                     // 암묵적 폼 제출에 기대지 않는다. 채팅에서 Enter 는 핵심 동작이다.
                     if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) submit(e)
                   }}
                   placeholder="수집된 기술 이슈에 대해 무엇이든 물어보세요" disabled={busy} />
            <button className="send" type="submit" disabled={busy || !question.trim()}>↑</button>
          </form>
          <p className="disclaimer">
            수집 스냅샷 기준 · 커뮤니티 의견이므로 사실 검증 대상이 아닙니다
          </p>
        </div>
      </section>
    </div>
  )
}

/**
 * 커뮤니티 반응과 실무 시사점은 검색에 쓰이지 않지만 응답에는 항상 포함된다.
 * 원문 대목은 적합성 판단이 원문 앞부분에서 뽑은 문장이다.
 * passage · practicalImplication 이 없는 스토리는 해당 줄을 생략한다.
 */
function EvidenceCard({ e }) {
  return (
    <div className="evidence">
      <div className="etitle">
        <span>
          {e.url ? <a href={e.url} target="_blank" rel="noreferrer"
                     style={{ color: 'inherit' }}>↗ {e.title}</a> : e.title}
        </span>
        {e.matchedPlans?.length > 0 && (
          <span className="plans-tag">계획 {e.matchedPlans.join('·')}</span>
        )}
      </div>
      <div className="edomain">{e.domain}</div>

      {e.passage && (
        <>
          <div className="field-label">📄 원문 대목</div>
          <div className="field-body passage">“{e.passage}”</div>
        </>
      )}
      {e.communityReaction && (
        <>
          <div className="field-label">💬 커뮤니티 반응</div>
          <div className="field-body">{e.communityReaction}</div>
        </>
      )}
      {e.practicalImplication && (
        <>
          <div className="field-label impl">💡 실무 시사점</div>
          <div className="field-body">{e.practicalImplication}</div>
        </>
      )}
    </div>
  )
}
