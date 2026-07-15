import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { CarType } from '../../../api/types'

const CAR_TYPE_LABELS: Record<CarType, string> = {
  SEDAN: 'Sedan',
  SUV: 'SUV',
  VAN: 'Van',
}

interface CarTypeCardProps {
  carType: CarType
  availableCount: number
  selected: boolean
  onSelect: () => void
}

export function CarTypeCard({ carType, availableCount, selected, onSelect }: CarTypeCardProps) {
  const isAvailable = availableCount > 0

  return (
    <Card variant="outlined" sx={{ borderColor: selected ? 'primary.main' : undefined, borderWidth: selected ? 2 : 1 }}>
      <CardActionArea
        disabled={!isAvailable}
        onClick={onSelect}
        aria-pressed={selected}
        sx={{ p: 2 }}
      >
        <CardContent sx={{ p: 0 }}>
          <Typography variant="h6" component="div">
            {CAR_TYPE_LABELS[carType]}
          </Typography>
          <Typography color={isAvailable ? 'text.secondary' : 'error'} variant="body2">
            {isAvailable ? `${availableCount} available` : 'Fully booked'}
          </Typography>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}
