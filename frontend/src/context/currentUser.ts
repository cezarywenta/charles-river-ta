import { createContext } from 'react'

export interface CurrentUser {
  id: string
  displayName: string
}

/**
 * This is not authentication. The backend independently hardcodes the same
 * customer id; a production system would derive identity from an
 * authenticated principal on both sides, never from client-supplied data.
 */
export const DEMO_USER: CurrentUser = { id: 'customer-123', displayName: 'Demo Customer' }

export const CurrentUserContext = createContext<CurrentUser>(DEMO_USER)
