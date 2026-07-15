import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import type { Reservation } from '../../../api/types'

interface CancelReservationDialogProps {
  reservation: Reservation | null
  isCancelling: boolean
  onConfirm: () => void
  onClose: () => void
}

export function CancelReservationDialog({
  reservation,
  isCancelling,
  onConfirm,
  onClose,
}: CancelReservationDialogProps) {
  return (
    <Dialog open={reservation !== null} onClose={onClose}>
      <DialogTitle>Cancel reservation?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {reservation &&
            `This will cancel your ${reservation.carType} reservation starting ${reservation.startAt.replace('T', ' ')}. This cannot be undone.`}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isCancelling}>
          Keep reservation
        </Button>
        <Button onClick={onConfirm} color="error" disabled={isCancelling}>
          {isCancelling ? 'Cancelling…' : 'Cancel reservation'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
