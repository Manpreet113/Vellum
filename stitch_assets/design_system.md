## Neo-Reader Design System

### Style Guidelines
## Brand & Style

The design system is engineered for the modern intellectual—users who demand a premium, focused reading environment that feels both futuristic and grounding. The brand personality is sophisticated, avant-garde, and hyper-clean. 

The aesthetic follows a **Neo-minimalist** direction, utilizing vast "Ink Black" space to eliminate visual noise. This is layered with **Glassmorphism** to provide a sense of depth and physical presence to the interface without breaking the minimalist flow. The emotional response is one of "focused luxury": the interface stays out of the way until needed, appearing as frosted glass floating over a void.

## Layout & Spacing

The layout utilizes a **Fixed Grid** for reading content to preserve the ideal line length, while the UI chrome follows a **Fluid** model.

- **Margins:** Use aggressive outer margins (48px+) on desktop to create a centered, focused reading column.
- **Rhythm:** All spacing must be a multiple of 8px. Use 80px (xl) spacing to separate major content sections to emphasize the minimalist aesthetic.
- **The Reading Column:** Maximize the width of the reading area at 720px for optimal focus.

## Elevation & Depth

Hierarchy is established through transparency and blur rather than traditional drop shadows.

- **Background Layers:** The lowest layer is the solid #0A0A0A background.
- **Glass Layers:** Navigation bars and floating menus use a 20px backdrop blur with a 5% white tint and a 1px white border at 10% opacity. 
- **Active States:** Elements being interacted with should increase in brightness or "glow" with a subtle Indigo shadow (blur: 24px, opacity: 0.3) to simulate a light-emitting interface.

## Components

- **Buttons:** Primary buttons are solid Electric Indigo with white text. Secondary buttons are "ghost" style with a 1px white border at 20% opacity. All buttons use a 32px height for mobile and 48px for desktop.
- **Navigation Bar:** A floating glassmorphic dock at the bottom of the screen. Icons are thick-stroked (2px minimum) and high-contrast white.
- **Cards:** Cards should not have background colors; instead, use a 1px border (#FFFFFF at 10% opacity) and a subtle backdrop blur to separate them from the background.
- **Progress Indicators:** Linear, thin bars in Electric Indigo. For reading progress, use a vertical bar on the right edge of the screen.
- **Typography Toggles:** Large, tactile pill-switches for adjusting font size and serif/sans-serif modes.
- **Inputs:** Minimalist underlines or glassmorphic fields with 24px+ radius. Use Indigo for the caret and active border state.

### Theme Data
```json
{
  "colorMode": "DARK",
  "font": "EPILOGUE",
  "roundness": "ROUND_FULL",
  "customColor": "#6366F1",
  "headlineFont": "EPILOGUE",
  "bodyFont": "NEWSREADER",
  "labelFont": "INTER",
  "namedColors": {
    "background": "#131313",
    "error": "#ffb4ab",
    "error_container": "#93000a",
    "inverse_on_surface": "#313030",
    "inverse_primary": "#494bd6",
    "inverse_surface": "#e5e2e1",
    "on_background": "#e5e2e1",
    "on_error": "#690005",
    "on_error_container": "#ffdad6",
    "on_primary": "#1000a9",
    "on_primary_container": "#0d0096",
    "on_primary_fixed": "#07006c",
    "on_primary_fixed_variant": "#2f2ebe",
    "on_secondary": "#313030",
    "on_secondary_container": "#bab8b7",
    "on_secondary_fixed": "#1c1b1b",
    "on_secondary_fixed_variant": "#474646",
    "on_surface": "#e5e2e1",
    "on_surface_variant": "#c7c4d7",
    "on_tertiary": "#2f3131",
    "on_tertiary_container": "#040506",
    "on_tertiary_fixed": "#1a1c1c",
    "on_tertiary_fixed_variant": "#454747",
    "outline": "#908fa0",
    "outline_variant": "#464554",
    "primary": "#c0c1ff",
    "primary_container": "#8083ff",
    "primary_fixed": "#e1e0ff",
    "primary_fixed_dim": "#c0c1ff",
    "secondary": "#c9c6c5",
    "secondary_container": "#4a4949",
    "secondary_fixed": "#e5e2e1",
    "secondary_fixed_dim": "#c9c6c5",
    "surface": "#131313",
    "surface_bright": "#393939",
    "surface_container": "#20201f",
    "surface_container_high": "#2a2a2a",
    "surface_container_highest": "#353535",
    "surface_container_low": "#1c1b1b",
    "surface_container_lowest": "#0e0e0e",
    "surface_dim": "#131313",
    "surface_tint": "#c0c1ff",
    "surface_variant": "#353535",
    "tertiary": "#c6c6c7",
    "tertiary_container": "#767777",
    "tertiary_fixed": "#e2e2e2",
    "tertiary_fixed_dim": "#c6c6c7"
  }
}
```
