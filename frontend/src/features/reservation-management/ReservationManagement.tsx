import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { cancelReservation } from '../../api/client'
import type { Reservation } from '../../api/types'
import { CancelReservationDialog } from './components/CancelReservationDialog'
import { ReservationFilters } from './components/ReservationFilters'
import { ReservationList } from './components/ReservationList'
import { useReservations } from './hooks/useReservations'
import { matchesFilter, type ReservationFilter } from './model/filters'

const EMPTY_MESSAGES: Record<ReservationFilter, string> = {
  upcoming: 'You have no upcoming reservations.',
  past: 'You have no past reservations.',
  cancelled: 'You have no cancelled reservations.',
}

export function ReservationManagement() {
  const { state, reload } = useReservations()
  const [filter, setFilter] = useState<ReservationFilter>('upcoming')
  const [pendingCancellation, setPendingCancellation] = useState<Reservation | null>(null)
  const [isCancelling, setIsCancelling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)

  async function handleConfirmCancel() {
    if (!pendingCancellation) {
      return
    }
    setIsCancelling(true)
    setCancelError(null)
    try {
      const result = await cancelReservation(pendingCancellation.reservationId)
      if (result.kind === 'not-allowed') {
        setCancelError(result.detail)
      } else if (result.kind === 'not-found') {
        setCancelError('This reservation no longer exists.')
      }
      setPendingCancellation(null)
      reload()
    } catch {
      setCancelError('Something went wrong. Please try again.')
    } finally {
      setIsCancelling(false)
    }
  }

  const now = new Date()

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h4" component="h1" gutterBottom>
          My reservations
        </Typography>
        <Typography color="text.secondary">View and manage your car reservations.</Typography>
      </div>

      <ReservationFilters value={filter} onChange={setFilter} />

      {cancelError && (
        <Alert severity="error" onClose={() => setCancelError(null)}>
          {cancelError}
        </Alert>
      )}

      {state.status === 'loading' && (
        <Stack sx={{ alignItems: 'center', py: 4 }} data-testid="reservations-loading">
          <CircularProgress />
        </Stack>
      )}

      {state.status === 'error' && (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={reload}>
              Retry
            </Button>
          }
        >
          {state.message}
        </Alert>
      )}

      {state.status === 'loaded' && (
        <ReservationList
          reservations={state.reservations.filter((reservation) => matchesFilter(reservation, filter, now))}
          now={now}
          emptyMessage={EMPTY_MESSAGES[filter]}
          onCancelRequested={setPendingCancellation}
        />
      )}

      <CancelReservationDialog
        reservation={pendingCancellation}
        isCancelling={isCancelling}
        onConfirm={handleConfirmCancel}
        onClose={() => setPendingCancellation(null)}
      />
    </Stack>
  )
}
