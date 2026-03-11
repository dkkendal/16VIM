# Project Brief: 8VIM

## Overview
8VIM is an Android keyboard (Input Method Editor / IME) application inspired by the now-discontinued **8pen** keyboard and the **Vim** text editor. It implements a radial gesture-based input mechanism where the user draws strokes through sectors of a circular layout to produce characters.

## Core Mission
Create an **editor-in-keyboard** for Android that enables:
- Blind, fast, and accurate typing through fluid gestures
- Vim-like editing capabilities available system-wide (not just inside a specific app)
- A typing experience that is natural, efficient, and reduces cognitive load on mobile devices

## Key Goals
1. Implement the 8pen-style radial gesture keyboard for character input
2. Support Vim-inspired editing modes (selection, clipboard, symbols, number pad)
3. Allow user-customizable keyboard layouts via YAML files
4. Achieve 40+ words-per-minute typing speed for proficient users
5. Be open-source and available on F-Droid and Google Play

## Scope
- **Platform**: Android (minSdk 24, targetSdk 34)
- **Package ID**: `inc.flide.vi8`
- **Current Version**: 0.17.0
- **Distribution**: F-Droid (primary), Google Play (stable + beta)

## Project Identity
- **GitHub**: https://github.com/8VIM/8VIM (origin: git@github.com:flide/8VIM.git)
- **Language**: Kotlin (primary), with some Java
- **Build System**: Gradle (Kotlin DSL)
