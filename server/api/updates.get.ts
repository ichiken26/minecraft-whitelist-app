import { readFile, stat } from 'node:fs/promises'
import { join } from 'node:path'
const dir = () => process.env.QUEUE_DIR || '/srv/minecraft-container-test/plugins/KokageWhitelist'
export default defineEventHandler(async () => {
  const base = dir()
  let updateAvailable = false
  let update: { status: string; at: string } | null = null
  try { await stat(join(base, 'update-notice')); updateAvailable = true } catch {}
  try { update = JSON.parse(await readFile(join(base, 'update-status.json'), 'utf8')) } catch {}
  return { updateAvailable, update }
})
