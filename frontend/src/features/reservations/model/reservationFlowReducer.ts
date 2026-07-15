import type { AvailabilityResponse, Reservation } from '../../../api/types'
import type { ReservationCriteria } from './schema'

/** Pairs a loaded availability response with the criteria it was fetched
 * for, so the UI can tell whether it still matches the current form values
 * (the debounce window otherwise leaves a gap where stale availability
 * could be submitted against different criteria). */
export interface LoadedAvailability {
  criteria: ReservationCriteria
  response: AvailabilityResponse
}

export type ReservationFlowState =
  | { status: 'editing' }
  | { status: 'loading-availability' }
  | { status: 'ready'; availability: LoadedAvailability }
  | { status: 'submitting'; availability: LoadedAvailability }
  | { status: 'confirmed'; reservation: Reservation }
  | { status: 'unavailable'; message: string; availability?: LoadedAvailability }
  | { status: 'failed'; message: string }

export type ReservationFlowAction =
  | { type: 'CRITERIA_CHANGED' }
  | { type: 'CRITERIA_CLEARED' }
  | { type: 'AVAILABILITY_LOADED'; criteria: ReservationCriteria; availability: AvailabilityResponse }
  | { type: 'AVAILABILITY_FAILED'; message: string }
  | { type: 'SUBMIT_STARTED' }
  | { type: 'RESERVATION_CONFIRMED'; reservation: Reservation }
  | { type: 'RESERVATION_UNAVAILABLE'; message: string }
  | { type: 'AVAILABILITY_REFRESHED'; criteria: ReservationCriteria; availability: AvailabilityResponse }
  | { type: 'SUBMIT_FAILED'; message: string }
  | { type: 'RESET' }

export const initialReservationFlowState: ReservationFlowState = { status: 'editing' }

export function selectLoadedAvailability(state: ReservationFlowState): LoadedAvailability | null {
  switch (state.status) {
    case 'ready':
    case 'submitting':
      return state.availability
    case 'unavailable':
      return state.availability ?? null
    default:
      return null
  }
}

/** A submission may proceed from 'ready', or from 'unavailable' once a
 * post-conflict refresh has attached a fresh availability snapshot. */
function isSubmittable(state: ReservationFlowState): boolean {
  return state.status === 'ready' || (state.status === 'unavailable' && state.availability !== undefined)
}

export function reservationFlowReducer(
  state: ReservationFlowState,
  action: ReservationFlowAction,
): ReservationFlowState {
  switch (action.type) {
    case 'CRITERIA_CHANGED':
      return { status: 'loading-availability' }

    case 'CRITERIA_CLEARED':
      return { status: 'editing' }

    case 'AVAILABILITY_LOADED':
      return { status: 'ready', availability: { criteria: action.criteria, response: action.availability } }

    case 'AVAILABILITY_FAILED':
      return { status: 'failed', message: action.message }

    case 'SUBMIT_STARTED': {
      if (!isSubmittable(state)) {
        return state
      }
      const availability = selectLoadedAvailability(state)
      return availability ? { status: 'submitting', availability } : state
    }

    case 'RESERVATION_CONFIRMED':
      return { status: 'confirmed', reservation: action.reservation }

    case 'RESERVATION_UNAVAILABLE': {
      const availability = state.status === 'submitting' ? state.availability : undefined
      return { status: 'unavailable', message: action.message, availability }
    }

    case 'AVAILABILITY_REFRESHED':
      return state.status === 'unavailable'
        ? { ...state, availability: { criteria: action.criteria, response: action.availability } }
        : state

    case 'SUBMIT_FAILED':
      return { status: 'failed', message: action.message }

    case 'RESET':
      return { status: 'editing' }

    default: {
      const exhaustiveCheck: never = action
      return exhaustiveCheck
    }
  }
}
