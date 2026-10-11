import { enqueue, entries } from '../utils/store'
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  if (!body || typeof body.name !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(body.name) || !['java','bedrock'].includes(body.edition) || typeof body.op !== 'boolean') throw createError({ statusCode: 400, message: 'Invalid name, edition or OP setting' })
  const all = await entries()
  if (all.some(item => item.edition === body.edition && item.name.toLowerCase() === body.name.toLowerCase())) throw createError({ statusCode: 409, message: 'Player already listed' })
  return enqueue({ name: body.name, edition: body.edition, op: body.op })
})
