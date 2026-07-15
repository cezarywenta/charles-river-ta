import Typography from '@mui/material/Typography'
import { calculateReturnDate, formatReturnDate } from '../utils/dateTime'

interface CalculatedReturnDateProps {
  startAt: string
  numberOfDays: number
}

export function CalculatedReturnDate({ startAt, numberOfDays }: CalculatedReturnDateProps) {
  const returnDate = calculateReturnDate(startAt, numberOfDays)

  if (!returnDate) {
    return null
  }

  return (
    <Typography color="text.secondary" data-testid="calculated-return-date">
      Return date: {formatReturnDate(returnDate)}
    </Typography>
  )
}
