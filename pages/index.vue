<script setup lang="ts">
type Entry = { id: string; name: string; edition: 'java' | 'bedrock'; op: boolean; createdAt: string; status: string; uuid?: string }
const name = ref('')
const edition = ref<'java'|'bedrock'>('bedrock')
const op = ref(false)
const message = ref('')
const busy = ref(false)
const { data, refresh, pending } = await useFetch<Entry[]>('/api/entries', { default: () => [], server: false })
async function add() {
  busy.value = true; message.value = ''
  try { await $fetch('/api/entries', { method: 'POST', body: { name: name.value.trim(), edition: edition.value, op: op.value } }); name.value = ''; await refresh() }
  catch (e: any) { message.value = e.data?.message || e.message || '登録に失敗しました' }
  finally { busy.value = false }
}
async function remove(id: string) {
  if (!confirm('この登録情報を削除しますか？（ゲーム内のホワイトリストは解除されません）')) return
  try { await $fetch('/api/entries/' + id, { method: 'DELETE' }); await refresh() }
  catch (e: any) { message.value = e.data?.message || '削除できませんでした' }
}
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => { timer = setInterval(() => refresh(), 5000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>
<template>
  <main class="shell">
    <header><div><p class="eyebrow">KOKAGE STUDIO / PRIVATE ADMIN</p><h1>Minecraft Whitelist</h1><p class="muted">Java & Bedrock · ホワイトリストは常時ON</p></div><span class="pill">Tailscale only</span></header>
    <section class="panel"><h2>プレイヤーを承認待ちに追加</h2><form @submit.prevent="add"><label>ユーザー名<input v-model="name" required minlength="3" maxlength="16" pattern="[A-Za-z0-9_]{3,16}" placeholder="ExamplePlayer" autocomplete="off" /></label><div class="row"><label>エディション<select v-model="edition"><option value="java">Java</option><option value="bedrock">Bedrock (Floodgate)</option></select></label><label class="check"><input v-model="op" type="checkbox" /> OP権限を付与</label></div><button :disabled="busy" type="submit">{{ busy ? '登録中…' : '承認キューに追加' }}</button></form><p v-if="message" class="error">{{ message }}</p><p class="muted note">本人が初回ログインすると、認証済みUUIDが許可リストに登録されます。最初の接続で拒否されたら再接続してもらってね。</p></section>
    <section class="panel"><div class="section-heading"><h2>登録キュー</h2><button class="secondary" @click="refresh()">更新</button></div><p v-if="pending" class="muted">読み込み中…</p><p v-if="!data?.length" class="muted">まだ登録はありません。</p><div v-for="entry in data" :key="entry.id" class="entry"><div><strong>{{ entry.name }}</strong><div class="muted detail">{{ entry.edition === 'bedrock' ? 'Bedrock' : 'Java' }} · {{ entry.op ? 'OP付与' : '一般プレイヤー' }}</div><div v-if="entry.uuid" class="muted detail">{{ entry.uuid }}</div></div><div class="actions"><span :class="['status',entry.status]">{{ entry.status === 'completed' ? '登録済み' : '接続待ち' }}</span><button class="secondary" @click="remove(entry.id)">削除</button></div></div></section>
    <footer>既存の minecraft.service / playit.gg には変更を加えません。</footer>
  </main>
</template>
