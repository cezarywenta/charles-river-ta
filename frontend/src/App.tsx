import CssBaseline from '@mui/material/CssBaseline'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'

function App() {
  return (
    <>
      <CssBaseline />
      <Stack spacing={2} sx={{ p: 4 }}>
        <Typography variant="h4" component="h1">
          Car Rental
        </Typography>
        <Typography color="text.secondary">Frontend skeleton is wired up.</Typography>
      </Stack>
    </>
  )
}

export default App
