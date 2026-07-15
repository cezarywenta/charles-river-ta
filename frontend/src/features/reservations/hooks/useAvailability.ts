import { useEffect, useRef, useState } from 'react'
import { getAvailability } from '../../../api/client'
import type { ReservationFlowAction } from '../model/reservationFlowReducer'
import type { ReservationCriteria } from '../model/schema'

/**
 * Fetches availability whenever the (already debounced) criteria change.
 * Guards against out-of-order responses two ways: aborting the previous
 * in-flight request via AbortController (the "correct" production
 * behavior), and ignoring any response that isn't from the latest request
 * by id (a safety net independent of whether abort actually cut the
 * response off in time).
 */
export function useAvailability(
  criteria: ReservationCriteria | null,
  dispatch: React.Dispatch<ReservationFlowAction>,
): { retry: () => void } {
  const requestIdRef = useRef(0)
  const abortControllerRef = useRef<AbortController | null>(null)
  const [retryToken, setRetryToken] = useState(0)

  useEffect(() => {
    if (!criteria) {
      abortControllerRef.current?.abort()
      dispatch({ type: 'CRITERIA_CLEARED' })
      return
    }

    abortControllerRef.current?.abort()
    const controller = new AbortController()
    abortControllerRef.current = controller
    const requestId = ++requestIdRef.current

    dispatch({ type: 'CRITERIA_CHANGED' })

    getAvailability(criteria.startAt, criteria.numberOfDays, controller.signal)
      .then((availability) => {
        if (requestIdRef.current !== requestId) {
          return
        }
        dispatch({ type: 'AVAILABILITY_LOADED', criteria, availability })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted || requestIdRef.current !== requestId) {
          return
        }
        const message = error instanceof Error ? error.message : 'Failed to load availability'
        dispatch({ type: 'AVAILABILITY_FAILED', message })
      })

    return () => {
      controller.abort()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [criteria?.startAt, criteria?.numberOfDays, retryToken, dispatch])

  return { retry: () => setRetryToken((n) => n + 1) }
}
