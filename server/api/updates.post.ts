import { writeFile, mkdir } from 'node:fs/promises'
import { join } from 'node:path'
import { createError } from 'h3'
export default defineEventHandler(async () => {
  const base = process.env.QUEUE_DIR || '/srv/minecraft-container-test/plugins/KokageWhitelist'
  await mkdir(base, { recursive: true })
  try {
    await writeFile(join(base, 'update-request'), new Date().toISOString(), { flag: 'wx', mode: 0o640 })
  } catch (e: any) {
    if (e.code === 'EEXIST') throw createError({ statusCode: 409, message: '更新リクエストは処理待ちです' })
    throw e
  }
  return { queued: true }
})
