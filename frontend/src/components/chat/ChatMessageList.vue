<script setup lang="ts">
import UserMessage from '@/components/chat/UserMessage.vue'
import AssistantMessage from '@/components/chat/AssistantMessage.vue'
import EvidencePanel from '@/components/chat/EvidencePanel.vue'
import type { DocumentEvidence } from '@/lib/api'

export interface ChatMessageItem {
  id: number
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  confidence?: string
  evidences?: DocumentEvidence[]
  refusalReason?: string | null
}

defineProps<{
  messages: ChatMessageItem[]
  emptyHint?: string
}>()
</script>

<template>
  <div v-if="messages.length === 0" class="flex h-full min-h-[320px] items-center justify-center px-4">
    <p class="text-center text-sm text-muted-foreground">
      {{ emptyHint ?? '输入问题开始对话' }}
    </p>
  </div>
  <div v-else class="flex flex-col py-2">
    <template v-for="message in messages" :key="message.id">
      <UserMessage v-if="message.role === 'user'" :content="message.content" />
      <template v-else>
        <AssistantMessage
          :content="message.content"
          :loading="message.loading"
          :confidence="message.confidence"
          :refusal-reason="message.refusalReason"
        />
        <EvidencePanel v-if="message.evidences?.length" :evidences="message.evidences" />
      </template>
    </template>
  </div>
</template>
