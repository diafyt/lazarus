# diafyt Lazarus

Diafyt Lazarus is a project to repurpose expired Libre Freestyle CGM
sensors (e.g. into thermometers).

## Contents

This repository contains the open source parts of diafyt Lazarus. It
comprises the following parts (organized by subdirectory):

- `android/` Android application.
- `embedded/` the code for programming the hardware.
- `doc/` further documentation.
- `ios/` not yet available, will contain the iOS application.

## Contributions

Please use the facilities provided at
https://github.com/diafyt/lazarus for bug reports and patch
submission.

## Building

The Android application is provided as an Android Studio project which
can be opened and built with Android Studio (it provides Kotlin
support out of the box).

The embedded component is provided as a Code Composer Studio project
which can be opened and built with Code Composer Studio (which needs
to be installed with support for the MSP430).

## Format

The hardware exposes 244 blocks (of 8 bytes each) of memory via NFC
(in terms of internal pointer addreses 0xf860 through 0xffff). The
last four blocks are used for interrupt handlers and should not be
modified. The first three blocks are used by diafyt Lazarus for an
integrity check and should also not be modified.

Furthermore diafyt Lazarus uses the fifth and sixth byte of block 39
(in terms of internal pointer addreses 0xf99c and 0xf99d) to store in
little endian a signature to recognize reprogrammed sensors. The
following values are recognized:

- 0 to 0x7fff: sensor which was not reprogrammed
- 0x8001: reprogrammed as thermometer by this app
- 0x8002 to 0xffff: reprogrammed sensor with custom program

