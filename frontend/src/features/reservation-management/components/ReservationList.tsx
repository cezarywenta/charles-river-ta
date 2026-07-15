import Stack from '@mui/material/Stack'
import type { Reservation } from '../../../api/types'
import { EmptyState } from './EmptyState'
import { ReservationCard } from './ReservationCard'

interface ReservationListProps {
  reservations: Reservation[]
  now: Date
  emptyMessage: string
  onCancelRequested: (reservation: Reservation) => void
}

export function ReservationList({ reservations, now, emptyMessage, onCancelRequested }: ReservationListProps) {
  if (reservations.length === 0) {
    return <EmptyState message={emptyMessage} />
  }

  return (
    <Stack spacing={2} data-testid="reservation-list">
      {reservations.map((reservation) => (
        <ReservationCard
          key={reservation.reservationId}
          reservation={reservation}
          now={now}
          onCancelRequested={onCancelRequested}
        />
      ))}
    </Stack>
  )
}
