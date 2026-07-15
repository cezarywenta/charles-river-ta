import { z } from 'zod'

export const carTypeSchema = z.enum(['SEDAN', 'SUV', 'VAN'])

/** A one-minute tolerance so a start time the user picked in good faith
 * doesn't get rejected purely because a few seconds passed before submit;
 * the backend remains the authoritative check regardless. */
function isNotInPast(value: string): boolean {
  const selected = new Date(value).getTime()
  return selected >= Date.now() - 60_000
}

/** Governs when an availability fetch is triggered; carType is not part of
 * it since availability must be checked before a type is chosen. */
export const reservationCriteriaSchema = z.object({
  startAt: z
    .string()
    .min(1, 'Start date and time is required')
    .refine(isNotInPast, 'Start date must not be in the past'),
  numberOfDays: z
    .number()
    .int('Number of days must be a whole number')
    .positive('Number of days must be positive'),
})

export const reservationRequestSchema = reservationCriteriaSchema.extend({
  carType: carTypeSchema,
})

export type ReservationCriteria = z.infer<typeof reservationCriteriaSchema>
export type ReservationFormValues = z.infer<typeof reservationRequestSchema>
