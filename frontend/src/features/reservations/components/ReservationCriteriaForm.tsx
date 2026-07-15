import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import type { FieldErrors, UseFormRegister } from 'react-hook-form'
import type { ReservationFormValues } from '../model/schema'
import { toDateTimeLocalValue } from '../utils/dateTime'

interface ReservationCriteriaFormProps {
  register: UseFormRegister<ReservationFormValues>
  errors: FieldErrors<ReservationFormValues>
  disabled: boolean
}

export function ReservationCriteriaForm({ register, errors, disabled }: ReservationCriteriaFormProps) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
      <TextField
        label="Start date and time"
        type="datetime-local"
        disabled={disabled}
        slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: toDateTimeLocalValue(new Date()) } }}
        error={!!errors.startAt}
        helperText={errors.startAt?.message}
        {...register('startAt')}
      />
      <TextField
        label="Number of days"
        type="number"
        disabled={disabled}
        slotProps={{ htmlInput: { min: 1 } }}
        error={!!errors.numberOfDays}
        helperText={errors.numberOfDays?.message}
        {...register('numberOfDays', { valueAsNumber: true })}
      />
    </Stack>
  )
}
