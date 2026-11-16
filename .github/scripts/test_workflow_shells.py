#!/usr/bin/env python3
"""Scan .github/workflows/*.yml for a Windows job step missing shell:.

Windows runners default to pwsh. A `run:` step written for bash (or any
non-default shell) that forgets `shell:` silently runs under pwsh instead
and fails in confusing ways (GitHub #489: `msg=...` parsed as a pwsh
assignment, not a bash one, broke the 3.8 release). Fixed by hand for that
one step in #365; this test generalises the check so the next bash step
added to a Windows job cannot fail the same way.

Python 3 stdlib only (no PyYAML) -- the runner's system Python must not
need packages. The scan is line/indentation based, not a real YAML parser:
it assumes the 2-space block-style indentation used throughout
.github/workflows/*.yml.

Rule: inside a job whose `runs-on:` contains "windows", every step with a
`run:` key must have a `shell:` key in the same step, unless the job (or
the file's top-level `defaults:`) sets `defaults: run: shell:`.
"""

import glob
import os
import re
import unittest

KEY_RE = re.compile(r'^([A-Za-z0-9_.\-]+):(\s*(.*))?$')


def _indent(line):
    return len(line) - len(line.lstrip(' '))


def _is_blank_or_comment(line):
    stripped = line.strip()
    return stripped == '' or stripped.startswith('#')


def _block_end(lines, start, indent):
    """First index at or after `start` whose indentation is <= `indent`,
    skipping blank/comment lines when measuring."""
    i = start
    n = len(lines)
    while i < n:
        line = lines[i]
        if not _is_blank_or_comment(line) and _indent(line) <= indent:
            return i
        i += 1
    return n


def iter_mapping_keys(lines, start, end, indent, first_line_override=None):
    """Yield (key, inline_value, line_index, content_start, content_end)
    for mapping keys sitting at exactly `indent` within [start, end).

    `first_line_override` lets a sequence item's '- key: value' line be
    parsed as if the leading '- ' were stripped, without touching the
    underlying file.
    """
    i = start
    n = end
    while i < n:
        raw_line = first_line_override if (i == start and first_line_override is not None) else lines[i]
        if _is_blank_or_comment(raw_line):
            i += 1
            continue
        ind = _indent(raw_line)
        if ind < indent:
            break
        if ind != indent:
            i += 1
            continue
        m = KEY_RE.match(raw_line[ind:])
        if not m:
            i += 1
            continue
        key = m.group(1)
        value = (m.group(3) or '').strip()
        content_start = i + 1
        content_end = _block_end(lines, content_start, indent)
        yield key, value, i, content_start, content_end
        i = content_end


def iter_seq_items(lines, start, end, indent):
    """Yield (item_start, item_end) for '- ' sequence items at `indent`."""
    i = start
    n = end
    while i < n:
        line = lines[i]
        if _is_blank_or_comment(line):
            i += 1
            continue
        ind = _indent(line)
        if ind < indent:
            break
        if ind == indent and line[ind:ind + 2] == '- ':
            item_end = _block_end(lines, i + 1, indent)
            yield i, item_end
            i = item_end
        else:
            i += 1


def _default_shell(lines, start, end, indent):
    """Find `defaults: run: shell:` among the direct children of a block
    that itself sits at `indent` (i.e. look for `defaults:` at `indent`)."""
    for key, _value, _li, cs, ce in iter_mapping_keys(lines, start, end, indent):
        if key != 'defaults':
            continue
        for key2, _v2, _li2, cs2, ce2 in iter_mapping_keys(lines, cs, ce, indent + 2):
            if key2 != 'run':
                continue
            for key3, value3, _li3, _cs3, _ce3 in iter_mapping_keys(lines, cs2, ce2, indent + 4):
                if key3 == 'shell':
                    return value3
    return None


def find_violations(text, filename='<workflow>'):
    """Return a list of human-readable violation strings for `text`."""
    lines = text.splitlines()
    n = len(lines)
    violations = []

    file_default_shell = _default_shell(lines, 0, n, 0)

    jobs_range = None
    for key, _value, _li, cs, ce in iter_mapping_keys(lines, 0, n, 0):
        if key == 'jobs':
            jobs_range = (cs, ce)
            break
    if jobs_range is None:
        return violations
    jobs_start, jobs_end = jobs_range

    for job_name, _jv, _jli, job_start, job_end in iter_mapping_keys(lines, jobs_start, jobs_end, 2):
        job_keys = {}
        for key, value, li, cs, ce in iter_mapping_keys(lines, job_start, job_end, 4):
            job_keys[key] = (value, li, cs, ce)

        if 'runs-on' not in job_keys:
            continue
        runs_on_value = job_keys['runs-on'][0]
        if 'windows' not in runs_on_value.lower():
            continue

        job_default_shell = _default_shell(lines, job_start, job_end, 4)
        effective_default = job_default_shell or file_default_shell

        if 'steps' not in job_keys:
            continue
        _steps_value, _steps_li, steps_start, steps_end = job_keys['steps']

        for item_start, item_end in iter_seq_items(lines, steps_start, steps_end, 6):
            dash_line = lines[item_start]
            override = dash_line[:6] + '  ' + dash_line[8:]
            step_keys = {}
            for key, value, li, cs, ce in iter_mapping_keys(
                lines, item_start, item_end, 8, first_line_override=override
            ):
                step_keys[key] = (value, li, cs, ce)

            if 'run' not in step_keys:
                continue
            if 'shell' in step_keys:
                continue
            if effective_default:
                continue

            step_name = step_keys['name'][0] if 'name' in step_keys else '<unnamed step>'
            violations.append(
                '{}: job {}: step "{}" runs a script without shell: '
                '(Windows defaults to pwsh)'.format(filename, job_name, step_name)
            )

    return violations


def _workflow_files():
    here = os.path.dirname(os.path.abspath(__file__))
    workflows_dir = os.path.normpath(os.path.join(here, '..', 'workflows'))
    return sorted(glob.glob(os.path.join(workflows_dir, '*.yml')))


class WindowsStepShellTest(unittest.TestCase):
    def test_step_without_shell_is_reported(self):
        text = (
            'name: sample\n'
            'on: [push]\n'
            'jobs:\n'
            '  build:\n'
            '    runs-on: windows-latest\n'
            '    steps:\n'
            '      - uses: actions/checkout@v4\n'
            '      - name: Run script\n'
            '        run: echo hello\n'
        )
        violations = find_violations(text, filename='sample.yml')
        self.assertEqual(
            violations,
            ['sample.yml: job build: step "Run script" runs a script without '
             'shell: (Windows defaults to pwsh)'],
        )

    def test_job_default_shell_satisfies_every_step(self):
        text = (
            'name: sample\n'
            'on: [push]\n'
            'jobs:\n'
            '  build:\n'
            '    runs-on: windows-latest\n'
            '    defaults:\n'
            '      run:\n'
            '        shell: bash\n'
            '    steps:\n'
            '      - uses: actions/checkout@v4\n'
            '      - name: Run script\n'
            '        run: echo hello\n'
            '      - name: Other shell\n'
            '        shell: cmd\n'
            '        run: echo hi\n'
        )
        self.assertEqual(find_violations(text, filename='sample.yml'), [])

    def test_file_default_shell_satisfies_every_job(self):
        text = (
            'name: sample\n'
            'on: [push]\n'
            'defaults:\n'
            '  run:\n'
            '    shell: bash\n'
            'jobs:\n'
            '  build:\n'
            '    runs-on: windows-latest\n'
            '    steps:\n'
            '      - name: Run script\n'
            '        run: echo hello\n'
        )
        self.assertEqual(find_violations(text, filename='sample.yml'), [])

    def test_non_windows_job_is_ignored(self):
        text = (
            'name: sample\n'
            'on: [push]\n'
            'jobs:\n'
            '  build:\n'
            '    runs-on: ubuntu-latest\n'
            '    steps:\n'
            '      - name: Run script\n'
            '        run: echo hello\n'
        )
        self.assertEqual(find_violations(text, filename='sample.yml'), [])

    def test_step_shell_overrides_defaults_not_needed(self):
        text = (
            'name: sample\n'
            'on: [push]\n'
            'jobs:\n'
            '  build:\n'
            '    runs-on: windows-latest\n'
            '    steps:\n'
            '      - name: Run script\n'
            '        shell: pwsh\n'
            '        run: echo hello\n'
        )
        self.assertEqual(find_violations(text, filename='sample.yml'), [])

    def test_every_workflow_file_passes(self):
        files = _workflow_files()
        self.assertTrue(files, 'no workflow files found to scan')
        all_violations = []
        for path in files:
            with open(path, encoding='utf-8') as f:
                text = f.read()
            all_violations.extend(find_violations(text, filename=os.path.basename(path)))
        self.assertEqual(all_violations, [])


if __name__ == '__main__':
    unittest.main()
