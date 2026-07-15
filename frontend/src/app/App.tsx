import CssBaseline from '@mui/material/CssBaseline'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './AppShell'
import { CurrentUserProvider } from '../auth/CurrentUserProvider'
import { ReservationsPage } from '../pages/ReservationsPage'
import { ReservePage } from '../pages/ReservePage'

function App() {
  return (
    <CurrentUserProvider>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<Navigate to="/reserve" replace />} />
            <Route path="/reserve" element={<ReservePage />} />
            <Route path="/reservations" element={<ReservationsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </CurrentUserProvider>
  )
}

export default App
