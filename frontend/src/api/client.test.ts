import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/msw/server'
import { ApiError, cancelReservation, createReservation, getAvailability, getReservation, listReservations } from './client'
import type { ProblemDetail, Reservation } from './types'

const RESERVATION: Reservation = {
  reservationId: '11111111-1111-1111-1111-111111111111',
  carType: 'SUV',
  startAt: '2027-01-10T10:00:00',
  endAt: '2027-01-12T10:00:00',
  status: 'CONFIRMED',
  createdAt: '2026-07-15T10:00:00Z',
}

function problem(overrides: Partial<ProblemDetail>): ProblemDetail {
  return {
    type: 'urn:problem:invalid-request',
    title: 'Invalid request',
    status: 400,
    detail: 'something went wrong',
    ...overrides,
  }
}

describe('createReservation', () => {
  it('maps 201 to a confirmed result', async () => {
    server.use(
      http.post('/api/reservations', () => HttpResponse.json(RESERVATION, { status: 201 })),
    )

    const result = await createReservation({ carType: 'SUV', startAt: RESERVATION.startAt, numberOfDays: 2 })

    expect(result).toEqual({ kind: 'confirmed', reservation: RESERVATION })
  })

  it('maps 409 to an unavailable result carrying the problem detail', async () => {
    server.use(
      http.post('/api/reservations', () =>
        HttpResponse.json(
          problem({
            type: 'urn:problem:car-unavailable',
            title: 'Car unavailable',
            status: 409,
            detail: 'No SUV is available for the selected period',
          }),
          { status: 409 },
        ),
      ),
    )

    const result = await createReservation({ carType: 'SUV', startAt: RESERVATION.startAt, numberOfDays: 2 })

    expect(result).toEqual({ kind: 'unavailable', detail: 'No SUV is available for the selected period' })
  })

  it('throws an ApiError for a 500 technical failure', async () => {
    server.use(
      http.post('/api/reservations', () =>
        HttpResponse.json(
          problem({ type: 'urn:problem:internal-error', title: 'Internal server error', status: 500, detail: 'An unexpected error occurred' }),
          { status: 500 },
        ),
      ),
    )

    await expect(
      createReservation({ carType: 'SUV', startAt: RESERVATION.startAt, numberOfDays: 2 }),
    ).rejects.toThrow(ApiError)
  })

  it('throws an ApiError with the response status even without a JSON body', async () => {
    server.use(http.post('/api/reservations', () => new HttpResponse(null, { status: 502 })))

    const error = await createReservation({ carType: 'SUV', startAt: RESERVATION.startAt, numberOfDays: 2 }).catch(
      (caught: unknown) => caught,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(502)
  })
})

describe('cancelReservation', () => {
  it('maps 200 to a cancelled result', async () => {
    const cancelled = { ...RESERVATION, status: 'CANCELLED' as const }
    server.use(http.patch(`/api/reservations/${RESERVATION.reservationId}`, () => HttpResponse.json(cancelled)))

    const result = await cancelReservation(RESERVATION.reservationId)

    expect(result).toEqual({ kind: 'cancelled', reservation: cancelled })
  })

  it('maps 404 to a not-found result', async () => {
    server.use(
      http.patch(`/api/reservations/${RESERVATION.reservationId}`, () =>
        HttpResponse.json(problem({ status: 404, detail: 'No reservation found' }), { status: 404 }),
      ),
    )

    const result = await cancelReservation(RESERVATION.reservationId)

    expect(result).toEqual({ kind: 'not-found' })
  })

  it('maps 409 to a not-allowed result', async () => {
    server.use(
      http.patch(`/api/reservations/${RESERVATION.reservationId}`, () =>
        HttpResponse.json(
          problem({ status: 409, detail: 'The reservation can no longer be cancelled' }),
          { status: 409 },
        ),
      ),
    )

    const result = await cancelReservation(RESERVATION.reservationId)

    expect(result).toEqual({ kind: 'not-allowed', detail: 'The reservation can no longer be cancelled' })
  })
})

describe('getAvailability', () => {
  it('returns the parsed availability on 200', async () => {
    const availability = {
      startAt: '2027-01-10T10:00:00',
      endAt: '2027-01-12T10:00:00',
      availability: [
        { carType: 'SEDAN', availableCount: 3 },
        { carType: 'SUV', availableCount: 0 },
        { carType: 'VAN', availableCount: 1 },
      ],
    }
    server.use(http.get('/api/availability', () => HttpResponse.json(availability)))

    const result = await getAvailability('2027-01-10T10:00:00', 2)

    expect(result).toEqual(availability)
  })
})

describe('listReservations', () => {
  it('returns the parsed list on 200', async () => {
    server.use(http.get('/api/reservations', () => HttpResponse.json([RESERVATION])))

    const result = await listReservations()

    expect(result).toEqual([RESERVATION])
  })
})

describe('getReservation', () => {
  it('returns the reservation on 200', async () => {
    server.use(http.get(`/api/reservations/${RESERVATION.reservationId}`, () => HttpResponse.json(RESERVATION)))

    const result = await getReservation(RESERVATION.reservationId)

    expect(result).toEqual(RESERVATION)
  })

  it('returns null on 404', async () => {
    server.use(
      http.get(`/api/reservations/${RESERVATION.reservationId}`, () =>
        HttpResponse.json(problem({ status: 404 }), { status: 404 }),
      ),
    )

    const result = await getReservation(RESERVATION.reservationId)

    expect(result).toBeNull()
  })
})
