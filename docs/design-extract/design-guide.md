# Comprehensive Design Guide

**Source URL:** [https://www.gumtree.com/](https://www.gumtree.com/)
**Generated:** Automated comprehensive extraction
**Viewport:** 1600x1200

---

## 📸 Visual Assets

### Screenshots

- **Desktop Viewport:** `viewport_screenshot.png`
- **Full Page:** `fullpage_screenshot.png`
- **Interactive States:** `interactive_hover.png`

### Responsive Screenshots

- **Mobile (375x812):** `responsive_mobile.png`
- **Tablet (768x1024):** `responsive_tablet.png`
- **Desktop (1920x1080):** `responsive_desktop.png`

---

## 🎨 Color System

### Primary Colors

```css
:root {
  /* Text Colors */
  --text-1: rgb(0, 0, 0);
  --text-2: rgb(60, 50, 65);
  --text-3: rgb(255, 255, 255);
  --text-4: rgb(99, 91, 103);
  --text-5: rgb(13, 73, 92);

  /* Background Colors */
  --bg-1: rgb(13, 71, 92);
  --bg-2: rgb(255, 255, 255);
  --bg-3: rgb(92, 224, 11);
  --bg-4: rgb(241, 241, 241);
  --bg-5: rgb(219, 218, 219);

  /* Border Colors */
  --border-1: rgb(0, 0, 0);
  --border-2: rgb(60, 50, 65);
  --border-3: rgb(255, 255, 255);
  --border-4: rgb(99, 91, 103);
  --border-5: rgb(216, 214, 217);
}
```

### Gradients

1. `linear-gradient(90deg, rgba(255, 255, 255, 0) 0%, rgb(241, 241, 241) 100%)`

### Shadow Colors

- `rgba(177, 173, 179, 0.3)`
- `rgba(0, 0, 0, 0.25)`
- `rgba(0, 0, 0, 0.2)`
- `rgb(183, 180, 183)`

---

## 📝 Typography System

### Font Stack

```css
:root {
  --font-1: "Readex Pro", sans-serif;
  --font-2: gumicon, serif;
}
```

### Type Scale

```css
:root {
  --text-1: 0px;
  --text-2: 12px;
  --text-3: 13px;
  --text-4: 13.008px;
  --text-5: 13.12px;
  --text-6: 13.28px;
  --text-7: 14px;
  --text-8: 15px;
  --text-9: 16px;
  --text-10: 17px;
}
```

### Heading Hierarchy


| Element | Font Size | Weight | Line Height | Letter Spacing |
| ------- | --------- | ------ | ----------- | -------------- |
| h1      | 20px      | 600    | 32px        | 0.2px          |
| h2      | 15px      | 330    | 24px        | 0.15px         |
| h3      | 13px      | 600    | 18.2px      | normal         |
| h4      | 26px      | 600    | 22px        | 0.26px         |


### Font Weights

- `330`
- `400`
- `500`
- `600`
- `700`

---

## 📐 Spacing & Layout

### Spacing Scale

```css
:root {
  /* Margins */
  --margin-1: -16px;
  --margin-2: -8px;
  --margin-3: -6px;
  --margin-4: -1px;
  --margin-5: 2px;
  --margin-6: 4px;
  --margin-7: 5px;
  --margin-8: 6px;
  --margin-9: 8px;
  --margin-10: 10px;

  /* Paddings */
  --padding-1: 1px;
  --padding-2: 3px;
  --padding-3: 4px;
  --padding-4: 5px;
  --padding-5: 6px;
  --padding-6: 8px;
  --padding-7: 10px;
  --padding-8: 11px;
  --padding-9: 12px;
  --padding-10: 14px;

  /* Gaps (Flexbox/Grid) */
  --gap-1: 4px;
  --gap-2: 8px;
  --gap-3: 16px;
}
```

### Border Radius

```css
:root {
  --radius-1: 0px 2px 2px 0px;
  --radius-2: 2px;
  --radius-3: 2px 0px 0px 2px;
  --radius-4: 4px;
  --radius-5: 8px;
  --radius-6: 16px;
  --radius-7: 20px;
  --radius-8: 50%;
}
```

---

## 🌟 Visual Effects

### Box Shadows

```css
/* Shadow 1 */
box-shadow: rgba(177, 173, 179, 0.3) 0px 1px 8px 0px;

/* Shadow 2 */
box-shadow: rgba(0, 0, 0, 0.25) 0px 4px 8px 0px;

/* Shadow 3 */
box-shadow: rgba(0, 0, 0, 0.2) 0px 0px 12px 0px;

/* Shadow 4 */
box-shadow: rgb(183, 180, 183) 0px 0px 3px 0px;

/* Shadow 5 */
box-shadow: rgba(0, 0, 0, 0.2) 0px 0px 18px 0px;

```

### Opacity Values

- `0`
- `0.12`
- `0.2`
- `0.25`

---

## ✨ Animations & Transitions

### Transitions

```css
/* Transition 1 */
transition: all;

/* Transition 2 */
transition: 0.1s;

/* Transition 3 */
transition: border-color 0.222s ease-out;

/* Transition 4 */
transition: background-color 0.222s ease-out;

/* Transition 5 */
transition: opacity 0.2s;

/* Transition 6 */
transition: box-shadow 0.15s ease-out;

/* Transition 7 */
transition: transform 0.1s linear;

/* Transition 8 */
transition: 0.3s ease-in;

```

### Keyframe Animations

#### @keyframes fade_collapse

```css
0% { max-height: none; opacity: 1; overflow: hidden; }
99% { opacity: 0; }
100% { display: none; overflow: hidden; max-height: 0px; opacity: 0; }
```

#### @keyframes onetrust-fade-in

```css
0% { opacity: 0; }
100% { opacity: 1; }
```

#### @keyframes animation-6msyyc

```css
0% { transform: translateY(-40px); }
100% { transform: translateY(-110px); }
```

---

## ⚡ Interactive States

---

## 🧩 Component Patterns

### Buttons

#### Button 1: "Post an ad"

```css
background-color: rgba(0, 0, 0, 0);
color: rgb(255, 255, 255);
padding: 0px;
border-radius: 2px;
border: 1px solid rgba(0, 0, 0, 0);
font-size: 12px;
font-weight: 330;
```

#### Button 2: "Sign up"

```css
background-color: rgba(0, 0, 0, 0);
color: rgb(255, 255, 255);
padding: 0px;
border-radius: 2px;
border: 1px solid rgba(0, 0, 0, 0);
font-size: 12px;
font-weight: 330;
```

#### Button 3: "Login"

```css
background-color: rgba(0, 0, 0, 0);
color: rgb(255, 255, 255);
padding: 0px;
border-radius: 2px;
border: 1px solid rgba(0, 0, 0, 0);
font-size: 12px;
font-weight: 330;
```

### Cards

#### Card Pattern 1

```css
background-color: rgb(255, 255, 255);
border-radius: 8px;
box-shadow: rgba(0, 0, 0, 0.2) 0px 0px 12px 0px;
padding: 0px;
```

#### Card Pattern 2

```css
background-color: rgb(255, 255, 255);
border-radius: 8px;
box-shadow: rgba(0, 0, 0, 0.2) 0px 0px 12px 0px;
padding: 0px;
```

#### Card Pattern 3

```css
background-color: rgb(255, 255, 255);
border-radius: 8px;
box-shadow: rgba(0, 0, 0, 0.2) 0px 0px 12px 0px;
padding: 0px;
```

---

## 🎭 UX Patterns

### Interaction Metrics

- **Interactive Elements:** 303
- **Scroll Behavior:** `auto`
- **Cursor Styles Used:** `pointer`, `text`, `default`

### Accessibility Features

- ARIA Labels: 54
- ARIA Descriptions: 0
- Role Attributes: 68
- Image Alt Texts: 57

### Sticky/Fixed Elements

- `div` - Position: `fixed`, Top: `0px`, Z-Index: `auto`
- `div` - Position: `fixed`, Top: `0px`, Z-Index: `auto`
- `div` - Position: `fixed`, Top: `0px`, Z-Index: `auto`
- `div` - Position: `fixed`, Top: `0px`, Z-Index: `auto`
- `div` - Position: `fixed`, Top: `0px`, Z-Index: `auto`

---

## 📱 Responsive Design

### Mobile (375x812)

- Viewport: 375x812
- Scroll Height: 11892px
- Body Width: 375px
- Screenshot: `responsive_mobile.png`

### Tablet (768x1024)

- Viewport: 768x1024
- Scroll Height: 11152px
- Body Width: 768px
- Screenshot: `responsive_tablet.png`

### Desktop (1920x1080)

- Viewport: 1920x1080
- Scroll Height: 6677px
- Body Width: 1920px
- Screenshot: `responsive_desktop.png`

---

## 🚀 Implementation Recommendations

### Step 1: Define Design Tokens

Create a comprehensive token system using CSS custom properties:

```css
:root {
  /* Use the color, typography, and spacing values above */
}
```

### Step 2: Implement Component Patterns

Use the extracted component styles for buttons, cards, forms, etc.

### Step 3: Apply Interactive States

Implement hover, focus, and active states as documented above.

### Step 4: Add Animations

Apply the transitions and keyframe animations for smooth interactions.

### Step 5: Ensure Responsive Behavior

Use the responsive patterns to create mobile-first, adaptive layouts.

### Step 6: Test Accessibility

Follow the accessibility patterns identified in the analysis.

---

## 📚 Files Reference

- `design-guide.md` - This comprehensive guide
- `design_data.json` - Complete raw data
- `extracted.html` - Original HTML
- `extracted.css` - All CSS styles
- `computed_styles.json` - Computed styles for every element
- `interactive_hover.png` - Hover state captures
- `responsive_*.png` - Responsive screenshots

---

**Last Updated:** {click.style('Auto-generated', fg='cyan')}
**Extraction Completeness:** {click.style('Comprehensive', fg='green')}