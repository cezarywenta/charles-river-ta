import type { Reservation } from '../../../api/types'

export type ReservationFilter = 'upcoming' | 'past' | 'cancelled'

export const RESERVATION_FILTERS: readonly ReservationFilter[] = ['upcoming', 'past', 'cancelled']

/** Mirrors the backend's own rule (Reservation.isCancellable): confirmed and
 * not yet started. A reservation that has started but not yet ended still
 * shows under "upcoming" but is not cancellable. */
export function isCancellable(reservation: Reservation, now: Date): boolean {
  return reservation.status === 'CONFIRMED' && new Date(reservation.startAt) > now
}

function hasEnded(reservation: Reservation, now: Date): boolean {
  return new Date(reservation.endAt) <= now
}

export function matchesFilter(reservation: Reservation, filter: ReservationFilter, now: Date): boolean {
  switch (filter) {
    case 'cancelled':
      return reservation.status === 'CANCELLED'
    case 'past':
      return reservation.status === 'CONFIRMED' && hasEnded(reservation, now)
    case 'upcoming':
      return reservation.status === 'CONFIRMED' && !hasEnded(reservation, now)
    default: {
      const exhaustiveCheck: never = filter
      return exhaustiveCheck
    }
  }
}
