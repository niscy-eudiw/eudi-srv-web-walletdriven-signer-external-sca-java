# Changelog

## [0.4.0]

_X Jan 2026_

### Added:
- Prometheus metrics endpoint for enhanced service monitoring.
- Unit tests to verify that SCA endpoints correctly validate incoming requests.

### Changed:
- Enhanced Swagger documentation for better API clarity.
- Improve exceptions to improve traceability. 
- Upgraded logging for more detailed diagnostic information. 
- Update Maven dependencies versions.
- Updated Docker Java and Maven images version.

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

