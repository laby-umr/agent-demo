<script setup lang="ts">
import { computed } from 'vue'
import { BotIcon } from '@lucide/vue'
import { Badge } from '@/components/ui/badge'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  content?: string
  loading?: boolean
  confidence?: string
  refusalReason?: string | null
}>()

const renderedHtml = computed(() => {
  if (props.loading || !props.content?.trim()) {
    return ''
  }
  return renderMarkdown(props.content)
})

const showLoading = computed(() => props.loading === true)

function confidenceVariant(confidence?: string) {
  if (confidence === 'HIGH') return 'secondary'
  if (confidence === 'MEDIUM') return 'outline'
  if (confidence === 'LOW') return 'outline'
  if (confidence === 'REFUSED') return 'destructive'
  return 'outline'
}
</script>

<template>
  <div class="flex w-full items-start gap-3 pb-4">
    <div
      class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"
      aria-hidden="true"
    >
      <BotIcon class="size-4" />
    </div>
    <div class="min-w-0 flex-1 space-y-2">
      <div v-if="confidence || refusalReason" class="flex flex-wrap items-center gap-2">
        <Badge v-if="confidence" :variant="confidenceVariant(confidence)" class="text-xs">
          {{ confidence }}
        </Badge>
        <Badge v-if="refusalReason" variant="destructive" class="text-xs">
          无引用守卫
        </Badge>
      </div>

      <div v-if="showLoading" class="flex items-center gap-2 text-sm text-muted-foreground">
        <span class="inline-block size-1.5 animate-pulse rounded-full bg-primary" />
        <span>{{ content }}</span>
      </div>

      <div
        v-else-if="renderedHtml"
        class="answer-md prose prose-sm max-w-none text-sm leading-relaxed text-foreground"
        v-html="renderedHtml"
      />

      <p v-if="refusalReason" class="text-xs text-destructive">
        {{ refusalReason }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.answer-md :deep(p) {
  margin: 0.35em 0;
}

.answer-md :deep(p:first-child) {
  margin-top: 0;
}

.answer-md :deep(p:last-child) {
  margin-bottom: 0;
}

.answer-md :deep(ul),
.answer-md :deep(ol) {
  margin: 0.35em 0;
  padding-left: 1.25rem;
}

.answer-md :deep(code) {
  border-radius: 0.25rem;
  background: color-mix(in oklab, var(--muted) 80%, transparent);
  padding: 0.1rem 0.35rem;
  font-size: 0.85em;
}

.answer-md :deep(pre) {
  margin: 0.5em 0;
  overflow-x: auto;
  border-radius: 0.5rem;
  border: 1px solid var(--border);
  background: var(--muted);
  padding: 0.75rem;
}

.answer-md :deep(pre code) {
  background: transparent;
  padding: 0;
}

.answer-md :deep(blockquote) {
  margin: 0.5em 0;
  border-left: 3px solid var(--border);
  padding-left: 0.75rem;
  color: var(--muted-foreground);
}

.answer-md :deep(table) {
  display: block;
  overflow-x: auto;
  border-collapse: collapse;
  margin: 0.5em 0;
  font-size: 0.85em;
}

.answer-md :deep(th),
.answer-md :deep(td) {
  border: 1px solid var(--border);
  padding: 0.35rem 0.5rem;
}
</style>
