import { randomUUID } from 'node:crypto'
import { mkdir, readdir, readFile, writeFile, unlink } from 'node:fs/promises'
import { join } from 'node:path'

export type Entry = { id: string; name: string; edition: 'java' | 'bedrock'; op: boolean; createdAt: string; uuid?: string; status?: string }
const root = () => process.env.QUEUE_DIR || '/opt/minecraft/plugins/KokageWhitelist'
const dirs = () => ({ pending: join(root(), 'pending'), completed: join(root(), 'completed') })
export async function entries() {
  const d = dirs()
  const result: Entry[] = []
  for (const [status, folder] of Object.entries(d)) {
    await mkdir(folder, { recursive: true })
    for (const file of await readdir(folder)) {
      if (!/^[0-9a-f-]{36}\.json$/.test(file)) continue
      try { result.push({ ...JSON.parse(await readFile(join(folder, file), 'utf8')), status }) } catch { /* ignore corrupt entry; don't delete */ }
    }
  }
  return result.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}
export async function enqueue(input: { name: string; edition: 'java' | 'bedrock'; op: boolean }) {
  const id = randomUUID()
  const entry: Entry = { ...input, id, createdAt: new Date().toISOString() }
  const d = dirs(); await mkdir(d.pending, { recursive: true })
  await writeFile(join(d.pending, id + '.json'), JSON.stringify(entry, null, 2), { flag: 'wx', mode: 0o640 })
  return entry
}
export async function remove(id: string) {
  if (!/^[0-9a-f-]{36}$/.test(id)) return false
  const d = dirs()
  for (const path of [d.pending, d.completed]) {
    try { await unlink(join(path, id + '.json')); return true } catch (err: any) { if (err.code !== 'ENOENT') throw err }
  }
  return false
}
