import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { CarType } from '../../../api/types'
import { calculateReturnDate, formatReturnDate } from '../utils/dateTime'

const CAR_TYPE_LABELS: Record<CarType, string> = {
  SEDAN: 'Sedan',
  SUV: 'SUV',
  VAN: 'Van',
}

interface ReservationSummaryProps {
  carType: CarType
  startAt: string
  numberOfDays: number
}

export function ReservationSummary({ carType, startAt, numberOfDays }: ReservationSummaryProps) {
  const returnDate = calculateReturnDate(startAt, numberOfDays)

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Typography variant="subtitle1" gutterBottom>
        Reservation summary
      </Typography>
      <Stack spacing={0.5}>
        <Typography>Car type: {CAR_TYPE_LABELS[carType]}</Typography>
        <Typography>Pickup: {startAt.replace('T', ' ')}</Typography>
        {returnDate && <Typography>Return: {formatReturnDate(returnDate)}</Typography>}
      </Stack>
    </Paper>
  )
}
