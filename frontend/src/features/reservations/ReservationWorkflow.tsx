import { zodResolver } from '@hookform/resolvers/zod'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useEffect, useReducer } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { ApiError, createReservation, getAvailability } from '../../api/client'
import type { CarType } from '../../api/types'
import { CalculatedReturnDate } from './components/CalculatedReturnDate'
import { CarTypeAvailabilityGrid } from './components/CarTypeAvailabilityGrid'
import { ReservationCriteriaForm } from './components/ReservationCriteriaForm'
import { ReservationFeedback } from './components/ReservationFeedback'
import { ReservationSummary } from './components/ReservationSummary'
import { useAvailability } from './hooks/useAvailability'
import { useDebouncedValue } from './hooks/useDebouncedValue'
import {
  initialReservationFlowState,
  reservationFlowReducer,
  selectLoadedAvailability,
} from './model/reservationFlowReducer'
import { reservationCriteriaSchema, reservationRequestSchema, type ReservationFormValues } from './model/schema'
import { toApiDateTime } from './utils/dateTime'

const AVAILABILITY_DEBOUNCE_MS = 300

const DEFAULT_VALUES: ReservationFormValues = {
  startAt: '',
  numberOfDays: 1,
  carType: undefined as unknown as CarType,
}

export function ReservationWorkflow() {
  const [state, dispatch] = useReducer(reservationFlowReducer, initialReservationFlowState)

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ReservationFormValues>({
    resolver: zodResolver(reservationRequestSchema),
    defaultValues: DEFAULT_VALUES,
  })

  const watchedStartAt = useWatch({ control, name: 'startAt' })
  const watchedNumberOfDays = useWatch({ control, name: 'numberOfDays' })
  const watchedCarType = useWatch({ control, name: 'carType' })

  const debouncedStartAt = useDebouncedValue(watchedStartAt, AVAILABILITY_DEBOUNCE_MS)
  const debouncedNumberOfDays = useDebouncedValue(watchedNumberOfDays, AVAILABILITY_DEBOUNCE_MS)

  const parsedCriteria = reservationCriteriaSchema.safeParse({
    startAt: debouncedStartAt,
    numberOfDays: debouncedNumberOfDays,
  })
  const validCriteria = parsedCriteria.success ? parsedCriteria.data : null

  const { retry } = useAvailability(validCriteria, dispatch)

  // If the criteria become invalid after having been valid (e.g. the date
  // field is cleared), drop any previously chosen type: it was chosen
  // against availability that's no longer being shown.
  useEffect(() => {
    if (!parsedCriteria.success) {
      setValue('carType', undefined as unknown as CarType)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parsedCriteria.success, setValue])

  const loadedAvailability = selectLoadedAvailability(state)
  // The debounce leaves a window where `state` is still 'ready' with
  // availability for the *previous* criteria while the form already shows
  // new values. Comparing against the raw (undebounced) watched values
  // closes that window: submission is blocked until the loaded availability
  // actually corresponds to what's on screen.
  const availabilityMatchesCurrentCriteria =
    loadedAvailability !== null &&
    loadedAvailability.criteria.startAt === watchedStartAt &&
    loadedAvailability.criteria.numberOfDays === watchedNumberOfDays

  const selectedType: CarType | null = watchedCarType ?? null
  const selectedAvailableCount =
    loadedAvailability?.response.availability.find((entry) => entry.carType === selectedType)?.availableCount ?? 0

  const isSubmittableState = state.status === 'ready' || (state.status === 'unavailable' && state.availability !== undefined)
  const canSubmit =
    isSubmittableState && availabilityMatchesCurrentCriteria && selectedType !== null && selectedAvailableCount > 0
  const isSubmitting = state.status === 'submitting'

  async function onSubmit(values: ReservationFormValues) {
    if (!isSubmittableState || !availabilityMatchesCurrentCriteria) {
      return
    }
    dispatch({ type: 'SUBMIT_STARTED' })
    try {
      const result = await createReservation({
        carType: values.carType,
        startAt: toApiDateTime(values.startAt),
        numberOfDays: values.numberOfDays,
      })

      if (result.kind === 'confirmed') {
        dispatch({ type: 'RESERVATION_CONFIRMED', reservation: result.reservation })
        return
      }

      dispatch({ type: 'RESERVATION_UNAVAILABLE', message: result.detail })
      try {
        const refreshed = await getAvailability(toApiDateTime(values.startAt), values.numberOfDays)
        dispatch({
          type: 'AVAILABILITY_REFRESHED',
          criteria: { startAt: values.startAt, numberOfDays: values.numberOfDays },
          availability: refreshed,
        })
      } catch {
        // Keep the unavailable message visible even if the refresh itself fails.
      }
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
      dispatch({ type: 'SUBMIT_FAILED', message })
    }
  }

  function handleReserveAnother() {
    dispatch({ type: 'RESET' })
    reset(DEFAULT_VALUES)
  }

  return (
    <Stack spacing={3} component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <div>
        <Typography variant="h4" component="h1" gutterBottom>
          Reserve a car
        </Typography>
        <Typography color="text.secondary">Choose your dates, then pick from the available car types below.</Typography>
      </div>

      <ReservationCriteriaForm
        register={register}
        errors={errors}
        disabled={isSubmitting || state.status === 'confirmed'}
      />

      {watchedStartAt && watchedNumberOfDays > 0 && (
        <CalculatedReturnDate startAt={watchedStartAt} numberOfDays={watchedNumberOfDays} />
      )}

      {state.status === 'confirmed' ? (
        <ReservationFeedback state={state} onRetry={retry} onReserveAnother={handleReserveAnother} />
      ) : (
        <>
          {state.status === 'editing' ? (
            <Typography color="text.secondary">Enter your dates to see availability.</Typography>
          ) : (
            <CarTypeAvailabilityGrid
              availability={loadedAvailability?.response.availability ?? null}
              loading={state.status === 'loading-availability'}
              selected={selectedType}
              onSelect={(carType) => setValue('carType', carType, { shouldValidate: true })}
            />
          )}

          {selectedType && watchedStartAt && (
            <ReservationSummary carType={selectedType} startAt={watchedStartAt} numberOfDays={watchedNumberOfDays} />
          )}

          <ReservationFeedback state={state} onRetry={retry} onReserveAnother={handleReserveAnother} />

          <div>
            <Button type="submit" variant="contained" disabled={!canSubmit || isSubmitting}>
              {isSubmitting ? <CircularProgress size={20} /> : 'Reserve'}
            </Button>
          </div>
        </>
      )}
    </Stack>
  )
}
