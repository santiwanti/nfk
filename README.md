# NFK

## What is it?

Kotlin Multiplatform library for NFC communication.

## How To Use

### Android Setup

- [ ] Make sure NFC is enabled. The library does not handle that.
- [ ] If your app does not require NFC override the `uses-feature`. This library assumes NFC is
  required.
- [ ] If your app needs to detect nfc cards in the background use `intent-filter`s. Take a look
  at [the documentation](https://developer.android.com/develop/connectivity/nfc/nfc#filter-intents)
  for more information.

## Supported Features

|  Feature   | Android | iOS |
|:----------:|:-------:|:---:|
| Read Tags  |    ✓    |  ✓  |
| Write Tags |         |     |

✓: Supported

✓✓: Supported in background

