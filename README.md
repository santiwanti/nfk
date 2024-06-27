# NFK

## Note

Don't use this library yet. This is still pre-alpha and the final API hasn't been define yet.

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

### iOS Setup

The steps are detailed in [the docs](https://developer.apple.com/documentation/corenfc/building_an_nfc_tag-reader_app#3240401):
- [ ] Turn on Near Field Communication Tag Reading which will: 
  - [ ] Add the NFC tag-reading feature to the App ID.
  - [ ] Add the Near Field Communication Tag Reader Session Formats Entitlement.
- [ ] Add the NFCReaderUsageDescription to `Info.plist`

## Supported Features

|  Feature   | Android | iOS |
|:----------:|:-------:|:---:|
| Read Tags  |    ✓    |  ✓  |
| Write Tags |         |     |

✓: Supported

✓✓: Supported in background

