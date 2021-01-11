# Astal-PCMx

—Plays P16 audio files from Astal for the Sega Saturn.

*This project is on hold.*

## Downloads

* [[**Latest Release**](https://github.com/ed7n/astal-pcmx/raw/master/release/astalpcmx.jar)] — Update 0 Revision 0, 01/17/2019.

## Usage

`java -jar astalpcmx.jar [loop] <file path>`

## Building

    $ javac -d release --release 8 --source-path src src/eden/astalpcmx/Main.java && jar -c -f release/astalpcmx.jar -e eden.astalpcmx.Main -C release eden

## About

Astal is a 2D platformer. I wanted to extract the audio out of the game.

### Streamed Audio

Despite being CD-quality--to my surprise--the game does not utilize CDDA.
Instead, they are instead contained in .P16 files. Each frame consists of 2048
samples for each channel.

### Buffered Audio

These are .SND files. They aren't interesting enough to be part of the project
yet. Maybe next time.
