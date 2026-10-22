/** 将后端返回的 ISO 时间字符串显示为 YYYY-MM-DD。 */
export function formatDate(dateTime: string | null | undefined): string {
  if (!dateTime) {
    return ''
  }

  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) {
    return dateTime.slice(0, 10)
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
