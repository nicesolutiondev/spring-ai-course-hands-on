import { useEffect, useState } from 'react'
import { getTopics, getTopicStories } from '../api.js'
import EmptyState from './EmptyState.jsx'

export default function TopicsTab({ status, onGoSetup }) {
  const [stats, setStats] = useState(null)
  const [selected, setSelected] = useState(null)
  const [sort, setSort] = useState('score')
  const [stories, setStories] = useState(null)

  const ready = (status?.searchableStories ?? 0) > 0

  useEffect(() => {
    if (!ready) return
    getTopics().then((s) => {
      setStats(s)
      setSelected((prev) => prev ?? s.techFields[0]?.value ?? null)
    }).catch(() => {})
  }, [ready])

  useEffect(() => {
    if (!selected) return
    getTopicStories(selected, sort).then(setStories).catch(() => {})
  }, [selected, sort])

  // 보여줄 분포 자체가 없다. 판정 근거는 searchableStories 하나다.
  if (!ready) {
    return (
      <div className="page">
        <h1>주제 탐색</h1>
        <div className="card"><EmptyState status={status} onGoSetup={onGoSetup} /></div>
      </div>
    )
  }
  if (!stats) return <div className="page"><p className="sub">불러오는 중…</p></div>

  const selectedLabel = stats.techFields.find((f) => f.value === selected)?.label ?? selected
  const maxCount = Math.max(1, ...stats.techFields.map((f) => f.count))

  return (
    <div className="page">
      <h1>주제 탐색</h1>
      <p className="sub">적재된 {stats.stats.stories}건이 어떤 기술 분야와 원문 타입으로 나뉘는지 보여줍니다</p>

      <div className="grid-stats">
        <Stat value={stats.stats.stories} label="검색 대상 스토리" />
        <Stat value={stats.stats.techFields} label="기술 분야" />
        <Stat value={stats.stats.categories} label="원문 타입" />
        <Stat value={stats.stats.chunks} label="원문 청크" />
        <Stat value={stats.stats.keywords} label="키워드" />
      </div>

      <div className="topics">
        <div style={{ display: 'grid', gap: 14 }}>
          <section className="card">
            <div className="card-head">
              <div>
                <h2>기술 분야</h2>
                <div className="desc" style={{ color: 'var(--muted)', fontSize: 12 }}>
                  LLM 이 분류한 techField 기준
                </div>
              </div>
            </div>
            <div className="card-body">
              {stats.techFields.map((f) => (
                <div key={f.value} className="field" aria-selected={f.value === selected}
                     onClick={() => setSelected(f.value)}>
                  <span>{f.label}</span>
                  <span className="fbar"><i style={{ width: `${(f.count / maxCount) * 100}%` }} /></span>
                  <span className="fnum">{f.count}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="card">
            <div className="card-head"><h2>원문 타입</h2></div>
            <div className="card-body">
              <div className="cat-grid">
                {stats.categories.map((c) => (
                  <div className="cat" key={c.value}><span>{c.label}</span><b>{c.count}</b></div>
                ))}
              </div>
            </div>
          </section>
        </div>

        <section className="card">
          <div className="card-head">
            <h2>{selectedLabel}
              <span className="chip" style={{ marginLeft: 8, color: 'var(--accent)', background: '#fff7ed' }}>
                {stories?.total ?? 0}건
              </span>
            </h2>
            <select className="btn" value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="score">점수순</option>
              <option value="recent">최신순</option>
            </select>
          </div>
          <div className="card-body">
            {!stories || stories.stories.length === 0
              ? <p className="sub" style={{ margin: 0 }}>이 분야에 적재된 스토리가 없습니다</p>
              : stories.stories.map((s) => <Story key={s.storyId} s={s} />)}
          </div>
        </section>
      </div>
    </div>
  )
}

function Stat({ value, label }) {
  return (
    <div className="card kpi stat">
      <div className="value">{value.toLocaleString()}</div>
      <div className="label">{label}</div>
    </div>
  )
}

/** 목록에 커뮤니티 반응을 미리 노출한다. 이 데이터셋의 가치가 원문 요약이 아니라 커뮤니티 논의에 있다. */
function Story({ s }) {
  return (
    <article className="story">
      <div className="score"><span>▲</span>{s.score}</div>
      <div>
        <div className="title">
          {s.url ? <a href={s.url} target="_blank" rel="noreferrer"
                     style={{ color: 'inherit', textDecoration: 'none' }}>{s.title}</a> : s.title}
        </div>
        <div className="line">
          {s.domain && <span className="domain">{s.domain}</span>}
          {s.category && <span className="chip">{s.category}</span>}
          {s.keywords?.map((k) => <span className="kw" key={k}>#{k}</span>)}
        </div>
        {s.communityReaction && (
          <div className="reaction"><span>💬</span><span>{s.communityReaction}</span></div>
        )}
      </div>
    </article>
  )
}
