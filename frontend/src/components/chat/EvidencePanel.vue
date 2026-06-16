<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronDownIcon, FileTextIcon } from '@lucide/vue'
import { Badge } from '@/components/ui/badge'
import type { DocumentEvidence } from '@/lib/api'

const props = defineProps<{
  evidences?: DocumentEvidence[]
}>()

const expanded = ref(true)
const activeDoc = ref<string | null>(null)

const grouped = computed(() => {
  const map = new Map<string, DocumentEvidence[]>()
  for (const item of props.evidences ?? []) {
    const key = item.docId || 'document'
    if (!map.has(key)) {
      map.set(key, [])
    }
    map.get(key)!.push(item)
  }
  return [...map.entries()].map(([docId, items]) => ({ docId, items }))
})

function toggleDoc(docId: string) {
  activeDoc.value = activeDoc.value === docId ? null : docId
}
</script>

<template>
  <div v-if="evidences?.length" class="ml-11 mt-1 rounded-lg border bg-muted/40 p-2.5">
    <button
      type="button"
      class="mb-2 flex w-full items-center gap-1.5 text-left text-xs font-medium text-muted-foreground hover:text-foreground"
      @click="expanded = !expanded"
    >
      <FileTextIcon class="size-3.5" />
      知识引用
      <Badge variant="secondary" class="ml-1 h-5 px-1.5 text-[10px]">
        {{ evidences.length }}
      </Badge>
      <ChevronDownIcon
        class="ml-auto size-3.5 transition-transform"
        :class="expanded ? 'rotate-180' : ''"
      />
    </button>

    <div v-show="expanded" class="space-y-2">
      <div v-for="group in grouped" :key="group.docId" class="rounded-md border bg-card">
        <button
          type="button"
          class="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-accent/40"
          @click="toggleDoc(group.docId)"
        >
          <span class="truncate font-medium text-foreground">{{ group.docId }}</span>
          <span class="shrink-0 text-xs text-muted-foreground">{{ group.items.length }} 条</span>
        </button>

        <div v-if="activeDoc === group.docId" class="border-t px-3 py-2">
          <div
            v-for="(evidence, index) in group.items"
            :key="index"
            class="border-b border-border/60 py-2 last:border-b-0 last:pb-0"
          >
            <div class="mb-1 flex flex-wrap gap-1.5">
              <Badge v-if="evidence.page > 0" variant="outline" class="text-[10px]">
                p.{{ evidence.page }}
              </Badge>
              <Badge variant="secondary" class="text-[10px]">
                score {{ evidence.score.toFixed(3) }}
              </Badge>
              <Badge v-if="evidence.blockType" variant="outline" class="text-[10px]">
                {{ evidence.blockType }}
              </Badge>
            </div>
            <p class="whitespace-pre-wrap text-xs leading-relaxed text-muted-foreground">
              {{ evidence.snippet }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
