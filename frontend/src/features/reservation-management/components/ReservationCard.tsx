import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { CarType, Reservation } from '../../../api/types'
import { isCancellable } from '../model/filters'

const CAR_TYPE_LABELS: Record<CarType, string> = {
  SEDAN: 'Sedan',
  SUV: 'SUV',
  VAN: 'Van',
}

interface ReservationCardProps {
  reservation: Reservation
  now: Date
  onCancelRequested: (reservation: Reservation) => void
}

export function ReservationCard({ reservation, now, onCancelRequested }: ReservationCardProps) {
  const cancellable = isCancellable(reservation, now)

  return (
    <Card variant="outlined" data-testid={`reservation-card-${reservation.reservationId}`}>
      <CardContent>
        <Stack direction="row" spacing={2} sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Stack spacing={0.5}>
            <Typography variant="subtitle1">{CAR_TYPE_LABELS[reservation.carType]}</Typography>
            <Typography color="text.secondary" variant="body2">
              {reservation.startAt.replace('T', ' ')} &rarr; {reservation.endAt.replace('T', ' ')}
            </Typography>
          </Stack>
          <Stack spacing={1} sx={{ alignItems: 'flex-end' }}>
            <Chip
              size="small"
              label={reservation.status}
              color={reservation.status === 'CANCELLED' ? 'default' : 'success'}
            />
            {cancellable && (
              <Button size="small" color="error" onClick={() => onCancelRequested(reservation)}>
                Cancel
              </Button>
            )}
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  )
}
