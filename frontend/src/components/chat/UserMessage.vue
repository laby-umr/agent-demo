<script setup lang="ts">
import { UserIcon } from '@lucide/vue'

defineProps<{
  content?: string
}>()
</script>

<template>
  <div class="flex w-full justify-end pb-3">
    <div class="flex max-w-[min(88%,44rem)] items-start gap-2.5">
      <div
        v-if="content?.trim()"
        class="user-bubble min-w-0 flex-1 rounded-[14px_14px_4px_14px] border border-primary/15 bg-primary/8 px-3.5 py-2.5 text-sm leading-relaxed text-foreground shadow-sm"
      >
        <p class="whitespace-pre-wrap break-words">{{ content }}</p>
      </div>
      <div
        class="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"
        aria-hidden="true"
      >
        <UserIcon class="size-4" />
      </div>
    </div>
  </div>
</template>
