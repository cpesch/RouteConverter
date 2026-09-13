#!/usr/bin/env python3
"""Assert the macOS deployment target (``minos``) of every slice of a Mach-O.

macOS refuses to ``exec`` a Mach-O whose ``LC_BUILD_VERSION.minos`` is higher
than the running OS, and the Finder then shows the app with a prohibition badge
on a greyed icon -- no dialog, no log. ``clang`` defaults that floor to the host
SDK, so a launcher stub compiled without ``-mmacosx-version-min`` silently
inherits whatever the build runner happens to be: RouteConverter 3.6 shipped a
stub with ``minos 26.0`` and could not start on macOS 15 or earlier (#393).

``vtool``/``otool`` would answer this in one line, but the ``.app`` bundles are
assembled on a Linux runner where neither exists. Hence this parser: standard
library only, no third-party imports, runs anywhere Python 3 runs.

Usage::

    check-macho-minos.py <mach-o file> <arch>=<max-minos> [<arch>=<max-minos> ...]

Every named arch must be present in the file, and every slice's floor must be
at or below the ceiling given for its arch. Slices whose arch is not named are
reported but not checked. Exits 0 and prints ``arch=minos`` per slice on
success, non-zero with a one-line reason otherwise.
"""

from __future__ import annotations
import struct
import sys
from pathlib import Path

FAT_MAGIC = 0xCAFEBABE      # big-endian 32-bit fat header
FAT_MAGIC_64 = 0xCAFEBABF   # big-endian 64-bit fat header (fat_arch_64)
MH_MAGIC = 0xFEEDFACE       # thin 32-bit, host-endian
MH_CIGAM = 0xCEFAEDFE       # thin 32-bit, byte-swapped
MH_MAGIC_64 = 0xFEEDFACF    # thin 64-bit, host-endian
MH_CIGAM_64 = 0xCFFAEDFE    # thin 64-bit, byte-swapped

LC_VERSION_MIN_MACOSX = 0x24
LC_BUILD_VERSION = 0x32

# cputype (+ the masked cpusubtype where it distinguishes a slice we ship)
CPU_TYPE_X86_64 = 0x01000007
CPU_TYPE_ARM64 = 0x0100000C
CPU_TYPE_NAMES = {
    CPU_TYPE_X86_64: 'x86_64',
    CPU_TYPE_ARM64: 'arm64',
    0x00000007: 'i386',
    0x0000000C: 'arm',
}


class MachOError(Exception):
    """The file is not a Mach-O we can read, or contradicts itself."""


def _arch_name(cputype: int) -> str:
    return CPU_TYPE_NAMES.get(cputype, 'cputype-0x%08x' % cputype)


def _parse_version(packed: int) -> tuple[int, int, int]:
    """Decode an ``xxxx.yy.zz`` version word into (major, minor, patch)."""
    return (packed >> 16) & 0xFFFF, (packed >> 8) & 0xFF, packed & 0xFF


def _format_version(version: tuple[int, int, int]) -> str:
    major, minor, patch = version
    return '%d.%d.%d' % version if patch else '%d.%d' % (major, minor)


def parse_version_string(text: str) -> tuple[int, int, int]:
    """Parse a ``11``, ``11.0`` or ``10.13.4`` ceiling into a comparable tuple."""
    parts = text.split('.')
    if not 1 <= len(parts) <= 3:
        raise ValueError('not a macOS version: %r' % text)
    numbers = []
    for part in parts:
        if not part.isdigit():
            raise ValueError('not a macOS version: %r' % text)
        numbers.append(int(part))
    while len(numbers) < 3:
        numbers.append(0)
    return numbers[0], numbers[1], numbers[2]


def _slice_minos(data: bytes, offset: int) -> tuple[str, tuple[int, int, int] | None]:
    """Return (arch name, minos) for the Mach-O header at ``offset``.

    ``minos`` is None when the slice carries neither LC_BUILD_VERSION nor
    LC_VERSION_MIN_MACOSX -- the caller decides whether that is fatal.
    """
    if offset + 32 > len(data):
        raise MachOError('Mach-O header at offset %d is past the end of the file' % offset)

    magic, = struct.unpack_from('>I', data, offset)
    if magic in (MH_MAGIC, MH_MAGIC_64):
        endian = '>'
    elif magic in (MH_CIGAM, MH_CIGAM_64):
        endian = '<'
    else:
        raise MachOError('no Mach-O magic at offset %d (found 0x%08x)' % (offset, magic))
    wide = magic in (MH_MAGIC_64, MH_CIGAM_64)

    cputype, _cpusubtype, _filetype, ncmds, _sizeofcmds, _flags = struct.unpack_from(
        endian + 'iiIIII', data, offset + 4)
    # mach_header is 28 bytes, mach_header_64 adds a 4-byte reserved field
    command_offset = offset + (32 if wide else 28)

    minos = None
    for _ in range(ncmds):
        if command_offset + 8 > len(data):
            raise MachOError('load commands of the slice at offset %d run past the end of the file' % offset)
        cmd, cmdsize = struct.unpack_from(endian + 'II', data, command_offset)
        if cmdsize < 8:
            raise MachOError('load command at offset %d declares size %d' % (command_offset, cmdsize))
        if cmd == LC_BUILD_VERSION and cmdsize >= 24:
            # struct build_version_command: cmd, cmdsize, platform, minos, sdk, ntools
            _platform, packed_minos = struct.unpack_from(endian + 'II', data, command_offset + 8)
            minos = _parse_version(packed_minos)
            break  # LC_BUILD_VERSION is authoritative; stop looking
        if cmd == LC_VERSION_MIN_MACOSX and cmdsize >= 16 and minos is None:
            # struct version_min_command: cmd, cmdsize, version, sdk
            packed_minos, = struct.unpack_from(endian + 'I', data, command_offset + 8)
            minos = _parse_version(packed_minos)
        command_offset += cmdsize

    return _arch_name(cputype), minos


def read_slices(path: Path) -> list[tuple[str, tuple[int, int, int] | None]]:
    """Return (arch name, minos) for every slice of a thin or fat Mach-O."""
    data = path.read_bytes()
    if len(data) < 8:
        raise MachOError('%s is too short to be a Mach-O' % path)

    magic, = struct.unpack_from('>I', data, 0)
    if magic not in (FAT_MAGIC, FAT_MAGIC_64):
        return [_slice_minos(data, 0)]

    # A fat header and its fat_arch entries are always big-endian.
    nfat_arch, = struct.unpack_from('>I', data, 4)
    entry_format = '>iiQQI' if magic == FAT_MAGIC_64 else '>iiIII'
    entry_size = struct.calcsize(entry_format)
    slices = []
    for index in range(nfat_arch):
        entry_offset = 8 + index * entry_size
        if entry_offset + entry_size > len(data):
            raise MachOError('fat header declares %d slices but the file holds fewer' % nfat_arch)
        _cputype, _cpusubtype, slice_offset, _size, _align = struct.unpack_from(
            entry_format, data, entry_offset)
        # Trust the slice's own header over the fat entry's cputype: one file, one truth.
        slices.append(_slice_minos(data, slice_offset))
    if not slices:
        raise MachOError('fat header declares no slices')
    return slices


def main(argv: list[str]) -> int:
    if len(argv) < 3:
        sys.stderr.write(__doc__.split('Usage::')[1].strip() + '\n')
        return 2

    path = Path(argv[1])
    ceilings: dict[str, tuple[int, int, int]] = {}
    for argument in argv[2:]:
        if '=' not in argument:
            sys.stderr.write('error: expected <arch>=<max-minos>, got %r\n' % argument)
            return 2
        arch, _, ceiling = argument.partition('=')
        try:
            ceilings[arch] = parse_version_string(ceiling)
        except ValueError as error:
            sys.stderr.write('error: %s\n' % error)
            return 2

    try:
        slices = read_slices(path)
    except MachOError as error:
        sys.stderr.write('error: %s\n' % error)
        return 1
    except OSError as error:
        sys.stderr.write('error: cannot read %s: %s\n' % (path, error))
        return 1

    failures = []
    seen = set()
    reported = []
    for arch, minos in slices:
        seen.add(arch)
        if minos is None:
            reported.append('  %s=(none)' % arch)
            failures.append('%s: slice carries neither LC_BUILD_VERSION nor LC_VERSION_MIN_MACOSX' % arch)
            continue
        reported.append('  %s=%s' % (arch, _format_version(minos)))
        ceiling = ceilings.get(arch)
        if ceiling is not None and minos > ceiling:
            failures.append('%s: minos %s exceeds the allowed %s, so macOS %s users cannot launch it'
                            % (arch, _format_version(minos), _format_version(ceiling), _format_version(ceiling)))

    for arch in sorted(ceilings):
        if arch not in seen:
            failures.append('%s: no such slice (the file holds %s)' % (arch, ', '.join(sorted(seen))))

    print('%s:' % path)
    for line in reported:
        print(line)
    sys.stdout.flush()

    for failure in failures:
        sys.stderr.write('error: %s: %s\n' % (path, failure))
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
