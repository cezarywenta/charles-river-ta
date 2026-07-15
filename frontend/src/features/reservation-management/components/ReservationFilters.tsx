import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import { RESERVATION_FILTERS, type ReservationFilter } from '../model/filters'

const FILTER_LABELS: Record<ReservationFilter, string> = {
  upcoming: 'Upcoming',
  past: 'Past',
  cancelled: 'Cancelled',
}

interface ReservationFiltersProps {
  value: ReservationFilter
  onChange: (filter: ReservationFilter) => void
}

export function ReservationFilters({ value, onChange }: ReservationFiltersProps) {
  return (
    <Tabs value={value} onChange={(_, next: ReservationFilter) => onChange(next)} textColor="primary" indicatorColor="primary">
      {RESERVATION_FILTERS.map((filter) => (
        <Tab key={filter} value={filter} label={FILTER_LABELS[filter]} />
      ))}
    </Tabs>
  )
}
