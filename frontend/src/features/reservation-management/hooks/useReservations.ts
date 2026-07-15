import { useCallback, useEffect, useState } from 'react'
import { ApiError, listReservations } from '../../../api/client'
import type { Reservation } from '../../../api/types'

export type ReservationsState =
  | { status: 'loading' }
  | { status: 'loaded'; reservations: Reservation[] }
  | { status: 'error'; message: string }

export function useReservations(): { state: ReservationsState; reload: () => void } {
  const [state, setState] = useState<ReservationsState>({ status: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  const load = useCallback(async (signal?: AbortSignal) => {
    setState({ status: 'loading' })
    try {
      const reservations = await listReservations(signal)
      setState({ status: 'loaded', reservations })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      const message = error instanceof ApiError ? error.message : 'Failed to load reservations.'
      setState({ status: 'error', message })
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load, reloadToken])

  return { state, reload: () => setReloadToken((n) => n + 1) }
}
