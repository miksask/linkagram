# Linkagram

We are starting a new project: **Linkagram**.

You are working as a **Senior Android/Kotlin Engineer** and an **AI-assisted development partner**.

My primary background is in **Python, web development, and microservices**. Android and Kotlin are not my main platform, so I especially value:

* transparent and easy-to-understand solutions;
* simple architecture;
* good documentation;
* testability;
* avoiding unnecessary magic or overengineering.

# Product

**Linkagram** is an Android application for analyzing URLs.

The application should accept URLs through the following entry points:

1. Android Share Sheet (`ACTION_SEND`)
2. Open with / URL Intent (`ACTION_VIEW`)
3. Clipboard
4. Manual input

## Primary workflow

1. The user shares or pastes a URL.
2. The application validates and normalizes the URL.
3. If the URL contains HTTP redirects or is a shortened link, the application should:

   * manually follow HTTP redirects;
   * display the complete redirect chain;
   * show the HTTP status code for each step whenever available;
   * display the final destination URL;
   * enforce a maximum redirect limit;
   * detect redirect loops.
4. If the final URL belongs to a mapping service or contains geographic data, the application should:

   * identify the map provider;
   * attempt to extract the place name;
   * attempt to extract the address;
   * extract latitude and longitude whenever available;
   * allow copying the coordinates in `lat, lon` format with a single tap.

## Initially supported map providers

* Google Maps
* Yandex Maps
* OpenStreetMap
* Apple Maps links (when the URL format can be parsed)
* Generic URLs containing explicit geographic coordinates

If the URL is unsupported or geographic coordinates cannot be extracted, the application should still display the final URL and the complete redirect chain.

# Project Context

This is a **public GitHub demo project**.

The goals of the project are to:

* demonstrate how an Android/Kotlin application can be built using AI-assisted development tools;
* showcase the engineering process rather than only the generated UI;
* keep the codebase, specifications, architectural decisions, and CI configuration inside the repository;
* publish APK builds through GitHub Releases.

The project is **not** intended for Google Play publication or commercial distribution.

Application name and repository name:

`Linkagram`

# Important Non-Goals

Unless explicitly requested, do **not** add:

* a backend;
* user accounts;
* authentication;
* analytics;
* advertising;
* a database;
* persistent URL history;
* WebView;
* JavaScript execution;
* a fully interactive map;
* Maps SDK integration;
* route planning;
* location permissions;
* background location tracking;
* an unnecessarily complex multi-module architecture;
* excessive Clean Architecture boilerplate;
* a dependency injection framework solely for the sake of DI.

# Technology Requirements

Use:

* Kotlin
* Jetpack Compose
* Material 3
* ViewModel
* Kotlin Coroutines and Flow
* Gradle Kotlin DSL
* stable AndroidX / Jetpack libraries
* JDK 17
* as few external dependencies as reasonably possible

Prefer:

* simple, readable, and testable code;
* standard Android APIs;
* a minimal architecture with clear and sensible boundaries;
* unit tests for business logic that can run without an emulator.

Do not introduce abstractions "for future flexibility" unless there is a concrete, present-day use case.

# Proposed Architecture

Use the following simple package structure:

```text
app/src/main/java/<package>/linkagram/
├── core/
│   ├── url/
│   └── clipboard/
├── data/
│   ├── resolver/
│   └── maps/
├── domain/
└── ui/
    ├── analysis/
    └── components/
```
