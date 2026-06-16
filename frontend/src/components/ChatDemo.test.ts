import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ChatDemo from '@/components/ChatDemo.vue'

vi.mock('@/lib/api', () => ({
  getHealth: vi.fn(async () => ({
    status: 'UP',
    message: 'Spring Boot is running',
    application: 'ai-demo',
  })),
  chat: vi.fn(),
  addDocument: vi.fn(),
  searchKnowledge: vi.fn(),
}))

describe('ChatDemo', () => {
  it('renders title and backend status badge', async () => {
    const wrapper = mount(ChatDemo)
    await wrapper.vm.$nextTick()
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('AI Demo · RAG 问答')
    expect(wrapper.text()).toContain('UP')
  })

  it('disables send button when question is empty', () => {
    const wrapper = mount(ChatDemo)
    const sendButton = wrapper.find('button')
    expect(sendButton.attributes('disabled')).toBeDefined()
  })
})
