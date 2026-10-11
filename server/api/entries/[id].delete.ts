import { remove } from '../../utils/store'
export default defineEventHandler(async (event) => {
  if (!await remove(getRouterParam(event, 'id') || '')) throw createError({ statusCode: 404, message: 'Not found' })
  return { ok: true }
})
