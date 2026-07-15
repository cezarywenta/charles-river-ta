import type { ReactNode } from 'react'
import { CurrentUserContext, DEMO_USER } from './currentUser'

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  return <CurrentUserContext.Provider value={DEMO_USER}>{children}</CurrentUserContext.Provider>
}
