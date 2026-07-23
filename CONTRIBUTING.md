# Contributing to RouteConverter

Thanks for your interest in RouteConverter! Contributions are very welcome —
bug fixes, new formats, UI improvements, and translations all help.

## Reporting bugs / requesting features

Please use the issue forms when you [open a new issue](https://github.com/cpesch/RouteConverter/issues/new/choose):
one for bug reports, one for feature requests. For general usage questions,
please ask on the [forum](https://forum.routeconverter.com/) instead of
opening an issue.

## Pull requests

1. Fork the repository, create a branch, and open a PR against `master`.
2. Keep your diff small and focused, and match the surrounding code style.
3. Add tests for your change.
4. A human maintainer reviews and merges every PR — no auto-merge.
5. Keep the CI matrix green (Java 21 + 25, plus the Windows smoke build).
6. Never commit secrets.

## Project conventions

See [`AGENTS.md`](AGENTS.md) for the module layout, build/test commands, the
GPL header rule, the JAXB test-object convention, and the IntelliJ GUI
Designer `.form` round-trip.

## Build & IDE setup

Don't duplicate the commands here — see the README's
["Build & run from source"](README.md#build--run-from-source) and
["IDE setup"](README.md#ide-setup-intellij-idea) sections.

## Translations

Translations go through [Weblate](https://hosted.weblate.org/projects/routeconverter/).
Never hand-edit `RouteConverter_*.properties` directly — code PRs may only
touch the `_en`/`_de` bundles (see `AGENTS.md`).

## License

By contributing, you agree your code ships under the **GNU GPL v2** (see
[`LICENSE-GPL.txt`](LICENSE-GPL.txt)). Contributors are listed in
[`CONTRIBUTORS.txt`](CONTRIBUTORS.txt).
