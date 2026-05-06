# Design System: Kanso Editorial (Curator Manga App)

## 1. Overview & Creative North Star
**The Creative North Star: "The Digital Curator"**

This design system transcends the typical "scrolling grid" of media apps. It is inspired by high-end print editorials and premium digital libraries like Apple Books and MUBI. The goal is to transform manga reading into a serene, intentional experience.

**Key Principles:**
- **Editorial Hierarchy:** Use bold serif headings to create a sense of "cover story" importance.
- **Negative Space as Luxury:** Generous padding and margins (24px) are used to prevent visual clutter.
- **Paper-Like Tactility:** A warm, off-white background (#F9F8F6) reduces eye strain and mimics high-quality paper.
- **Soft Precision:** 16px radii on cards and containers provide a modern, approachable feel.

---

## 2. Visual Foundation

### Color Palette
- **Primary (Ink):** `#1A1A1A` — Used for main headers, primary buttons, and active states.
- **Background (Paper):** `#F9F8F6` — The primary surface color for all screens.
- **Surface (Card):** `#FFFFFF` — Used for floating elements and cards to create depth.
- **Text (Soft Ink):** `#2C2C2C` — High-readability body text.
- **Muted (Graphite):** `#8C8B88` — Secondary info, inactive icons, and subtle borders.
- **Accent (Terracotta):** `#C04A33` — Unread indicators, bookmarks, and alerts.

### Typography
- **Headings (Serif):** `Newsreader` (or Playfair Display), Bold.
  - H1: 32-36px (Hero/Title)
  - H2: 24px (Section Headers)
  - H3: 18-20px (Subtitles/Card Titles)
- **Body & Controls (Sans-serif):** `Cabinet Grotesk` (or Manrope/Inter).
  - Body: 16px, 1.6 line-height.
  - Small/Info: 13px.
  - Buttons: 14px, Medium, All-Caps, 0.1em tracking.

---

## 3. Layout & Components

### Spatial System
- **App Margins:** 24px (standard).
- **Gutter/Spacing:** 8px / 16px / 32px (Base-8 system).
- **Corner Radius:** 16px (Standard Card), 24px (Search/Pills).

### Core Components

#### 1. Top Bar (Editorial)
- **Style:** Minimal, background match (`#F9F8F6`) or blurred glass.
- **Logo:** Serif, Italic, 24px.
- **Actions:** Subtle icon buttons (Menu, Search, Avatar).

#### 2. Book Cover Card
- **Ratio:** 2:3 aspect ratio.
- **Shadow:** `0 4px 20px rgba(0,0,0,0.04)` — separation through soft depth, not borders.
- **Details:** Titles in serif, authors in muted sans-serif.

#### 3. Action Buttons
- **Primary:** Filled `#1A1A1A`, White text, rounded-lg/full.
- **Secondary:** Transparent or White background, 1px muted border or subtle tint.

#### 4. Bottom Navigation
- **Style:** Floating pill or full-width with backdrop blur.
- **Active State:** Scale-up effect or heavy underline.

---

## 4. Specific Screen Specs

### Discover
- Large hero "Spotlight" cards (340px height).
- Horizontal scrolling sections with 8px gaps between items.

### Manga Details
- Blurred backdrop header using the cover art.
- Overlapping cover art (160x240px) with heavy elevation (`0 8px 30px rgba(0,0,0,0.15)`).

### Reader
- Immersive black or white background.
- UI Overlays: 90% opacity `#1A1A1A` with serif page counters (`12 / 45`).