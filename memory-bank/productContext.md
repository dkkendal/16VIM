# Product Context: 8VIM

## Why This Project Exists

Mobile text input is frustrating. Standard QWERTY keyboards on touch screens require looking at the screen and are slow and error-prone. The 8pen keyboard introduced a revolutionary gesture-based approach that allowed for blind, fast, and fluid typing — but it was discontinued by its creators.

8VIM was started as a clone of 8pen to keep the concept alive, then expanded with a **Vim philosophy**: the keyboard itself should be the editor, always present whenever you need to input or edit text.

## The Core Problem It Solves

1. **Mobile typing is slow and tedious** — Most people struggle to type quickly on glass screens without looking.
2. **Mobile editing is nearly impossible** — There's no good way to edit text on a phone without switching to a separate editor app.
3. **8pen is gone** — The original inspiration was pulled from app stores, leaving users without the keyboard they loved.

## How It Works

### The Radial Gesture Model
The keyboard displays a circle divided into 4 sectors (TOP, BOTTOM, LEFT, RIGHT). Each sector acts as both a navigation region and the start of a character gesture:

- **INSIDE_CIRCLE** → the user's finger is inside the central circle
- **Sector positions** → TOP, LEFT, BOTTOM, RIGHT
- A **MovementSequence** is the list of `FingerPosition` values recorded as the finger moves across the keyboard

A character is produced by completing a specific movement sequence, e.g.:
`[INSIDE_CIRCLE, TOP, RIGHT, INSIDE_CIRCLE]` → maps to a specific character action.

### Keyboard Modes / Views
| View | Purpose |
|------|---------|
| **MainKeyboardView** | Primary gesture input — types characters |
| **NumberKeypadView** | Numeric input (auto-switches for number fields) |
| **SelectionKeypadView** | Text selection and cursor movement |
| **ClipboardKeypadView** | Clipboard history management |
| **SymbolKeypadView** | Special symbols and punctuation |

### Sector Shortcuts (Single-Touch)
- **Right sector** → Backspace
- **Bottom sector** → Enter
- **Top sector** → Shift / Caps Lock toggle (once = Shift, twice = Caps Lock, thrice = Off)
- **Left sector** → Switch to Number Pad

### Layer System
8VIM supports **6 visible layers** (FIRST through SIXTH) plus a HIDDEN layer:
- **Layer 1 (FIRST)**: Default characters (primary alphabet)
- **Layer 2–6**: Accessed via a specific rotation/sequence prefix
- Each layer extends the available character set without the user needing separate modes

### Customizable Layouts
- Layouts are defined in **YAML files** (parsed via Jackson)
- Layouts are cached in **CBOR format** for performance
- Users can load custom layouts from files
- Built-in embedded layouts (e.g., `EmbeddedLayout("en")`)
- JSON schema validation ensures layout file correctness

## User Experience Goals

1. **Speed** — Users should reach 40+ WPM with practice; gestures should feel fluid
2. **Blind typing** — No need to look at the keyboard once the layout is memorized
3. **Natural gestures** — Inspired by handwriting on a constrained canvas
4. **Editor-grade control** — Selection, copy/paste, cursor movement all accessible from the keyboard
5. **Deep customization** — Colors, trail visibility, circle position, layout files, sidebar placement
6. **Privacy** — Password fields are detected; no text logging

## Target Users
- Power users who want maximum mobile typing efficiency
- Former 8pen users looking for an open-source replacement
- Vim enthusiasts who want editing capabilities system-wide
- Developers and contributors who want a hackable, open keyboard
