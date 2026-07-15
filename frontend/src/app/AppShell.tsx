import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useCurrentUser } from '../context/useCurrentUser'

const NAV_ROUTES = ['/reserve', '/reservations'] as const

export function AppShell() {
  const location = useLocation()
  const currentUser = useCurrentUser()
  const currentTab = (NAV_ROUTES as readonly string[]).includes(location.pathname) ? location.pathname : false

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Car Rental
          </Typography>
          <Tabs value={currentTab} textColor="primary" indicatorColor="primary">
            <Tab label="Reserve" value="/reserve" component={Link} to="/reserve" />
            <Tab label="My reservations" value="/reservations" component={Link} to="/reservations" />
          </Tabs>
          <Typography variant="body2" color="text.secondary">
            {currentUser.displayName}
          </Typography>
        </Toolbar>
      </AppBar>
      <Container component="main" sx={{ py: 4, flexGrow: 1 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
