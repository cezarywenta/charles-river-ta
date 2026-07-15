import Box from '@mui/material/Box'
import Skeleton from '@mui/material/Skeleton'
import type { CarTypeAvailability } from '../../../api/types'
import type { CarType } from '../../../api/types'
import { CarTypeCard } from './CarTypeCard'

interface CarTypeAvailabilityGridProps {
  availability: CarTypeAvailability[] | null
  loading: boolean
  selected: CarType | null
  onSelect: (carType: CarType) => void
}

export function CarTypeAvailabilityGrid({ availability, loading, selected, onSelect }: CarTypeAvailabilityGridProps) {
  if (loading || !availability) {
    return (
      <Box data-testid="availability-loading" sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
        {[0, 1, 2].map((key) => (
          <Skeleton key={key} variant="rounded" height={88} />
        ))}
      </Box>
    )
  }

  return (
    <Box data-testid="availability-grid" sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
      {availability.map((entry) => (
        <CarTypeCard
          key={entry.carType}
          carType={entry.carType}
          availableCount={entry.availableCount}
          selected={selected === entry.carType}
          onSelect={() => onSelect(entry.carType)}
        />
      ))}
    </Box>
  )
}
