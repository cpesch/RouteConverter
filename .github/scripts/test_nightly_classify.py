import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nightly_classify  # noqa: E402
from nightly_model import JobResult  # noqa: E402


def surefire_xml(classname, method, exc_type, message, frames):
    frame_lines = "\n".join(f"\tat {f}.someMethod({f.split('.')[-1]}.java:1)" for f in frames)
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<testsuite><testcase classname="{classname}" name="{method}" time="0.1">'
        f'<failure message="{message}" type="{exc_type}">{frame_lines}</failure>'
        "</testcase></testsuite>"
    )


class ParseSurefireReportsTest(unittest.TestCase):
    def test_single_failure_with_frames(self):
        xml = surefire_xml(
            "slash.navigation.gpx.GpxFormatTest",
            "testRead",
            "java.lang.AssertionError",
            "boom",
            ["slash.navigation.gpx.GpxFormat", "slash.navigation.base.BaseRoute"],
        )
        failures = nightly_classify.parse_surefire_reports([xml])
        self.assertEqual(len(failures), 1)
        failure = failures[0]
        self.assertEqual(failure.test_class, "slash.navigation.gpx.GpxFormatTest")
        self.assertEqual(failure.method, "testRead")
        self.assertEqual(failure.exception, "java.lang.AssertionError")
        self.assertEqual(
            failure.frames,
            ("slash.navigation.gpx.GpxFormat", "slash.navigation.base.BaseRoute"),
        )

    def test_error_without_type_uses_first_message_token(self):
        xml = (
            '<?xml version="1.0"?>\n'
            '<testsuite><testcase classname="slash.navigation.NmeaTest" name="testWrite">'
            '<error message="NullPointerException thrown here">at slash.navigation.Nmea.write(Nmea.java:5)</error>'
            "</testcase></testsuite>"
        )
        failures = nightly_classify.parse_surefire_reports([xml])
        self.assertEqual(len(failures), 1)
        self.assertEqual(failures[0].exception, "NullPointerException")

    def test_malformed_xml_is_skipped(self):
        failures = nightly_classify.parse_surefire_reports(["<testsuite><testcase"])
        self.assertEqual(failures, [])


class IsInfraTest(unittest.TestCase):
    def test_failed_setup_step_is_infra(self):
        job = JobResult(name="Java 21", conclusion="failure", failed_step="Set up JDK", log="")
        self.assertTrue(nightly_classify.is_infra(job))

    def test_connection_reset_log_is_infra(self):
        job = JobResult(
            name="Java 21",
            conclusion="failure",
            failed_step="Build with Maven",
            log="Error: Connection reset by peer",
        )
        self.assertTrue(nightly_classify.is_infra(job))

    def test_plain_test_failure_in_maven_step_is_not_infra(self):
        job = JobResult(
            name="Java 21",
            conclusion="failure",
            failed_step="Build with Maven",
            log="[ERROR] Tests run: 1, Failures: 1",
        )
        self.assertFalse(nightly_classify.is_infra(job))


class MavenErrorTest(unittest.TestCase):
    def test_dependency_resolution_failure_without_plugin_goal(self):
        log = (
            "[ERROR] Failed to execute goal on project rest: Could not resolve dependencies for project "
            "slash.navigation:rest:jar:3.8-SNAPSHOT\n"
            "[ERROR] dependency: slash.navigation:common:jar:tests:3.8-SNAPSHOT (test)"
        )
        module, goal, _ = nightly_classify.maven_error(log)
        self.assertEqual(module, "rest")
        self.assertEqual(goal, "dependency-resolution")

    def test_other_failure_without_plugin_goal(self):
        module, goal, _ = nightly_classify.maven_error("[ERROR] Failed to execute goal on project gui: something else")
        self.assertEqual((module, goal), ("gui", "build"))

    def test_extracts_module_and_goal(self):
        log = (
            "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test "
            "(default-test) on project navigation-formats: There are test failures."
        )
        module, goal, error_line = nightly_classify.maven_error(log)
        self.assertEqual(module, "navigation-formats")
        self.assertEqual(goal, "test")
        self.assertIsNotNone(error_line)

    def test_normalisation_is_stable_across_path_line_and_hash(self):
        log_a = (
            "2026-10-11T03:15:22.1234567Z [ERROR] /home/runner/work/RC/RC/navigation-formats/"
            "src/test/java/Foo.java:42: build failed at commit abc1234def"
        )
        log_b = (
            "2026-10-11T04:01:09.7654321Z [ERROR] /home/runner/work/RC/RC/navigation-formats/"
            "src/test/java/Bar.java:99: build failed at commit 9988776fe"
        )
        _, _, error_line_a = nightly_classify.maven_error(log_a)
        _, _, error_line_b = nightly_classify.maven_error(log_b)
        self.assertEqual(error_line_a, error_line_b)


class SignatureTest(unittest.TestCase):
    def test_same_signature_across_jdk_and_os_job_names(self):
        tests = (
            nightly_classify.parse_surefire_reports(
                [
                    surefire_xml(
                        "slash.navigation.gpx.GpxFormatTest",
                        "testRead",
                        "java.lang.AssertionError",
                        "boom",
                        [],
                    )
                ]
            )
        )
        sig_linux = nightly_classify.signature(tests, None, None, None)
        sig_windows = nightly_classify.signature(tests, None, None, None)
        self.assertEqual(sig_linux, sig_windows)

    def test_differs_by_method_within_same_class_and_exception(self):
        failure_a = nightly_classify.parse_surefire_reports(
            [
                surefire_xml(
                    "slash.navigation.gpx.GpxFormatTest",
                    "testRead",
                    "java.lang.AssertionError",
                    "boom",
                    [],
                )
            ]
        )
        failure_b = nightly_classify.parse_surefire_reports(
            [
                surefire_xml(
                    "slash.navigation.gpx.GpxFormatTest",
                    "testWrite",
                    "java.lang.AssertionError",
                    "boom",
                    [],
                )
            ]
        )
        sig_a = nightly_classify.signature(tuple(failure_a), None, None, None)
        sig_b = nightly_classify.signature(tuple(failure_b), None, None, None)
        self.assertNotEqual(sig_a, sig_b)

    def test_unchanged_when_second_class_sorts_after_first(self):
        first_class_failure = nightly_classify.parse_surefire_reports(
            [
                surefire_xml(
                    "slash.navigation.AaaTest",
                    "testOne",
                    "java.lang.AssertionError",
                    "boom",
                    [],
                )
            ]
        )
        second_class_failure = nightly_classify.parse_surefire_reports(
            [
                surefire_xml(
                    "slash.navigation.ZzzTest",
                    "testTwo",
                    "java.lang.RuntimeException",
                    "kaboom",
                    [],
                )
            ]
        )
        only_first = tuple(first_class_failure)
        both = tuple(first_class_failure) + tuple(second_class_failure)
        self.assertEqual(
            nightly_classify.signature(only_first, None, None, None),
            nightly_classify.signature(both, None, None, None),
        )


class ExcerptTest(unittest.TestCase):
    def test_sanitises_backticks_and_at_and_caps_lines(self):
        lines = [f"line {i}" for i in range(100)]
        lines[50] = "[ERROR] ```fenced``` @someone failed"
        log = "\n".join(lines)
        result = nightly_classify.excerpt(log, max_lines=10)
        self.assertNotIn("```", result)
        self.assertIn("'''", result)
        self.assertNotIn("@someone", result)
        self.assertIn("@​someone", result)
        self.assertLessEqual(len(result.splitlines()), 10)


class ClassifyTest(unittest.TestCase):
    def test_flaky_when_rerun_succeeds(self):
        first = [JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log="[ERROR] boom")]
        rerun = [JobResult(name="Java 21", conclusion="success", failed_step=None, log="")]
        findings = nightly_classify.classify("build", first, rerun, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "flaky")

    def test_deterministic_when_rerun_fails_again_without_infra_signal(self):
        log = "[ERROR] Tests run: 1, Failures: 1"
        first = [JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log)]
        rerun = [JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log)]
        findings = nightly_classify.classify("build", first, rerun, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "deterministic")

    def test_infra_when_rerun_fails_with_infra_signal(self):
        first = [JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log="[ERROR] boom")]
        rerun = [
            JobResult(
                name="Java 21",
                conclusion="failure",
                failed_step="Build with Maven",
                log="Connection reset by peer",
            )
        ]
        findings = nightly_classify.classify("build", first, rerun, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "infra")

    def test_infra_when_rerun_is_none(self):
        first = [
            JobResult(
                name="Java 21",
                conclusion="failure",
                failed_step="Build with Maven",
                log="Connection reset by peer",
            )
        ]
        findings = nightly_classify.classify("build", first, None, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "infra")

    def test_deterministic_when_rerun_is_none_without_infra_signal(self):
        first = [JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log="[ERROR] boom")]
        findings = nightly_classify.classify("build", first, None, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "deterministic")

    def test_forward_compat_for_jdk_ea_canary(self):
        first = [JobResult(name="Java 25-ea", conclusion="failure", failed_step="Build with Maven", log="[ERROR] boom")]
        findings = nightly_classify.classify("jdk-ea-canary", first, None, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].klass, "forward_compat")

    def test_cancelled_job_from_fail_fast_is_not_a_flaky_finding(self):
        log = "[ERROR] Tests run: 1, Failures: 1"
        first = [
            JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log),
            JobResult(name="Java 25", conclusion="cancelled", failed_step="Build with Maven", log=""),
        ]
        rerun = [
            JobResult(name="Java 21", conclusion="success", failed_step=None, log=""),
            JobResult(name="Java 25", conclusion="success", failed_step=None, log=""),
        ]
        findings = nightly_classify.classify("build", first, rerun, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].jobs, ("Java 21",))
        self.assertEqual(findings[0].klass, "flaky")

    def test_cancelled_job_with_log_content_is_still_triaged(self):
        log = "[ERROR] Tests run: 1, Failures: 1"
        first = [
            JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log),
            JobResult(name="Java 25", conclusion="cancelled", failed_step="Build with Maven", log=log),
        ]
        rerun = [
            JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log),
            JobResult(name="Java 25", conclusion="failure", failed_step="Build with Maven", log=log),
        ]
        findings = nightly_classify.classify("build", first, rerun, {})
        names = {name for finding in findings for name in finding.jobs}
        self.assertIn("Java 25", names)

    def test_two_jobs_with_same_signature_merge(self):
        log = "[ERROR] Tests run: 1, Failures: 1"
        first = [
            JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log),
            JobResult(name="Java 21 on Windows", conclusion="failure", failed_step="Build with Maven", log=log),
        ]
        rerun = [
            JobResult(name="Java 21", conclusion="failure", failed_step="Build with Maven", log=log),
            JobResult(name="Java 21 on Windows", conclusion="failure", failed_step="Build with Maven", log=log),
        ]
        findings = nightly_classify.classify("build", first, rerun, {})
        self.assertEqual(len(findings), 1)
        self.assertEqual(set(findings[0].jobs), {"Java 21", "Java 21 on Windows"})


if __name__ == "__main__":
    unittest.main()
