# Image creation

This file explains how to create an image which can be used to
reprogram a sensor. We will use the thermometer image from this
project (which can be found at [1]) as example.

The file is in TI-TXT format and comprises three segment. The TI-TXT
format as well as most technical details touched upon here are
explained in depth in the TI documentation. Note that all segments are
aligned to 8 byte blocks as used by the underlying NFC commands.

[1] android/app/src/main/assets/thermometer-payload.txt

## Layout

The first segment at address 0xf998 writes the signature 0x8001 used
to recognize sensors reprogrammed to act as a thermometer.

The middle segment at address 0xfda0 contains the actual code to
execute. This is taken from the output of the Code Composer Studio
(see next section).

The last segment at address 0xffa0 rewrites the RF custom command
dispatch table. It consists of multiple 4-byte entries where the first
two bytes encode the address of the function handling the command and
the last two bytes encode the custom command name (both in little
endian). So if we want the custom command C4 to be handled by a
function starting at address 0xfdb8 the corresponding entry is
"B8 FD C4 00".

## Code Generation

The Code Composer Studio project generates a TI-TXT file as part of
the build step. It contains a handler for a NFC custom command (which
is by default AA). Locate its entry in the NFC dispatch table (of the
form "yy zz AA 00"). The target code is then at address zzyy and
terminated by a return which is encoded by the bytes "30 41". This is
the code to copy into the middle segment of the preceding section.
