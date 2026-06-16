export interface HealthResponse {
  status: string
  message: string
  application: string
}

export interface ChatRequest {
  question: string
  sessionId?: string
}

export interface ChatResponse {
  answer: string
  sessionId?: string
}

export interface AddDocumentRequest {
  title?: string
  content: string
}

export interface SearchHit {
  content: string
  score: number
}

export interface SearchResponse {
  query: string
  hits: SearchHit[]
}

const API_BASE = '/api'
const DEFAULT_TIMEOUT_MS = 10 * 60 * 1000

function parseErrorMessage(text: string, status: number): string {
  try {
    const json = JSON.parse(text) as { error?: string; message?: string }
    return json.error ?? json.message ?? text
  } catch {
    return text || `Request failed: ${status}`
  }
}

async function request<T>(path: string, init?: RequestInit, timeoutMs = DEFAULT_TIMEOUT_MS): Promise<T> {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), timeoutMs)

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
      signal: controller.signal,
      ...init,
    })

    if (!response.ok) {
      const text = await response.text()
      throw new Error(parseErrorMessage(text, response.status))
    }

    return response.json() as Promise<T>
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('请求超时：本地模型推理较慢，请稍后再试')
    }
    throw error
  } finally {
    window.clearTimeout(timer)
  }
}

export function getHealth() {
  return request<HealthResponse>('/health', undefined, 15000)
}

export function chat(payload: ChatRequest) {
  return request<ChatResponse>('/chat', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function addDocument(payload: AddDocumentRequest) {
  return request<{ status: string; message: string }>('/knowledge/documents', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function searchKnowledge(query: string) {
  return request<SearchResponse>(`/knowledge/search?q=${encodeURIComponent(query)}`)
}

export interface DocumentEvidence {
  segmentId?: number
  page: number
  snippet: string
  contextText?: string | null
  score: number
  docId: string
  blockType?: string | null
}

export interface DocumentQaResponse {
  answer: string
  confidence: string
  refusalReason?: string | null
  evidences: DocumentEvidence[]
  sessionId?: string
}

export interface DocumentIngestStatus {
  state: string
  fileName?: string | null
  pdfType?: string | null
  blockCount?: number | null
  chunkCount?: number | null
  error?: string | null
}

export function getDocumentStatus() {
  return request<DocumentIngestStatus>('/documents/status', undefined, 15000)
}

export function askDocument(payload: ChatRequest) {
  return request<DocumentQaResponse>('/documents/ask', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function searchDocuments(query: string) {
  return request<{ query: string; hits: DocumentEvidence[] }>(
    `/documents/search?q=${encodeURIComponent(query)}`,
  )
}

export async function ingestDocument(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS)
  try {
    const response = await fetch(`${API_BASE}/documents/ingest`, {
      method: 'POST',
      body: formData,
      signal: controller.signal,
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(parseErrorMessage(text, response.status))
    }
    return response.json() as Promise<DocumentIngestStatus>
  } finally {
    window.clearTimeout(timer)
  }
}
