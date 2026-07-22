/*
    This file is part of JavaAppLauncher.

    JavaAppLauncher is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    JavaAppLauncher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JavaAppLauncher; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

// Shared, product-generic macOS "Open with" launcher stub for RouteConverter
// and TimeAlbumPro. Built as a universal (x86_64 + arm64) Cocoa app; installed
// as the bundle's Contents/MacOS/<AppName> executable, replacing the former
// shell-script launcher that could not receive a kAEOpenDocuments ("Open
// with") Apple Event on a cold launch (see issue #206 / forum thread 4139).
//
// As an NSApplicationDelegate this stub catches -application:openFiles: (the
// Cocoa entry point for a launch-triggered "Open with"/double-click), which
// macOS delivers BEFORE -applicationDidFinishLaunching:. Once launch has
// finished (deferred one runloop tick so any queued openFiles: has drained),
// it execs the bundled JRE with the collected file paths appended as plain
// argv -- the path BaseRouteConverter.parseInitialArgs already understands --
// rather than trying to forward the odoc into the JVM's own AppKit/Desktop
// machinery, which does not come up in time for a cold launch.
//
// After execv, java IS the app (same PID); a subsequent *warm* "Open with"
// bypasses this stub entirely and is delivered straight to the running JVM,
// handled by RouteConverter's existing java.awt.Desktop open-file handler
// (ApplicationMenu#openFiles). No IPC, no resident stub process.
//
// No product name is hard-coded here: the JRE, jar, icon and dock name are
// all resolved from the enclosing .app bundle at runtime, so the same source
// serves both RouteConverter.app and TimeAlbumPro.app (see the mac.xml/pom.xml
// wiring in RouteConverterMac and TimeAlbumProMac).

#import <Cocoa/Cocoa.h>
#include <errno.h>
#include <mach-o/dyld.h>
#include <string.h>
#include <unistd.h>

@interface JavaAppLauncherDelegate : NSObject <NSApplicationDelegate>
@property(nonatomic, strong) NSMutableArray<NSString *> *pendingFilePaths;
@end

@implementation JavaAppLauncherDelegate

- (instancetype)init {
    self = [super init];
    if (self) {
        _pendingFilePaths = [NSMutableArray array];
    }
    return self;
}

// Cold-launch "Open with" / Finder double-click / `open -a App file` delivers
// the odoc here, before -applicationDidFinishLaunching:. Just buffer it.
- (void)application:(NSApplication *)sender openFiles:(NSArray<NSString *> *)filenames {
    [self.pendingFilePaths addObjectsFromArray:filenames];
    [sender replyToOpenOrPrint:NSApplicationDelegateReplySuccess];
}

// Legacy single-file variant of the same event; kept for older callers.
- (BOOL)application:(NSApplication *)sender openFile:(NSString *)filename {
    if (filename.length > 0) {
        [self.pendingFilePaths addObject:filename];
    }
    return YES;
}

- (void)applicationDidFinishLaunching:(NSNotification *)notification {
    // Defer to the next runloop turn so any -application:openFiles: that
    // AppKit queued for this launch is guaranteed to have been delivered
    // and drained into pendingFilePaths before we read it.
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchBundledJavaApplication];
    });
}

// Contents/ of the enclosing .app, resolved generically so this stub carries
// no product-specific paths.
- (NSString *)resolveContentsDirectory {
    NSString *bundlePath = [[NSBundle mainBundle] bundlePath];
    if (bundlePath.length > 0) {
        return [bundlePath stringByAppendingPathComponent:@"Contents"];
    }

    // Fallback if NSBundle lookup ever comes back empty: derive Contents/
    // from the running executable's own path, which is Contents/MacOS/<exe>.
    char executablePathBuffer[PATH_MAX];
    uint32_t bufferSize = sizeof(executablePathBuffer);
    if (_NSGetExecutablePath(executablePathBuffer, &bufferSize) != 0) {
        NSLog(@"JavaAppLauncher: cannot resolve bundle Contents/ directory");
        return nil;
    }
    NSString *executablePath = [NSString stringWithUTF8String:executablePathBuffer];
    NSString *macOSDirectory = [executablePath stringByDeletingLastPathComponent];
    return [macOSDirectory stringByDeletingLastPathComponent];
}

// Best-effort: clears the download quarantine flag from the whole bundle
// before exec'ing the bundled JRE. The bundled JRE (Contents/bin/java + its
// dylibs) is Developer-ID signed but not itself notarized; on Apple Silicon a
// quarantined, un-notarized binary exec'd as a child process is SIGKILLed by
// the kernel even after the user approves the Gatekeeper prompt for the outer
// .app -- that approval does not cover bin/java. Mirrors the former shell
// launcher's `xattr -dr com.apple.quarantine`.
- (void)removeQuarantineAttributeFromBundleAtPath:(NSString *)bundleRootPath {
    NSTask *xattrTask = [[NSTask alloc] init];
    xattrTask.launchPath = @"/usr/bin/xattr";
    xattrTask.arguments = @[ @"-dr", @"com.apple.quarantine", bundleRootPath ];
    xattrTask.standardOutput = [NSFileHandle fileHandleWithNullDevice];
    xattrTask.standardError = [NSFileHandle fileHandleWithNullDevice];
    @try {
        [xattrTask launch];
        [xattrTask waitUntilExit];
    } @catch (NSException *exception) {
        // Best-effort only, e.g. no write access to the bundle; a no-op once
        // already cleared. Fall through and try to launch regardless.
    }
}

// Returns the single file with the given extension in directoryPath, or nil
// (logging) if there is not exactly one match.
- (NSString *)findExactlyOneFileWithExtension:(NSString *)extension inDirectory:(NSString *)directoryPath {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error = nil;
    NSArray<NSString *> *entries = [fileManager contentsOfDirectoryAtPath:directoryPath error:&error];
    if (entries == nil) {
        NSLog(@"JavaAppLauncher: cannot list %@: %@", directoryPath, error);
        return nil;
    }

    NSMutableArray<NSString *> *matches = [NSMutableArray array];
    for (NSString *entry in entries) {
        if ([entry.pathExtension isEqualToString:extension]) {
            [matches addObject:entry];
        }
    }

    if (matches.count != 1) {
        NSLog(@"JavaAppLauncher: expected exactly one *.%@ in %@, found %lu", extension, directoryPath,
              (unsigned long)matches.count);
        return nil;
    }
    return [directoryPath stringByAppendingPathComponent:matches[0]];
}

- (void)launchBundledJavaApplication {
    NSString *contentsDirectory = [self resolveContentsDirectory];
    if (contentsDirectory.length == 0) {
        exit(EXIT_FAILURE);
    }
    NSString *bundleRootDirectory = [contentsDirectory stringByDeletingLastPathComponent];

    [self removeQuarantineAttributeFromBundleAtPath:bundleRootDirectory];

    NSString *javaExecutablePath = [contentsDirectory stringByAppendingPathComponent:@"bin/java"];
    NSString *javaDirectory = [contentsDirectory stringByAppendingPathComponent:@"Java"];
    NSString *resourcesDirectory = [contentsDirectory stringByAppendingPathComponent:@"Resources"];

    // The jar is essential to start the app at all -- assert exactly one.
    NSString *jarPath = [self findExactlyOneFileWithExtension:@"jar" inDirectory:javaDirectory];
    if (jarPath.length == 0) {
        NSLog(@"JavaAppLauncher: no single *.jar in %@, cannot launch", javaDirectory);
        exit(EXIT_FAILURE);
    }
    // The dock icon is cosmetic -- degrade gracefully if it cannot be found.
    NSString *icnsPath = [self findExactlyOneFileWithExtension:@"icns" inDirectory:resourcesDirectory];

    NSString *dockName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
    if (dockName.length == 0) {
        dockName = bundleRootDirectory.lastPathComponent.stringByDeletingPathExtension;
    }

    NSMutableArray<NSString *> *javaArguments = [NSMutableArray array];
    [javaArguments addObject:javaExecutablePath];
    [javaArguments addObject:[NSString stringWithFormat:@"-Xdock:name=%@", dockName]];
    if (icnsPath.length > 0) {
        [javaArguments addObject:[NSString stringWithFormat:@"-Xdock:icon=%@", icnsPath]];
    }
    [javaArguments addObject:@"-Xmx1024m"];
    [javaArguments addObject:@"-Drouteconverter.bundledJre=true"];
    [javaArguments addObject:@"-jar"];
    [javaArguments addObject:jarPath];
    [javaArguments addObjectsFromArray:self.pendingFilePaths];

    NSUInteger argumentCount = javaArguments.count;
    char **execArguments = calloc(argumentCount + 1, sizeof(char *));
    for (NSUInteger index = 0; index < argumentCount; index++) {
        execArguments[index] = strdup(javaArguments[index].UTF8String);
    }
    execArguments[argumentCount] = NULL;

    // Replace this stub's process image with the JVM, same PID: after this,
    // a warm "Open with" is delivered straight to the running application.
    execv(javaExecutablePath.UTF8String, execArguments);

    // execv only returns on failure.
    NSLog(@"JavaAppLauncher: execv of %@ failed: %s", javaExecutablePath, strerror(errno));
    exit(EXIT_FAILURE);
}

@end

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        NSApplication *application = [NSApplication sharedApplication];
        JavaAppLauncherDelegate *delegate = [[JavaAppLauncherDelegate alloc] init];
        application.delegate = delegate;
        [application run];
    }
    return 0;
}
