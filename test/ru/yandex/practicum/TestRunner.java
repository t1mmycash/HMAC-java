package ru.yandex.practicum;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.lang.annotation.Annotation;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== HMAC Service Test Runner ===\n");

        List<Class<?>> testClasses = getTestClasses();

        if (testClasses.isEmpty()) {
            System.out.println("❌ No test classes found!");
            System.out.println("Make sure tests are compiled to: target/test-classes/");
            System.exit(1);
        }

        int totalTests = 0;
        int totalPassed = 0;
        int totalFailed = 0;

        for (Class<?> testClass : testClasses) {
            TestClassResult result = runTestClass(testClass);

            totalTests += result.totalTests;
            totalPassed += result.passed;
            totalFailed += result.failed;
        }

        printSummary(totalTests, totalPassed, totalFailed);

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static List<Class<?>> getTestClasses() {
        List<Class<?>> testClasses = new ArrayList<>();
        String[] classNames = {
                "ru.yandex.practicum.ModelTest",
                "ru.yandex.practicum.CodecTest",
                "ru.yandex.practicum.SecureComparatorTest",
                "ru.yandex.practicum.ConfigTest",
                "ru.yandex.practicum.HmacServiceTest"
        };

        for (String className : classNames) {
            try {
                Class<?> testClass = Class.forName(className);
                testClasses.add(testClass);
                System.out.println("✓ Loaded: " + testClass.getSimpleName());
            } catch (ClassNotFoundException e) {
                System.out.println("✗ Not found: " + getSimpleClassName(className) + " (skipping)");
            }
        }

        System.out.println();
        return testClasses;
    }

    private static String getSimpleClassName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private static TestClassResult runTestClass(Class<?> testClass) {
        System.out.println("Running tests in: " + testClass.getSimpleName());

        Method beforeEach = findLifecycleMethod(testClass,
                org.junit.jupiter.api.BeforeEach.class);
        Method afterEach = findLifecycleMethod(testClass,
                org.junit.jupiter.api.AfterEach.class);

        List<Method> testMethods = findTestMethods(testClass);

        if (testMethods.isEmpty()) {
            System.out.println("  No test methods found!");
            return new TestClassResult(0, 0, 0);
        }

        int classPassed = 0;
        int classFailed = 0;

        for (Method testMethod : testMethods) {
            boolean passed = runSingleTest(testClass, testMethod, beforeEach, afterEach);

            if (passed) {
                classPassed++;
            } else {
                classFailed++;
            }
        }

        System.out.println("  " + testClass.getSimpleName() +
                ": " + classPassed + " passed, " + classFailed + " failed\n");

        return new TestClassResult(testMethods.size(), classPassed, classFailed);
    }

    private static boolean runSingleTest(Class<?> testClass, Method testMethod,
                                         Method beforeEach, Method afterEach) {
        String testName = getTestDisplayName(testMethod);
        Object testInstance = null;

        try {
            testInstance = testClass.getDeclaredConstructor().newInstance();

            if (beforeEach != null) {
                invokeLifecycleMethod(beforeEach, testInstance);
            }

            System.out.print("  " + testName + "... ");
            testMethod.invoke(testInstance);

            System.out.println("✅ PASS");
            return true;

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            System.out.println("❌ FAIL");

            if (cause != null) {
                String errorMsg = cause.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = cause.getClass().getSimpleName();
                }
                System.out.println("    Error: " + errorMsg);

                if (cause instanceof org.opentest4j.AssertionFailedError assertionError) {
                    if (assertionError.getExpected() != null && assertionError.getActual() != null) {
                        System.out.println("    Expected: " + assertionError.getExpected());
                        System.out.println("    Actual:   " + assertionError.getActual());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ FAIL");
            System.out.println("    Infrastructure error: " + e.getMessage());

        } finally {
            if (afterEach != null && testInstance != null) {
                try {
                    afterEach.invoke(testInstance);
                } catch (Exception e) {
                    System.out.println("    Warning: @AfterEach failed: " + e.getMessage());
                }
            }
        }

        return false;
    }

    private static Method findLifecycleMethod(Class<?> testClass,
                                              Class<? extends Annotation> annotationClass) {
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static List<Method> findTestMethods(Class<?> testClass) {
        List<Method> testMethods = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                method.setAccessible(true);
                testMethods.add(method);
            }
        }
        return testMethods;
    }

    private static String getTestDisplayName(Method testMethod) {
        org.junit.jupiter.api.DisplayName displayName =
                testMethod.getAnnotation(org.junit.jupiter.api.DisplayName.class);
        return displayName != null ? displayName.value() : testMethod.getName();
    }

    private static void invokeLifecycleMethod(Method method, Object instance) {
        try {
            method.invoke(instance);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String methodType = method.getAnnotation(org.junit.jupiter.api.BeforeEach.class) != null
                    ? "BeforeEach" : "AfterEach";
            System.err.println("    Error in @" + methodType + " method '" +
                    method.getName() + "': " + cause.getMessage());
        }
    }

    private static void printSummary(int totalTests, int passed, int failed) {
        System.out.println("=== Test Summary ===");
        System.out.println("Total tests: " + totalTests);
        System.out.println("Passed:      " + passed);
        System.out.println("Failed:      " + failed);

        double percentage = totalTests > 0 ? (passed * 100.0) / totalTests : 0;
        System.out.printf("Success rate: %.1f%%\n", percentage);

        if (failed == 0) {
            System.out.println("\n🎉 ALL TESTS PASSED!");
        } else {
            System.out.println("\n❌ SOME TESTS FAILED");
        }
    }

    private static final class TestClassResult {
        public final int totalTests;
        public final int passed;
        public final int failed;

        public TestClassResult(int totalTests, int passed, int failed) {
            this.totalTests = totalTests;
            this.passed = passed;
            this.failed = failed;
        }
    }
}
