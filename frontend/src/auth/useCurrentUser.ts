import { useContext } from 'react'
import { CurrentUserContext, type CurrentUser } from './currentUser'

export function useCurrentUser(): CurrentUser {
  return useContext(CurrentUserContext)
}
