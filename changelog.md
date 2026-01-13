# Changelog

## [0.4.0]

_X Jan 2026_

### Fixed:
- Fixed an issue where CAdES signed documents were incorrectly labeled with the *application/pdf* MIME type instead of a CAdES-appropriate MIME type.

## [0.3.0]

_28 May 2025_

### Added:
- Visible representation of the PDF signature.
- Docker support for the application:
  - Added Dockerfile to build the application image.
  - Added docker-compose.yml.
  - Instructions for building and running the container added to README.md.

### Fixed:
- Remove unused code.
- Bugs found during Signature Parameter combinations testing.

## [0.2.0]

_29 Nov 2024_

### Added:
- Endpoint to calculate the digest of data to be signed
- Endpoint to obtain the signed document
- Support to obtaining signature types: PAdES, CAdES, XAdES, JAdES
- Support for Baseline signatures: -B, -T, -LT, -LTA

