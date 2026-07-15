import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { server } from '../../test/msw/server'
import type { Reservation } from '../../api/types'
import { ReservationManagement } from './ReservationManagement'

/**
 * This feature's own logic (upcoming/past categorization, cancel
 * eligibility) compares reservation dates against the real wall clock, so
 * fixtures are built relative to "now" rather than hardcoded absolute
 * dates - a hardcoded future date eventually becomes a past one and starts
 * failing these tests for reasons unrelated to the code under test.
 */
function toLocalDateTimeString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function relativeDateTime(hoursFromNow: number): string {
  return toLocalDateTimeString(new Date(Date.now() + hoursFromNow * 3600 * 1000))
}

function reservation(overrides: Partial<Reservation>): Reservation {
  return {
    reservationId: '11111111-1111-1111-1111-111111111111',
    carType: 'SEDAN',
    startAt: relativeDateTime(48),
    endAt: relativeDateTime(72),
    status: 'CONFIRMED',
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

const notYetStarted = reservation({
  reservationId: 'aaaaaaaa-0000-0000-0000-000000000001',
  carType: 'SEDAN',
  startAt: relativeDateTime(48),
  endAt: relativeDateTime(72),
  status: 'CONFIRMED',
})

const alreadyStarted = reservation({
  reservationId: 'aaaaaaaa-0000-0000-0000-000000000002',
  carType: 'SUV',
  startAt: relativeDateTime(-2),
  endAt: relativeDateTime(2),
  status: 'CONFIRMED',
})

const past = reservation({
  reservationId: 'aaaaaaaa-0000-0000-0000-000000000003',
  carType: 'VAN',
  startAt: relativeDateTime(-72),
  endAt: relativeDateTime(-48),
  status: 'CONFIRMED',
})

const cancelled = reservation({
  reservationId: 'aaaaaaaa-0000-0000-0000-000000000004',
  carType: 'SEDAN',
  startAt: relativeDateTime(24),
  endAt: relativeDateTime(48),
  status: 'CANCELLED',
})

function mockReservations(reservations: Reservation[]) {
  server.use(http.get('/api/reservations', () => HttpResponse.json(reservations)))
}

function cardTestId(r: Reservation) {
  return `reservation-card-${r.reservationId}`
}

beforeEach(() => {
  mockReservations([notYetStarted, alreadyStarted, past, cancelled])
})

describe('ReservationManagement', () => {
  it('shows upcoming reservations with their car type and status', async () => {
    render(<ReservationManagement />)

    const list = await screen.findByTestId('reservation-list')
    expect(within(list).getByTestId(cardTestId(notYetStarted))).toBeInTheDocument()
    expect(within(list).getByTestId(cardTestId(alreadyStarted))).toBeInTheDocument()
    expect(within(list).getAllByText('CONFIRMED')).toHaveLength(2)
    // past and cancelled reservations are not in the default "upcoming" tab
    expect(within(list).queryByTestId(cardTestId(past))).not.toBeInTheDocument()
    expect(within(list).queryByTestId(cardTestId(cancelled))).not.toBeInTheDocument()
  })

  it('filters reservations by tab', async () => {
    render(<ReservationManagement />)
    await screen.findByTestId('reservation-list')

    await userEvent.click(screen.getByRole('tab', { name: 'Past' }))
    let list = await screen.findByTestId('reservation-list')
    expect(within(list).getByTestId(cardTestId(past))).toBeInTheDocument()
    expect(within(list).queryByTestId(cardTestId(notYetStarted))).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('tab', { name: 'Cancelled' }))
    list = await screen.findByTestId('reservation-list')
    expect(within(list).getByTestId(cardTestId(cancelled))).toBeInTheDocument()
    expect(within(within(list).getByTestId(cardTestId(cancelled))).getByText('CANCELLED')).toBeInTheDocument()
  })

  it('shows an empty state message when a filter has no reservations', async () => {
    mockReservations([])
    render(<ReservationManagement />)

    expect(await screen.findByText('You have no upcoming reservations.')).toBeInTheDocument()
  })

  it('only offers cancellation for reservations that have not started yet', async () => {
    render(<ReservationManagement />)
    await screen.findByTestId('reservation-list')

    const notStartedCard = screen.getByTestId(cardTestId(notYetStarted))
    expect(within(notStartedCard).getByRole('button', { name: 'Cancel' })).toBeInTheDocument()

    const startedCard = screen.getByTestId(cardTestId(alreadyStarted))
    expect(within(startedCard).queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
  })

  it('requires confirmation before cancelling, then refreshes the list', async () => {
    render(<ReservationManagement />)
    await screen.findByTestId('reservation-list')

    await userEvent.click(
      within(screen.getByTestId(cardTestId(notYetStarted))).getByRole('button', { name: 'Cancel' }),
    )
    expect(await screen.findByText(/This will cancel your SEDAN reservation/)).toBeInTheDocument()

    // Backing out must not call the API.
    await userEvent.click(screen.getByRole('button', { name: 'Keep reservation' }))
    // Wait for the whole dialog to unmount, not just its text: MUI's Modal
    // only restores aria-hidden on the rest of the app once the exit
    // transition finishes, and until then background elements are
    // (correctly) invisible to role queries even though they're still in
    // the raw DOM.
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByTestId(cardTestId(notYetStarted))).toBeInTheDocument()

    let patchCalled = false
    server.use(
      http.patch(`/api/reservations/${notYetStarted.reservationId}`, () => {
        patchCalled = true
        return HttpResponse.json({ ...notYetStarted, status: 'CANCELLED' })
      }),
    )
    mockReservations([{ ...notYetStarted, status: 'CANCELLED' }, alreadyStarted, past, cancelled])

    await userEvent.click(
      within(screen.getByTestId(cardTestId(notYetStarted))).getByRole('button', { name: 'Cancel' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Cancel reservation' }))

    await waitFor(() => expect(patchCalled).toBe(true))
    // After cancelling, that reservation drops out of the "upcoming" tab.
    await waitFor(() => {
      expect(screen.queryByTestId(cardTestId(notYetStarted))).not.toBeInTheDocument()
    })
  })

  it('shows an error state with a retry action when loading fails', async () => {
    server.use(http.get('/api/reservations', () => HttpResponse.json(null, { status: 500 })))
    render(<ReservationManagement />)

    const retryButton = await screen.findByRole('button', { name: 'Retry' })
    expect(screen.queryByTestId('reservation-list')).not.toBeInTheDocument()

    mockReservations([notYetStarted])
    await userEvent.click(retryButton)

    expect(await screen.findByTestId(cardTestId(notYetStarted))).toBeInTheDocument()
  })
})
