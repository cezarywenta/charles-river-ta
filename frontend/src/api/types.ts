export type CarType = 'SEDAN' | 'SUV' | 'VAN'

export type ReservationStatus = 'CONFIRMED' | 'CANCELLED'

export interface Reservation {
  reservationId: string
  carType: CarType
  startAt: string
  endAt: string
  status: ReservationStatus
  createdAt: string
}

export interface CarTypeAvailability {
  carType: CarType
  availableCount: number
}

export interface AvailabilityResponse {
  startAt: string
  endAt: string
  availability: CarTypeAvailability[]
}

export interface CreateReservationRequest {
  carType: CarType
  startAt: string
  numberOfDays: number
}

/** RFC 7807-shaped error body returned by the backend's ApiExceptionHandler. */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  instance?: string
}
