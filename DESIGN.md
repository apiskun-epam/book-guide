# BookShelf AI Design System

Visual direction: **Readwise Reader**. A calm, dense, dark-navy UI with a persistent left sidebar and list-based content. This file describes what the frontend actually does. When you change the visuals, update this file in the same change and remove anything it replaces.

Source of truth for tokens is `frontend/src/index.css` (`:root`). Use the variables; do not hard-code colors, radii or spacing.

## Layout

- App shell (`.app`): two-column grid, 280px sidebar + scrolling main pane (`.app-main`), full viewport height. Below 768px the sidebar becomes a top bar and nav labels hide.
- Sidebar (`Navigation`): `--color-sidebar` background, 1px right border, "BookShelf" wordmark, then icon + label nav items.
- Main pane: each page starts with a sticky 64px header (title left, page controls right) and a 1px bottom border. Content below it is left-aligned, max ~1040px wide for lists.
- Floating status pill (`Count: N`) sits fixed at the bottom right of list pages.
- Gutters: `--space-4` (32px) on desktop, `--space-2` (16px) on mobile. Spacing uses the 8px rhythm tokens.

## Color

All colors come from tokens.

| Role | Token | Value |
|---|---|---|
| Sidebar | `--color-sidebar` | #10141a |
| Page background | `--color-bg` | #151a21 |
| Raised surface (row hover, popovers, modal) | `--color-bg-elevated` | #1a2029 |
| Control / pill background | `--color-bg-interactive` | #1f2630 |
| Active / selected item | `--color-bg-active` | #232b36 |
| Text | `--color-text-primary` / `-secondary` / `-tertiary` | #e8eaee / #959cab / #5f6674 |
| Divider | `--color-border` | #232a34 |
| Control border | `--color-border-strong` | #313a47 |
| Primary action, focus | `--color-accent` (`-light` on hover) | #5b9cf5 |
| Star ratings only | `--color-primary` | #f5b544 |
| Destructive hover, errors | `--color-danger` | #f0716b |

Rules:
- Dark theme only. No light mode, no gradients.
- Blue is the only action color. Amber is reserved for star ratings.
- Text on blue buttons is dark (`#0b1220`).
- Separate content with low-contrast 1px borders and background shifts, not heavy shadows. The only shadows are on floating layers (search dropdown, modal).

## Typography

- Body and UI: `--font-body` (IBM Plex Sans), weights 400 / 500 / 600.
- Wordmark only: `--font-display` (Playfair Display), bold.
- No monospace; metadata uses the body font.
- Sizes (rem): page title 1.25, book title 1.0625, body 0.9375, secondary / meta 0.875, small controls 0.8125, tab label 0.75 (uppercase, letter-spaced).
- Hierarchy comes from weight (600 titles, 400 meta) and the three text colors, not from large size jumps.

## Shape

- Radii: `--radius-sm` 6px (nav items, thumbnails, dropdown items), `--radius-md` 10px (list rows, covers in modal, popovers), 16px (modal), `--radius-full` (buttons, inputs, pills).
- Book covers: 2:3 ratio, rounded, with a `--color-bg-interactive` placeholder block when there is no cover.

## Components

- **Nav item**: 18px line icon + label, `--color-text-secondary`. Hover `--color-bg-interactive`; active `--color-bg-active` with primary text.
- **Book row** (Collection): 56x84 cover, title (single line, ellipsis), one-line description, meta line `author • year • stars`. Rows are separated by a 1px divider inset past the cover. Hover: `--color-bg-elevated` background and the divider fades. Actions (Remove) appear on hover or focus-within, and are always visible on mobile.
- **Star rating**: five `★` buttons. Filled and hover use `--color-primary`, empty uses `--color-text-tertiary`. Modal stars are larger (scoped under `.rating-section`).
- **Primary button**: pill, `--color-accent` background, weight 600.
- **Secondary / destructive button** (Remove): pill, `--color-bg-interactive` with `--color-border-strong` border; turns `--color-danger` on hover.
- **Search**: pill input on `--color-sidebar` with `--color-border-strong` border, blue border on focus. Typeahead: results appear after 3 characters, debounced 300ms; Enter or the Search button searches immediately at any length. Results open in a rounded `--color-bg-elevated` dropdown (items are buttons) with cover thumbnail (40x60), title, author. The dropdown has its own searching, empty ("No books found.") and error states, and closes on outside click or Escape.
- **Detail sidebar** (Collection): clicking a book row (or its title) opens a 380px right pane, sticky full-height, 1px left border, `--color-bg`. Header is 64px with uppercase tabs (Book, Author) and a close button. Book tab: 160px 2:3 cover, title, author, Summary, Metadata (label/value grid). Author tab: initial avatar, name, collection stats, other books by the author (click to switch). The selected row uses `--color-bg-active`. The Count pill shifts left of the pane. Below 768px the pane is a full-screen overlay.
- **Modal** (search result preview): overlay `rgba(8, 10, 14, 0.75)`, 16px radius, `--color-bg-elevated`, two-column body (200px 2:3 cover + info) that stacks below 640px. Info uses the same type as the detail sidebar: 1.25rem title, secondary author, uppercase Summary and Metadata labels, label/value grid. The description comes with the search results; the empty state reads "No description available." and long text is clamped to 6 lines with Show more. Rating and the primary **Add to collection** button sit at the bottom. Closes on Escape, overlay click or ×.
- **Count pill**: `--color-bg-interactive`, `--color-border`, full radius.

## States

Every data view needs loading, empty and error states, centered, in `--color-text-secondary` (errors in `--color-danger`). Empty states use a 600-weight primary line plus a secondary hint.

## Motion

- 150ms `ease-out` for hover, focus and color changes. No scale transforms, no decorative animation.

## Responsive

- Breakpoints used: 768px (sidebar becomes top bar, list tightens, header wraps) and 640px (modal stacks).

## Implementation notes

- Scoped CSS file per component or page, next to its `.tsx`. CSS variables live only in `index.css`.
- Class names that style shared primitives (e.g. `.star`, `.star-rating`) must be scoped to their parent when a component needs a variant, because CSS is global.
- Reuse existing components (`Navigation`, `SearchBar`, `BookDetailModal`) before creating new ones.
- No inline styles except for dynamic values.
- Do not add shadcn/ui or another component library without asking (see CLAUDE.md working style).
