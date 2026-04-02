# Agent 2 — UX Designer
# HMI Design Specification: Suspension Mode Control

## 1. Display Specifications

### 1.1 Screen Configuration
- **Resolution**: 1280 x 720 pixels (landscape)
- **Aspect Ratio**: 16:9
- **Target Vehicle**: Android Automotive OS (Polestar 2 emulator)
- **Theme**: Dark automotive theme (optimized for driving)

### 1.2 Screen Layout
```
┌────────────────────────────────────────────────────────────────────┐
│ [Status Bar - 48px height]                                        │
│  ┌──────────┐                                  ┌───────────────┐ │
│  │ Speed:   │                                  │ Current Mode: │ │
│  │ 45 km/h  │                                  │    SPORT      │ │
│  └──────────┘                                  └───────────────┘ │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│                    [Main Content Area]                            │
│                                                                    │
│     ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐          │
│     │         │  │         │  │         │  │         │          │
│     │ COMFORT │  │  SPORT  │  │OFF-ROAD│  │  AUTO   │          │
│     │    ○    │  │    ●    │  │    ○    │  │    ○    │          │
│     │         │  │         │  │    ⚠    │  │         │          │
│     └─────────┘  └─────────┘  └─────────┘  └─────────┘          │
│                                                                    │
│     [Mode Buttons - 160x160px each, 24px gap]                    │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ [Warning Bar - 32px height, conditional]                          │
│ "Off-road mode restricted above 30 km/h"                          │
└────────────────────────────────────────────────────────────────────┘
```

## 2. Component Specifications

### 2.1 Mode Button States

| State | Background | Border | Icon Color | Label Color | Opacity |
|-------|------------|--------|------------|-------------|---------|
| **Normal** | Surface Variant (#2D2D2D) | None | On Surface (#FFFFFF) | On Surface (#FFFFFF) | 100% |
| **Selected** | Primary (#4A90D9) | None | On Primary (#000000) | On Primary (#000000) | 100% |
| **Disabled** | Surface (#1A1A1A) | None | On Surface (#666666) | On Surface (#666666) | 60% |
| **Error** | Error Container (#4A2020) | Error (#FF4444) 2px | Error (#FF4444) | Error (#FF4444) | 100% |
| **Loading** | Surface Variant (#2D2D2D) | Primary 2px | Primary (#4A90D9) animating | Primary (#4A90D9) | 100% |

### 2.2 Button Dimensions & Layout
- **Size**: 160 x 160 pixels
- **Corner Radius**: 16px
- **Icon Size**: 64 x 64 pixels
- **Label Font**: Body Large, 16sp, Medium (500)
- **Spacing between buttons**: 24px
- **Horizontal padding**: 48px centered
- **Vertical padding**: 24px top, 16px bottom for label

## 3. Color Palette (Material Design 3 Dark Automotive)

### 3.1 Primary Colors
| Token | Hex | Usage |
|-------|-----|-------|
| Primary | #4A90D9 | Selected state, primary actions |
| On Primary | #000000 | Text on primary |
| Primary Container | #1A3A5C | Button hover/pressed |
| On Primary Container | #D6E4FF | Text on primary container |

### 3.2 Surface Colors
| Token | Hex | Usage |
|-------|-----|-------|
| Surface | #1A1A1A | Screen background |
| Surface Variant | #2D2D2D | Card/button background |
| On Surface | #FFFFFF | Primary text |
| On Surface Variant | #B0B0B0 | Secondary text |

### 3.3 Status Colors
| Token | Hex | Usage |
|-------|-----|-------|
| Error | #FF4444 | Error states, warnings |
| Error Container | #4A2020 | Error background |
| On Error | #000000 | Text on error |
| Warning | #FFB74D | Speed restriction warning |
| Success | #4CAF50 | Confirmation success |

### 3.4 Additional Colors
| Token | Hex | Usage |
|-------|-----|-------|
| Outline | #444444 | Dividers, borders |
| Outline Variant | #333333 | Subtle borders |

## 4. Typography

### 4.1 Font Specifications
- **Font Family**: Roboto (system default for Android Automotive)
- **Weight Map**:
  - Regular: 400
  - Medium: 500
  - Bold: 700

### 4.2 Text Styles
| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| Display Large | 32sp | 700 | Speed display (48 km/h) |
| Headline Medium | 24sp | 500 | Section headers |
| Title Large | 20sp | 500 | Current mode display |
| Title Medium | 16sp | 500 | Mode button labels |
| Body Large | 16sp | 400 | Status text |
| Body Medium | 14sp | 400 | Secondary info |
| Label Large | 14sp | 500 | Button states |
| Label Small | 11sp | 400 | Warning text |

## 5. Animation Specifications

### 5.1 Mode Transition Animation
- **Duration**: 300ms
- **Easing**: Ease-in-out curve (FastOutSlowIn)
- **Properties animated**:
  - Background color: fade 200ms
  - Scale: 1.0 → 0.95 → 1.0 (100ms down, 200ms up)
  - Icon rotation: 0° → 15° → 0° for selection feedback

### 5.2 Loading State Animation
- **Duration**: 1000ms per cycle, infinite
- **Type**: Circular progress indicator around icon
- **Color**: Primary (#4A90D9)

### 5.3 Warning Bar Animation
- **Entry**: Slide down + fade in, 200ms
- **Exit**: Slide up + fade out, 150ms

## 6. Icon Descriptions (Vector Drawable Specs)

### 6.1 COMFORT Mode Icon
- **Description**: Stylized soft spring/coil icon
- **Visual**: Horizontal coil spring, relaxed curves
- **Stroke**: 4px, rounded caps
- **Color**: On Surface (#FFFFFF)

### 6.2 SPORT Mode Icon
- **Description**: Dynamic/stylized arrow or performance indicator
- **Visual**: Upward-pointing chevron or speed arrow
- **Stroke**: 4px, rounded caps
- **Color**: On Surface (#FFFFFF)

### 6.3 OFF-ROAD Mode Icon
- **Description**: Terrain/mountain with vehicle silhouette
- **Visual**: Two peaks with vehicle on top
- **Stroke**: 4px, rounded caps
- **Additional**: Small warning triangle overlay when speed-restricted
- **Color**: On Surface (#FFFFFF), Warning (#FFB74D) for warning indicator

### 6.4 AUTO Mode Icon
- **Description**: Adaptive/auto symbol (circular arrow or brain)
- **Visual**: Circular arrow with center dot OR neural network nodes
- **Stroke**: 4px, rounded caps
- **Color**: On Surface (#FFFFFF)

## 7. Status Bar Specifications

### 7.1 Left Section - Speed Display
- **Icon**: Speedometer icon (24x24px)
- **Format**: "XXX km/h" or "--- km/h" if unavailable
- **Color**: On Surface (#FFFFFF)
- **Update frequency**: Real-time from VHAL

### 7.2 Right Section - Current Mode
- **Format**: Mode name in uppercase
- **Color**: Primary (#4A90D9) when active, On Surface (#B0B0B0) when unknown
- **Icon**: Small indicator matching current mode

## 8. Warning Bar Specifications

### 8.1 Speed Restriction Warning
- **Height**: 32px
- **Background**: Warning Container (#3D2A10)
- **Text**: "Off-road mode restricted above 30 km/h"
- **Icon**: Warning icon (16x16px), Warning color
- **Position**: Above main content, below status bar

### 8.2 Error Message Bar
- **Height**: 48px
- **Background**: Error Container (#4A2020)
- **Text**: Error message from error table
- **Icon**: Error icon (20x20px), Error color
- **Duration**: Persistent until error resolved

## 9. Confirmation Dialog (OFF-ROAD at Speed)

### 9.1 Dialog Specifications
- **Width**: 400px
- **Height**: Auto (content-based)
- **Background**: Surface Variant (#2D2D2D)
- **Corner Radius**: 24px
- **Shadow**: Elevation 8dp

### 9.2 Dialog Content
- **Title**: "Enable Off-Road Mode?"
- **Body**: "Off-road mode is not recommended above 30 km/h. Current speed: XX km/h"
- **Buttons**: 
  - Cancel (Secondary, left)
  - Confirm (Primary, right)

## 10. Accessibility

- **Touch target**: Minimum 48x48px (met by 160x160px buttons)
- **Content descriptions**: All icons have contentDescription for TalkBack
- **Contrast ratios**: Minimum 4.5:1 for text, 3:1 for icons
- **Haptic feedback**: Light vibration on button press

---
End of Agent 2 Output