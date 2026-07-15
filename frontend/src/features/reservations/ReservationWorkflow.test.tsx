import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw/server'
import type { AvailabilityResponse, CarType, Reservation } from '../../api/types'
import { ReservationWorkflow } from './ReservationWorkflow'

function availabilityResponse(counts: Record<CarType, number>): AvailabilityResponse {
  return {
    startAt: '2027-01-10T10:00:00',
    endAt: '2027-01-12T10:00:00',
    availability: (['SEDAN', 'SUV', 'VAN'] as const).map((carType) => ({
      carType,
      availableCount: counts[carType],
    })),
  }
}

function mockAvailability(counts: Record<CarType, number>) {
  server.use(http.get('/api/availability', () => HttpResponse.json(availabilityResponse(counts))))
}

// Debounce/delay timers fire outside of userEvent's own act() wrapping;
// wrapping the wait itself avoids "not wrapped in act(...)" noise.
async function wait(ms: number) {
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, ms))
  })
}

async function setStartAt(value: string) {
  // datetime-local inputs are unreliable with userEvent.type; fireEvent covers it directly.
  const input = screen.getByLabelText('Start date and time')
  fireEvent.change(input, { target: { value } })
}

describe('ReservationWorkflow', () => {
  it('calculates and displays the return date as criteria change', async () => {
    render(<ReservationWorkflow />)
    mockAvailability({ SEDAN: 3, SUV: 2, VAN: 1 })

    await setStartAt('2027-01-10T10:00')

    expect(await screen.findByTestId('calculated-return-date')).toHaveTextContent('Jan 11, 2027')
  })

  it('shows a loading indicator while availability is being fetched', async () => {
    server.use(
      http.get('/api/availability', async () => {
        await delay(200)
        return HttpResponse.json(availabilityResponse({ SEDAN: 3, SUV: 2, VAN: 1 }))
      }),
    )
    render(<ReservationWorkflow />)

    await setStartAt('2027-01-10T10:00')

    await waitFor(() => expect(screen.getByTestId('availability-loading')).toBeInTheDocument(), { timeout: 2000 })
    await waitFor(() => expect(screen.getByTestId('availability-grid')).toBeInTheDocument(), { timeout: 2000 })
  })

  it('does not let the user select a fully booked car type', async () => {
    mockAvailability({ SEDAN: 3, SUV: 0, VAN: 1 })
    render(<ReservationWorkflow />)

    await setStartAt('2027-01-10T10:00')
    await screen.findByTestId('availability-grid')

    const suvButton = screen.getByRole('button', { name: /SUV/i })
    expect(suvButton).toBeDisabled()

    // userEvent.click correctly refuses to click a pointer-events:none element,
    // which is itself proof the card is non-interactive. Selecting an available
    // type still works, showing the grid as a whole isn't broken.
    await userEvent.click(screen.getByRole('button', { name: /Sedan/i }))

    expect(screen.getByRole('button', { name: 'Reserve' })).toBeEnabled()
  })

  it('shows a success confirmation after a successful reservation', async () => {
    mockAvailability({ SEDAN: 3, SUV: 2, VAN: 1 })
    const reservation: Reservation = {
      reservationId: '11111111-1111-1111-1111-111111111111',
      carType: 'SEDAN',
      startAt: '2027-01-10T10:00:00',
      endAt: '2027-01-12T10:00:00',
      status: 'CONFIRMED',
      createdAt: '2026-07-15T10:00:00Z',
    }
    server.use(http.post('/api/reservations', () => HttpResponse.json(reservation, { status: 201 })))

    render(<ReservationWorkflow />)
    await setStartAt('2027-01-10T10:00')
    await screen.findByTestId('availability-grid')

    await userEvent.click(screen.getByRole('button', { name: /Sedan/i }))
    await userEvent.click(screen.getByRole('button', { name: 'Reserve' }))

    expect(await screen.findByText('Reservation confirmed.')).toBeInTheDocument()
  })

  it('preserves form values and shows a conflict message on 409', async () => {
    mockAvailability({ SEDAN: 3, SUV: 2, VAN: 1 })
    render(<ReservationWorkflow />)
    await setStartAt('2027-01-10T10:00')
    await screen.findByTestId('availability-grid')
    await userEvent.click(screen.getByRole('button', { name: /Sedan/i }))

    server.use(
      http.post('/api/reservations', () =>
        HttpResponse.json(
          {
            type: 'urn:problem:car-unavailable',
            title: 'Car unavailable',
            status: 409,
            detail: 'No SEDAN is available for the selected period',
          },
          { status: 409 },
        ),
      ),
    )
    // The submit handler refreshes availability after a conflict; keep responding.
    mockAvailability({ SEDAN: 0, SUV: 2, VAN: 1 })

    await userEvent.click(screen.getByRole('button', { name: 'Reserve' }))

    expect(await screen.findByText(/No SEDAN is available for the selected period/)).toBeInTheDocument()
    expect(screen.getByLabelText('Start date and time')).toHaveValue('2027-01-10T10:00')
    expect(screen.getByLabelText('Number of days')).toHaveValue(1)
  })

  it('allows selecting a different type and resubmitting after a 409 conflict', async () => {
    mockAvailability({ SEDAN: 3, SUV: 2, VAN: 1 })
    render(<ReservationWorkflow />)
    await setStartAt('2027-01-10T10:00')
    await screen.findByTestId('availability-grid')
    await userEvent.click(screen.getByRole('button', { name: /Sedan/i }))

    server.use(
      http.post('/api/reservations', () =>
        HttpResponse.json(
          {
            type: 'urn:problem:car-unavailable',
            title: 'Car unavailable',
            status: 409,
            detail: 'No SEDAN is available for the selected period',
          },
          { status: 409 },
        ),
      ),
    )
    // The post-conflict refresh reflects SEDAN now full, SUV still open.
    mockAvailability({ SEDAN: 0, SUV: 2, VAN: 1 })

    await userEvent.click(screen.getByRole('button', { name: 'Reserve' }))
    await screen.findByText(/No SEDAN is available for the selected period/)

    expect(screen.getByRole('button', { name: /Sedan/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Reserve' })).toBeDisabled()

    const reservation: Reservation = {
      reservationId: '22222222-2222-2222-2222-222222222222',
      carType: 'SUV',
      startAt: '2027-01-10T10:00:00',
      endAt: '2027-01-12T10:00:00',
      status: 'CONFIRMED',
      createdAt: '2026-07-15T10:00:00Z',
    }
    server.use(http.post('/api/reservations', () => HttpResponse.json(reservation, { status: 201 })))

    await userEvent.click(screen.getByRole('button', { name: /SUV/i }))
    expect(screen.getByRole('button', { name: 'Reserve' })).toBeEnabled()

    await userEvent.click(screen.getByRole('button', { name: 'Reserve' }))

    expect(await screen.findByText('Reservation confirmed.')).toBeInTheDocument()
  })

  it('blocks submission while the loaded availability no longer matches the current criteria', async () => {
    server.use(
      http.get('/api/availability', async ({ request }) => {
        const url = new URL(request.url)
        const numberOfDays = url.searchParams.get('numberOfDays')
        if (numberOfDays === '1') {
          return HttpResponse.json(availabilityResponse({ SEDAN: 3, SUV: 2, VAN: 1 }))
        }
        await delay(500)
        return HttpResponse.json(availabilityResponse({ SEDAN: 3, SUV: 2, VAN: 1 }))
      }),
    )

    render(<ReservationWorkflow />)
    await setStartAt('2027-01-10T10:00')
    await screen.findByTestId('availability-grid')
    await userEvent.click(screen.getByRole('button', { name: /Sedan/i }))
    expect(screen.getByRole('button', { name: 'Reserve' })).toBeEnabled()

    // Change numberOfDays: the debounce hasn't fired yet, so the grid still
    // shows availability for the old criteria. Submission must be blocked
    // even though the reducer status is still 'ready'.
    const numberOfDaysInput = screen.getByLabelText('Number of days')
    await userEvent.clear(numberOfDaysInput)
    await userEvent.type(numberOfDaysInput, '10')

    expect(screen.getByRole('button', { name: 'Reserve' })).toBeDisabled()
  })

  it('does not let a stale availability response overwrite a newer one', async () => {
    server.use(
      http.get('/api/availability', async ({ request }) => {
        const url = new URL(request.url)
        const numberOfDays = url.searchParams.get('numberOfDays')
        if (numberOfDays === '3') {
          await delay(400)
          return HttpResponse.json(availabilityResponse({ SEDAN: 1, SUV: 1, VAN: 1 }))
        }
        return HttpResponse.json(availabilityResponse({ SEDAN: 9, SUV: 9, VAN: 9 }))
      }),
    )

    render(<ReservationWorkflow />)
    await setStartAt('2027-01-10T10:00')

    const numberOfDaysInput = screen.getByLabelText('Number of days')
    await userEvent.clear(numberOfDaysInput)
    await userEvent.type(numberOfDaysInput, '3')
    await wait(350) // let the slow (3-day) request actually start

    await userEvent.clear(numberOfDaysInput)
    await userEvent.type(numberOfDaysInput, '7')
    await wait(350) // let the fast (7-day) request start and resolve

    await wait(450) // long enough for the slow request's response to have arrived too, if unguarded

    const grid = await screen.findByTestId('availability-grid')
    expect(within(grid).getAllByText('9 available')).toHaveLength(3)
    expect(within(grid).queryByText('1 available')).not.toBeInTheDocument()
  })
})
