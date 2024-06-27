# Comments

This document contains notes for contributors. It includes things like what has been tried, but
didn't work so that we avoid trying the same things in the future.

## Removing `expect` from the `NfcCard` `sealed class`

This doesn't work because on Android we need the `Tag` to be able to read and write to the tag.
Therefore we need to keep a reference to the `Tag` or to the Android specific NFC implementations.

## Using `interface`s such as `NfcWriter` and `NfcReader`

Due to the previous limitation there is no point in having these `interface`s. The `actual`
implementations can have the correct `read` and `write` functions for each `NfcCard`.
