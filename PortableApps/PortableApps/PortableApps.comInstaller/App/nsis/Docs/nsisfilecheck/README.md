# NSIS FileCheck [![Build status](https://github.com/past-due/nsisfilecheck/workflows/CI/badge.svg)](https://github.com/past-due/nsisfilecheck/actions?query=workflow%3ACI+branch%3Amaster) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![NSIS: 3.0+](https://img.shields.io/badge/NSIS-3.0%2B-orange.svg)](https://en.wikipedia.org/wiki/Nullsoft_Scriptable_Install_System)
[NSIS (Nullsoft Scriptable Install System)](https://en.wikipedia.org/wiki/Nullsoft_Scriptable_Install_System) plugin that enables:
- [Calculating a file's hash (SHA1, SHA2)](#calcfilehash)
- [Verifying a file's Authenticode code signature (including details)](#verifyfilesignature)
- [Obtaining a file's string version info](#getfileversioninfostring)

### Supports:
- **Windows**: Windows XP -> Windows 10
- **NSIS**: 3.0+ (ANSI or Unicode)

### General Compatibility Notes:
The resulting `filecheck.dll`:
- Does **not** have a dependency on the CRT, and should run on systems that do not yet have the VCRedist / CRT installed.
- Dynamically loads all libraries except `kernel32.dll` and `user32.dll`, and handles differing OS / patch-level support of the underlying Windows APIs used automatically.

# Usage

## calcFileHash

`filecheck::calcFileHash local_file ALGORITHM`

If successful, this call returns the `ALGORITHM` hash of the contents of the file `local_file` as a hex-encoded string; otherwise, it returns an error description string.

- **ALGORITHM**
  - Must be one of: `sha1`, `sha256`, `sha384`, `sha512`

> Note: SHA-2 algorithms (`sha256`, `sha384`, `sha512`) require Windows XP SP3+.

### Examples:

- Calculate a file's SHA-256 hash
```NSIS
filecheck::calcFileHash "path_to_file" sha256
Pop $R0 ; Get the return value
```

## verifyFileSignature

`filecheck::verifyFileSignature local_file [/ROOT microsoft] [/CERTNAME NAME] [/CERTISSUERNAME ISSUERNAME]`

This call returns "OK" if the file's Authenticode signature is valid (and passes any additional checks); otherwise, it returns an error description string.

- **/ROOT microsoft**
  - Specify the requirement for a particular root. The only supported value is `microsoft` which checks for a Microsoft root certificate.
- **/CERTNAME**
  - Check that the first valid signature is associated with a certificate with name NAME.
- **/CERTISSUERNAME**
  - Check that the first valid signature is associated with a certificate with _issuer_ name ISSUERNAME.

### Examples:

- Check for any valid code signature
> NOTE: This simply checks that the file has a valid code signature. It does *not* perform any additional validation on _what_ code signature it has. **You should not use this as the only check for file authenticity**, or any file with a valid code signature could be substituted and pass the check.
```NSIS
filecheck::verifyFileSignature "path_to_file"
Pop $R0 ; Get the return value
${If} $R0 == "OK"
  ; Verification succeeded
${Else}
  MessageBox MB_OK|MB_ICONSTOP "Code signature verification failed: $R0"
${EndIf}
```

- Check for a valid Microsoft code-signature
```NSIS
filecheck::verifyFileSignature "path_to_file" /ROOT "microsoft" /CERTNAME "Microsoft Corporation" /CERTISSUERNAME "Microsoft Code Signing PCA"
Pop $R0 ; Get the return value
${If} $R0 == "OK"
  ; Verification succeeded
${Else}
  MessageBox MB_OK|MB_ICONSTOP "Code signature verification failed: $R0"
${EndIf}
```

## getFileVersionInfoString

`filecheck::getFileVersionInfoString local_file STRINGNAME [/LANGUAGE LANGNUM=1033] [/CODEPAGE CODEPAGENUM=1252]`

This call returns returns the string info value corresponding to `STRINGNAME` in `local_file`'s version information (specifically, at the path: `\StringFileInfo\LANGNUM-CODEPAGENUM\STRINGNAME`). If there is an error, it returns an error description string.

### Examples:

- Get the FileDescription from a file's version info
```NSIS
filecheck::getFileVersionInfoString "path_to_file" "FileDescription" /LANGUAGE 1033 /CODEPAGE 1252
Pop $R0 ; Get the return value
```

# Security Tips

### Avoid SHA-1 if possible

> [Since 2005 SHA-1 has not been considered secure against well-funded opponents, and since 2010 many organizations have recommended its replacement by SHA-2 or SHA-3.](https://en.wikipedia.org/wiki/SHA-1)

This plugin supports SHA-2 on Windows XP SP3 and above. For almost all cases, there is zero reason to use SHA-1.

### Avoid TOCTOU

[Time of check to time of use (TOCTOU / TOCTTOU)](https://en.wikipedia.org/wiki/Time_of_check_to_time_of_use) bugs can lead to security vulnerabilities.

Do not assume that a file that has been checked has not been modified between the time of the check and the time of the use. Use proper security permissions on any containing / temporary folders to ensure that nothing unprivileged can modify a file between a check and any use.

# Development

### Compilation Requirements:
- Visual Studio 2017-2019
- CMake 3.5+ (3.15+ recommended)
