<script setup lang="ts">
type Entry = { id: string; name: string; edition: 'java' | 'bedrock'; op: boolean; createdAt: string; status: string; uuid?: string }
const name = ref('')
const edition = ref<'java'|'bedrock'>('bedrock')
const op = ref(false)
const message = ref('')
const busy = ref(false)
const updating = ref(false)
const { data: updateInfo, refresh: refreshUpdate } = await useFetch('/api/updates', { server: false })
async function requestUpdate() {
  if (!confirm('プレイヤーが退出していることを確認しましたか？ Minecraftサーバーが再起動します。')) return
  updating.value = true
  try { await $fetch('/api/updates', { method: 'POST' }); alert('更新を予約したよ。管理画面で状態を確認してね。'); await refreshUpdate() }
  catch (e: any) { alert(e.data?.message || e.message || '更新依頼に失敗') }
  finally { updating.value = false }
}
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
onMounted(() => { timer = setInterval(() => { refresh(); refreshUpdate() }, 5000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>
<template>
  <main class="shell">
    <header><div><p class="eyebrow">KOKAGE STUDIO / PRIVATE ADMIN</p><h1>Minecraft Whitelist</h1><p class="muted">Java & Bedrock · ホワイトリストは常時ON</p></div><span class="pill">Tailscale only</span></header>
    <section class="panel"><h2>Geyser / Floodgate 更新</h2><p v-if="updateInfo?.updateAvailable" class="error">Geyserの更新通知を検出しました。管理者による統合版アップデートが必要です。</p><p v-else class="muted">更新通知なし（または検出待ち）</p><p class="muted">更新状態: {{ updateInfo?.update?.status || "未実行" }} · {{ updateInfo?.update?.at || "" }}</p><button :disabled="updating" @click="requestUpdate">{{ updating ? "依頼中…" : "Geyser / Floodgate を手動更新" }}</button><p class="muted note">更新時はコンテナが一時停止し再起動します。ワールドの既存バックアップも必ず確認してください。</p></section><section class="panel"><h2>プレイヤーを承認待ちに追加</h2><form @submit.prevent="add"><label>ユーザー名<input v-model="name" required minlength="3" maxlength="16" pattern="[A-Za-z0-9_]{3,16}" placeholder="ExamplePlayer" autocomplete="off" /></label><div class="row"><label>エディション<select v-model="edition"><option value="java">Java</option><option value="bedrock">Bedrock (Floodgate)</option></select></label><label class="check"><input v-model="op" type="checkbox" /> OP権限を付与</label></div><button :disabled="busy" type="submit">{{ busy ? '登録中…' : '承認キューに追加' }}</button></form><p v-if="message" class="error">{{ message }}</p><p class="muted note">本人が初回ログインすると、認証済みUUIDが許可リストに登録されます。最初の接続で拒否されたら再接続してもらってね。</p></section>
    <section class="panel"><div class="section-heading"><h2>登録キュー</h2><button class="secondary" @click="refresh()">更新</button></div><p v-if="pending" class="muted">読み込み中…</p><p v-if="!data?.length" class="muted">まだ登録はありません。</p><div v-for="entry in data" :key="entry.id" class="entry"><div><strong>{{ entry.name }}</strong><div class="muted detail">{{ entry.edition === 'bedrock' ? 'Bedrock' : 'Java' }} · {{ entry.op ? 'OP付与' : '一般プレイヤー' }}</div><div v-if="entry.uuid" class="muted detail">{{ entry.uuid }}</div></div><div class="actions"><span :class="['status',entry.status]">{{ entry.status === 'completed' ? '登録済み' : '接続待ち' }}</span><button class="secondary" @click="remove(entry.id)">削除</button></div></div></section>
    <footer>mc-container-test.service 専用 · Tailscale内のみ</footer>
  </main>
</template>
