import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import type { ReservationFlowState } from '../model/reservationFlowReducer'

interface ReservationFeedbackProps {
  state: ReservationFlowState
  onRetry: () => void
  onReserveAnother: () => void
}

export function ReservationFeedback({ state, onRetry, onReserveAnother }: ReservationFeedbackProps) {
  switch (state.status) {
    case 'confirmed':
      return (
        <Alert
          severity="success"
          action={
            <Button color="inherit" size="small" onClick={onReserveAnother}>
              Reserve another
            </Button>
          }
        >
          Reservation confirmed.
        </Alert>
      )

    case 'unavailable':
      return (
        <Alert severity="warning">
          {state.message} Availability has been refreshed &ndash; pick another type or date.
        </Alert>
      )

    case 'failed':
      return (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={onRetry}>
              Retry
            </Button>
          }
        >
          {state.message}
        </Alert>
      )

    default:
      return null
  }
}
