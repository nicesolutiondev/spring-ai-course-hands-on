import { useCallback, useEffect, useState } from 'react'
import { getSetupStatus, openSetupStream } from './api.js'
import SetupTab from './tabs/SetupTab.jsx'
import TopicsTab from './tabs/TopicsTab.jsx'
import ChatTab from './tabs/ChatTab.jsx'

const TABS = [
  { id: 'setup', label: '데이터 셋업' },
  { id: 'topics', label: '주제 탐색' },
  { id: 'chat', label: 'Q&A' },
]

export default function App() {
  const [tab, setTab] = useState('setup')
  const [status, setStatus] = useState(null)

  // 부팅 시 1회 조회해 탭 잠금 상태를 초기화하고, 이후 갱신은 SSE 가 맡는다.
  useEffect(() => {
    getSetupStatus().then(setStatus).catch(() => {})
    return openSetupStream({
      onProgress: setStatus,
      onDone: () => getSetupStatus().then(setStatus).catch(() => {}),
    })
  }, [])

  const goSetup = useCallback(() => setTab('setup'), [])

  return (
    <>
      <header className="topbar">
        <div className="brand">
          <span className="logo">Y</span>
          HN Issue Analyzer
        </div>

        {/* 탭은 항상 클릭 가능하다. 잠그는 것은 이동이 아니라 내용이다. */}
        <nav className="tabs" role="tablist">
          {TABS.map((t) => (
            <button
              key={t.id}
              role="tab"
              className="tab"
              aria-selected={tab === t.id}
              onClick={() => setTab(t.id)}
            >
              {t.id === 'setup' && <SetupBadge status={status} />}
              {t.label}
            </button>
          ))}
        </nav>

        <span className="host">localhost:8080</span>
      </header>

      {tab === 'setup' && <SetupTab status={status} onStatus={setStatus} />}
      {tab === 'topics' && <TopicsTab status={status} onGoSetup={goSetup} />}
      {/* Q&A 만 언마운트하지 않는다. 언마운트하면 conversationId 가 새로 만들어져
          탭을 다녀오는 것만으로 대화가 끊긴다. 새 대화는 버튼으로만 시작한다. */}
      <div style={{ display: tab === 'chat' ? 'contents' : 'none' }}>
        <ChatTab status={status} />
      </div>
    </>
  )
}

/**
 * 배지 3상태. 재실행 중에도 체크만 떠 있으면 아무 일도 안 일어나는 것처럼 보인다.
 * 판정 근거는 searchableStories 다 — state 는 앱을 재기동하면 IDLE 로 돌아가지만
 * DB 의 데이터는 남아 있다.
 */
function SetupBadge({ status }) {
  if (!status) return null
  if (status.state === 'RUNNING') return <i className="badge-spin" />
  if (status.searchableStories > 0) return <span className="badge-check">✓</span>
  return null
}
