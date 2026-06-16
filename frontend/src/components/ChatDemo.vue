<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BotIcon, DatabaseIcon, SearchIcon, SendIcon } from '@lucide/vue'
import ChatMessageList, { type ChatMessageItem } from '@/components/chat/ChatMessageList.vue'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Spinner } from '@/components/ui/spinner'
import { Textarea } from '@/components/ui/textarea'
import {
  addDocument,
  askDocument,
  chat,
  getDocumentStatus,
  getHealth,
  searchDocuments,
  searchKnowledge,
  type DocumentEvidence,
  type DocumentIngestStatus,
  type SearchHit,
} from '@/lib/api'

const backendStatus = ref<'loading' | 'up' | 'down'>('loading')
const backendMessage = ref('')
const question = ref('')
const knowledgeTitle = ref('Demo 文档')
const knowledgeContent = ref('AgentScope 是一个 Java 智能体框架，支持 RAG 知识库问答。')
const searchQuery = ref('AgentScope')
const searchHits = ref<SearchHit[]>([])
const messages = ref<ChatMessageItem[]>([])
const sessionId = ref<string>()
const isChatting = ref(false)
const isAddingKnowledge = ref(false)
const isSearching = ref(false)
const errorMessage = ref('')

const docQuestion = ref('')
const docMessages = ref<ChatMessageItem[]>([])
const docSessionId = ref<string>()
const docSearchQuery = ref('键 技术条件')
const docSearchHits = ref<DocumentEvidence[]>([])
const ingestStatus = ref<DocumentIngestStatus>()
const selectedPdf = ref<File | null>(null)
const isDocAsking = ref(false)
const isDocSearching = ref(false)
const isIngesting = ref(false)

const canSend = computed(() => question.value.trim().length > 0 && !isChatting.value)
const canAskDoc = computed(
  () =>
    docQuestion.value.trim().length > 0 &&
    !isDocAsking.value &&
    ingestStatus.value?.state === 'COMPLETED',
)

async function loadHealth() {
  backendStatus.value = 'loading'
  try {
    const health = await getHealth()
    backendStatus.value = 'up'
    backendMessage.value = `${health.application} · ${health.message}`
  } catch {
    backendStatus.value = 'down'
    backendMessage.value = '后端未启动，请先运行 Spring Boot（8050）'
  }
}

function removeLoadingMessage(list: ChatMessageItem[], loadingId: number) {
  return list.filter((item) => item.id !== loadingId)
}

async function handleChat() {
  if (!canSend.value) {
    return
  }

  const content = question.value.trim()
  question.value = ''
  errorMessage.value = ''
  messages.value.push({ id: Date.now(), role: 'user', content })
  isChatting.value = true
  const loadingId = Date.now() + 0.5
  messages.value.push({
    id: loadingId,
    role: 'assistant',
    content: '正在检索知识库并生成回答…',
    loading: true,
  })

  try {
    const response = await chat({ question: content, sessionId: sessionId.value })
    sessionId.value = response.sessionId ?? sessionId.value
    messages.value = removeLoadingMessage(messages.value, loadingId)
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer,
    })
  } catch (error) {
    messages.value = removeLoadingMessage(messages.value, loadingId)
    errorMessage.value = error instanceof Error ? error.message : '问答请求失败'
  } finally {
    isChatting.value = false
  }
}

async function handleAddKnowledge() {
  isAddingKnowledge.value = true
  errorMessage.value = ''

  try {
    await addDocument({
      title: knowledgeTitle.value,
      content: knowledgeContent.value,
    })
    errorMessage.value = ''
    alert('知识已写入 Milvus 向量库')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '知识入库失败'
  } finally {
    isAddingKnowledge.value = false
  }
}

async function loadDocumentStatus() {
  try {
    ingestStatus.value = await getDocumentStatus()
  } catch {
    ingestStatus.value = undefined
  }
}

async function handleDocAsk() {
  if (!canAskDoc.value) {
    return
  }

  const content = docQuestion.value.trim()
  docQuestion.value = ''
  errorMessage.value = ''
  docMessages.value.push({ id: Date.now(), role: 'user', content })
  isDocAsking.value = true
  const loadingId = Date.now() + 0.5
  docMessages.value.push({
    id: loadingId,
    role: 'assistant',
    content: '正在检索 PDF 证据并生成回答…',
    loading: true,
  })

  try {
    const response = await askDocument({ question: content, sessionId: docSessionId.value })
    docSessionId.value = response.sessionId ?? docSessionId.value
    docMessages.value = removeLoadingMessage(docMessages.value, loadingId)
    docMessages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer,
      confidence: response.confidence,
      evidences: response.evidences,
      refusalReason: response.refusalReason,
    })
  } catch (error) {
    docMessages.value = removeLoadingMessage(docMessages.value, loadingId)
    errorMessage.value = error instanceof Error ? error.message : '文档问答失败'
  } finally {
    isDocAsking.value = false
  }
}

async function handleDocSearch() {
  isDocSearching.value = true
  errorMessage.value = ''
  try {
    const response = await searchDocuments(docSearchQuery.value)
    docSearchHits.value = response.hits
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档检索失败'
  } finally {
    isDocSearching.value = false
  }
}

function onPdfSelected(event: Event) {
  const input = event.target as HTMLInputElement
  selectedPdf.value = input.files?.[0] ?? null
}

async function pollIngestStatus(maxAttempts = 120) {
  for (let i = 0; i < maxAttempts; i++) {
    ingestStatus.value = await getDocumentStatus()
    if (ingestStatus.value.state === 'COMPLETED' || ingestStatus.value.state === 'FAILED') {
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 2000))
  }
}

async function handleIngestPdf() {
  if (!selectedPdf.value) {
    errorMessage.value = '请先选择 PDF 文件'
    return
  }
  isIngesting.value = true
  errorMessage.value = ''
  try {
    const formData = new FormData()
    formData.append('file', selectedPdf.value)
    const response = await fetch('/api/documents/ingest?async=true', {
      method: 'POST',
      body: formData,
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text)
    }
    ingestStatus.value = await response.json()
    await pollIngestStatus()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'PDF 入库失败'
  } finally {
    isIngesting.value = false
  }
}

async function handleSearch() {
  isSearching.value = true
  errorMessage.value = ''

  try {
    const response = await searchKnowledge(searchQuery.value)
    searchHits.value = response.hits
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '检索失败'
  } finally {
    isSearching.value = false
  }
}

onMounted(() => {
  void loadHealth()
  void loadDocumentStatus()
})
</script>

<template>
  <div class="min-h-svh bg-background text-foreground">
    <div class="mx-auto flex w-full max-w-6xl flex-col gap-6 p-6">
      <div class="flex flex-col gap-2">
        <div class="flex items-center gap-2">
          <BotIcon class="size-6" />
          <h1 class="text-2xl font-semibold tracking-tight">AI Demo · RAG 问答</h1>
        </div>
        <p class="text-sm text-muted-foreground">
          参考 laby-admin 对话布局 · DeepSeek 生成 · 智谱 Embedding · Milvus 检索
        </p>
      </div>

      <Alert>
        <DatabaseIcon class="size-4" />
        <AlertTitle>后端状态</AlertTitle>
        <AlertDescription class="flex items-center gap-2">
          <Skeleton v-if="backendStatus === 'loading'" class="h-4 w-40" />
          <template v-else>
            <Badge :variant="backendStatus === 'up' ? 'secondary' : 'destructive'">
              {{ backendStatus === 'up' ? 'UP' : 'DOWN' }}
            </Badge>
            <span>{{ backendMessage }}</span>
          </template>
        </AlertDescription>
      </Alert>

      <Alert v-if="errorMessage" variant="destructive">
        <AlertTitle>请求失败</AlertTitle>
        <AlertDescription>{{ errorMessage }}</AlertDescription>
      </Alert>

      <div class="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <Card class="overflow-hidden">
          <CardHeader class="border-b bg-muted/20 pb-4">
            <CardTitle>智能问答</CardTitle>
            <CardDescription>先「添加到知识库」，再提问（RAG 已开启）</CardDescription>
          </CardHeader>
          <CardContent class="flex flex-col gap-4 p-0">
            <ScrollArea class="h-[min(520px,60vh)] px-4">
              <ChatMessageList
                :messages="messages"
                empty-hint="输入问题开始对话，例如：AgentScope 是什么？"
              />
            </ScrollArea>
            <div class="flex flex-col gap-3 border-t bg-muted/10 p-4">
              <Textarea
                v-model="question"
                placeholder="输入你的问题..."
                rows="3"
                class="resize-none bg-background"
                @keydown.enter.exact.prevent="handleChat"
              />
              <div class="flex justify-end">
                <Button :disabled="!canSend" @click="handleChat">
                  <Spinner v-if="isChatting" data-icon="inline-start" />
                  <SendIcon v-else data-icon="inline-start" />
                  发送
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <div class="flex flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle>知识入库</CardTitle>
              <CardDescription>上传文本到向量知识库</CardDescription>
            </CardHeader>
            <CardContent class="flex flex-col gap-3">
              <Input v-model="knowledgeTitle" placeholder="文档标题" />
              <Textarea v-model="knowledgeContent" rows="5" placeholder="文档内容" />
            </CardContent>
            <CardFooter>
              <Button
                class="w-full"
                variant="secondary"
                :disabled="isAddingKnowledge"
                @click="handleAddKnowledge"
              >
                <Spinner v-if="isAddingKnowledge" data-icon="inline-start" />
                添加到知识库
              </Button>
            </CardFooter>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>知识检索</CardTitle>
              <CardDescription>调试向量检索结果</CardDescription>
            </CardHeader>
            <CardContent class="flex flex-col gap-3">
              <Input v-model="searchQuery" placeholder="检索关键词" />
              <Button variant="outline" :disabled="isSearching" @click="handleSearch">
                <Spinner v-if="isSearching" data-icon="inline-start" />
                <SearchIcon v-else data-icon="inline-start" />
                检索
              </Button>
              <Separator />
              <div v-if="searchHits.length === 0" class="text-sm text-muted-foreground">
                暂无检索结果
              </div>
              <div v-else class="flex max-h-64 flex-col gap-2 overflow-y-auto">
                <div
                  v-for="(hit, index) in searchHits"
                  :key="index"
                  class="rounded-lg border bg-muted/30 p-3 text-sm"
                >
                  <Badge variant="secondary" class="mb-2 text-[10px]">
                    score {{ hit.score.toFixed(3) }}
                  </Badge>
                  <p class="whitespace-pre-wrap leading-relaxed text-muted-foreground">
                    {{ hit.content }}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Separator />

      <div class="flex flex-col gap-2">
        <h2 class="text-xl font-semibold">智能文档问答 Agent</h2>
        <p class="text-sm text-muted-foreground">
          PDF 结构化入库 → Universal RAG 检索 → 带页码引用与拒答自检
        </p>
      </div>

      <div class="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <Card class="overflow-hidden">
          <CardHeader class="border-b bg-muted/20 pb-4">
            <CardTitle>文档问答</CardTitle>
            <CardDescription>基于 PDF 证据的 grounded 回答，展示置信度与引用</CardDescription>
          </CardHeader>
          <CardContent class="flex flex-col gap-4 p-0">
            <ScrollArea class="h-[min(520px,60vh)] px-4">
              <ChatMessageList
                :messages="docMessages"
                empty-hint="请先上传并入库 PDF，例如：本标准规定了什么内容？"
              />
            </ScrollArea>
            <div class="flex flex-col gap-3 border-t bg-muted/10 p-4">
              <div v-if="ingestStatus?.state !== 'COMPLETED'" class="text-xs text-amber-600">
                请先上传 PDF 并等待入库完成（状态 COMPLETED）后再提问
              </div>
              <Textarea
                v-model="docQuestion"
                placeholder="输入关于 PDF 的问题，例如：本标准规定了什么内容？"
                rows="3"
                class="resize-none bg-background"
                @keydown.enter.exact.prevent="handleDocAsk"
              />
              <div class="flex justify-end">
                <Button :disabled="!canAskDoc" @click="handleDocAsk">
                  <Spinner v-if="isDocAsking" data-icon="inline-start" />
                  <SendIcon v-else data-icon="inline-start" />
                  文档问答
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <div class="flex flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle>PDF 入库</CardTitle>
              <CardDescription>上传 PDF，结构化分片后写入 Milvus</CardDescription>
            </CardHeader>
            <CardContent class="flex flex-col gap-3">
              <Input type="file" accept="application/pdf,.pdf" @change="onPdfSelected" />
              <div v-if="ingestStatus" class="rounded-md border bg-muted/30 p-3 text-sm text-muted-foreground">
                状态：{{ ingestStatus.state }}
                <span v-if="ingestStatus.fileName"> · {{ ingestStatus.fileName }}</span>
                <span v-if="ingestStatus.pdfType"> · {{ ingestStatus.pdfType }}</span>
                <span v-if="ingestStatus.chunkCount"> · {{ ingestStatus.chunkCount }} chunks</span>
              </div>
            </CardContent>
            <CardFooter>
              <Button
                class="w-full"
                variant="secondary"
                :disabled="isIngesting || !selectedPdf"
                @click="handleIngestPdf"
              >
                <Spinner v-if="isIngesting" data-icon="inline-start" />
                解析并入库
              </Button>
            </CardFooter>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>文档检索</CardTitle>
              <CardDescription>调试 PDF 向量检索与页码元数据</CardDescription>
            </CardHeader>
            <CardContent class="flex flex-col gap-3">
              <Input v-model="docSearchQuery" placeholder="检索关键词" />
              <Button variant="outline" :disabled="isDocSearching" @click="handleDocSearch">
                <Spinner v-if="isDocSearching" data-icon="inline-start" />
                <SearchIcon v-else data-icon="inline-start" />
                检索
              </Button>
              <Separator />
              <div v-if="docSearchHits.length === 0" class="text-sm text-muted-foreground">
                暂无检索结果
              </div>
              <div v-else class="flex max-h-64 flex-col gap-2 overflow-y-auto">
                <div
                  v-for="(hit, index) in docSearchHits"
                  :key="index"
                  class="rounded-lg border bg-muted/30 p-3 text-sm"
                >
                  <Badge variant="secondary" class="mb-2 text-[10px]">
                    p.{{ hit.page }} · {{ hit.score.toFixed(3) }}
                  </Badge>
                  <p class="whitespace-pre-wrap leading-relaxed">{{ hit.snippet }}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
