/**
 * The big card image in the deck view's left rail: whatever card the pointer is over in the list,
 * shown at full size. The deckbuilder's Deck view and the shared-cube page both use it, so a list
 * read in either place previews the same way.
 */
import type React from 'react'
import { getCardImageUrl } from '@/utils/cardImages'
import styles from './deckbuilder.module.css'

export function DeckHoverPreview({
  name,
  imageUri,
  hint,
  imageRotateDeg = 0,
}: {
  name: string | null
  imageUri: string | null
  hint?: React.ReactNode
  imageRotateDeg?: 0 | 90
}) {
  if (!name) {
    return (
      <div className={styles.deckHoverPreviewEmpty}>
        Hover a card to preview it here.
      </div>
    )
  }
  const imageUrl = getCardImageUrl(name, imageUri, 'large')
  const landscape = imageRotateDeg === 90
  return (
    <div className={styles.deckHoverPreview}>
      {/* Split cards (Rooms etc.) are printed sideways: swap the wrap to a landscape aspect
          ratio and rotate the portrait image into it. The percentage dims are the relative
          equivalent of HoverCardPreview's pixel approach — width 5/7 and height 7/5 of the
          landscape box make the rotated 5:7 image fill it exactly. */}
      <div
        className={styles.deckHoverPreviewImageWrap}
        style={landscape ? { aspectRatio: '7 / 5' } : undefined}
      >
        <img
          className={styles.deckHoverPreviewImage}
          src={imageUrl}
          alt={name}
          style={
            landscape
              ? {
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  width: '71.4286%',
                  height: '140%',
                  transform: 'translate(-50%, -50%) rotate(90deg)',
                }
              : undefined
          }
        />
      </div>
      {hint}
    </div>
  )
}
