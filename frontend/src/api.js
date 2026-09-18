/** 백엔드 계약은 api-design.md 에 있다. 여기서는 그대로 받아 쓴다. */

export async function getSetupStatus() {
  const res = await fetch('/api/setup/status')
  if (!res.ok) throw new Error('status ' + res.status)
  return res.json()
}

export async function runSetup(limit) {
  const res = await fetch('/api/setup/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ limit }),
  })
  if (res.status === 409) throw new Error('이미 실행 중입니다')
  if (!res.ok) throw new Error('run ' + res.status)
  return res.json()
}

export async function stopSetup() {
  await fetch('/api/setup/stop', { method: 'POST' })
}

export async function getTopics() {
  const res = await fetch('/api/topics')
  if (!res.ok) throw new Error('topics ' + res.status)
  return res.json()
}

export async function getTopicStories(techField, sort = 'score', limit = 20) {
  const res = await fetch(
    `/api/topics/${encodeURIComponent(techField)}/stories?sort=${sort}&limit=${limit}`)
  if (!res.ok) throw new Error('stories ' + res.status)
  return res.json()
}

/**
 * 진행 상황은 SSE 로 받는다. 폴링 간격에 따른 끊김이 없다.
 * 실행 중이 아닐 때 연결해도 서버가 현재 상태를 한 번 보내고 유지한다.
 */
export function openSetupStream({ onProgress, onDone }) {
  const source = new EventSource('/api/setup/stream')
  source.addEventListener('progress', (e) => onProgress(JSON.parse(e.data)))
  source.addEventListener('done', (e) => onDone(JSON.parse(e.data)))
  return () => source.close()
}

/**
 * 응답이 SSE 인데 EventSource 는 GET 만 지원하므로 fetch + ReadableStream 으로 받는다.
 * 이벤트 순서는 plans → evidence → token 반복 → done 이다.
 */
export async function askChat({ conversationId, question, onEvent, signal }) {
  const res = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ conversationId, question }),
    signal,
  })
  if (!res.ok || !res.body) throw new Error('chat ' + res.status)

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''
    for (const block of blocks) {
      let name = 'message'
      const dataLines = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) name = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (dataLines.length === 0) continue
      try {
        onEvent(name, JSON.parse(dataLines.join('\n')))
      } catch {
        // 데이터가 JSON 이 아니면 건너뛴다
      }
    }
  }
}
