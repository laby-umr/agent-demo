import { describe, expect, it, vi, beforeEach } from 'vitest'
import { getHealth, chat } from '@/lib/api'

describe('api', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('getHealth returns backend status', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        status: 'UP',
        message: 'Spring Boot is running',
        application: 'ai-demo',
      }),
    } as Response)

    const result = await getHealth()
    expect(result.status).toBe('UP')
    expect(fetch).toHaveBeenCalledWith('/api/health', expect.any(Object))
  })

  it('chat posts question payload', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        answer: 'AgentScope 是一个 Java 智能体框架。',
        sessionId: 'demo-session',
      }),
    } as Response)

    const result = await chat({ question: 'AgentScope 是什么？' })
    expect(result.answer).toContain('AgentScope')
    expect(fetch).toHaveBeenCalledWith(
      '/api/chat',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ question: 'AgentScope 是什么？' }),
      }),
    )
  })

  it('throws when response is not ok', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 500,
      text: async () => 'Internal Server Error',
    } as Response)

    await expect(getHealth()).rejects.toThrow('Internal Server Error')
  })

  it('askDocument posts question payload', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        answer: '本标准规定了键的技术要求 [p.1]',
        confidence: 'HIGH',
        evidences: [{ page: 1, snippet: '键 技术条件', score: 0.8, docId: 'gbt1568' }],
        sessionId: 'doc-session',
      }),
    } as Response)

    const { askDocument } = await import('@/lib/api')
    const result = await askDocument({ question: '标准规定了什么？' })
    expect(result.confidence).toBe('HIGH')
    expect(result.evidences[0].page).toBe(1)
  })
})
