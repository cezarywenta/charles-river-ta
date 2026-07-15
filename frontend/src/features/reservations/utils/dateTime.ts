/** Converts a `datetime-local` input value ("2027-01-10T10:00") to the
 * LocalDateTime format the backend expects ("2027-01-10T10:00:00"). */
export function toApiDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

/** The inverse direction: formats a Date as a `datetime-local` input value
 * ("2027-01-10T10:00"), used for the input's `min` attribute. Built from
 * local getters (not toISOString, which is UTC-based) so the string means
 * the same instant regardless of the browser's time zone. */
export function toDateTimeLocalValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function calculateReturnDate(startAt: string, numberOfDays: number): Date | null {
  if (!startAt || !Number.isFinite(numberOfDays) || numberOfDays <= 0) {
    return null
  }
  const start = new Date(startAt)
  if (Number.isNaN(start.getTime())) {
    return null
  }
  const end = new Date(start)
  end.setDate(end.getDate() + numberOfDays)
  return end
}

// Explicit locale, not the runtime's default: formatting must be
// deterministic in tests regardless of the machine's system locale.
const RETURN_DATE_FORMATTER = new Intl.DateTimeFormat('en-US', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export function formatReturnDate(date: Date): string {
  return RETURN_DATE_FORMATTER.format(date)
}
