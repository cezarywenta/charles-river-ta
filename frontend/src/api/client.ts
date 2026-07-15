import type {
  AvailabilityResponse,
  CreateReservationRequest,
  ProblemDetail,
  Reservation,
} from './types'

/**
 * Thrown for statuses that are not an expected business outcome of the call
 * (network failure, 500, or any status the caller didn't model). Callers
 * distinguish "the car isn't available" from "the request failed" by return
 * value vs. thrown error, not by inspecting status codes themselves.
 */
export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetail

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export type CreateReservationResult =
  | { kind: 'confirmed'; reservation: Reservation }
  | { kind: 'unavailable'; detail: string }

export type CancelReservationResult =
  | { kind: 'cancelled'; reservation: Reservation }
  | { kind: 'not-found' }
  | { kind: 'not-allowed'; detail: string }

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail | undefined
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    problem = undefined
  }
  return new ApiError(problem?.detail ?? `Request failed with status ${response.status}`, response.status, problem)
}

export async function getAvailability(
  startAt: string,
  numberOfDays: number,
  signal?: AbortSignal,
): Promise<AvailabilityResponse> {
  const params = new URLSearchParams({ startAt, numberOfDays: String(numberOfDays) })
  const response = await fetch(`/api/availability?${params}`, { signal })

  if (response.status === 200) {
    return (await response.json()) as AvailabilityResponse
  }
  throw await toApiError(response)
}

export async function createReservation(
  request: CreateReservationRequest,
  signal?: AbortSignal,
): Promise<CreateReservationResult> {
  const response = await fetch('/api/reservations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  })

  if (response.status === 201) {
    return { kind: 'confirmed', reservation: (await response.json()) as Reservation }
  }
  if (response.status === 409) {
    const problem = (await response.json()) as ProblemDetail
    return { kind: 'unavailable', detail: problem.detail }
  }
  throw await toApiError(response)
}

export async function listReservations(signal?: AbortSignal): Promise<Reservation[]> {
  const response = await fetch('/api/reservations', { signal })

  if (response.status === 200) {
    return (await response.json()) as Reservation[]
  }
  throw await toApiError(response)
}

export async function getReservation(reservationId: string, signal?: AbortSignal): Promise<Reservation | null> {
  const response = await fetch(`/api/reservations/${reservationId}`, { signal })

  if (response.status === 200) {
    return (await response.json()) as Reservation
  }
  if (response.status === 404) {
    return null
  }
  throw await toApiError(response)
}

export async function cancelReservation(reservationId: string, signal?: AbortSignal): Promise<CancelReservationResult> {
  const response = await fetch(`/api/reservations/${reservationId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'CANCELLED' }),
    signal,
  })

  if (response.status === 200) {
    return { kind: 'cancelled', reservation: (await response.json()) as Reservation }
  }
  if (response.status === 404) {
    return { kind: 'not-found' }
  }
  if (response.status === 409) {
    const problem = (await response.json()) as ProblemDetail
    return { kind: 'not-allowed', detail: problem.detail }
  }
  throw await toApiError(response)
}
