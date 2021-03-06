/*
 * Copyright (C) 2017 team-cachebox.de
 *
 * Licensed under the : GNU General  License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.libPP;


import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.jnigen.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Created by Longri on 18.12.2017.
 */
public class LibPPBuild {

    public static void main(String[] args) throws Exception {

        CommandLine cmd = getCommandLine(args);


        FileDescriptor targetDescriptor = new FileDescriptor("libs");
        FileDescriptor jniDescriptor = new FileDescriptor("jni");
        FileDescriptor buildDescriptor = new FileDescriptor("../LibPP/build/classes/main");
        FileDescriptor rectpack2D_src = new FileDescriptor("rectpack2D_src");

        File jniPath = jniDescriptor.file().getAbsoluteFile();
        String jniPathString = jniPath.getAbsolutePath();


        File libPPPath = rectpack2D_src.file().getAbsoluteFile();
        String libPPPathString = libPPPath.getAbsolutePath();

        File buildPath = buildDescriptor.file().getAbsoluteFile();
        String buildPathString = buildPath.getAbsolutePath();


        //cleanup
        jniDescriptor.deleteDirectory();
//        targetDescriptor.deleteDirectory();


        String cFlags = "";


        String[] headers = new String[]{libPPPathString};

        // generate native code
        new NativeCodeGenerator().generate("../LibPP/src", buildPathString, jniPathString);


        //copy c/c++ src to 'jni' folder
        for (String headerPath : headers) {
            FileDescriptor fd = new FileDescriptor(headerPath);
            FileDescriptor[] list = fd.list();
            for (FileDescriptor descriptor : list) {
                descriptor.copyTo(jniDescriptor.child(descriptor.name()));
            }
        }

        //generate build scripts
        boolean all = cmd.hasOption("all");
        Array<BuildTarget> targets = new Array<>();


        if (all || cmd.hasOption("win64")) {
            BuildTarget win64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Windows, true);
            win64.compilerSuffix = ".exe";
//            win64.headerDirs = headers;
            win64.cFlags += cFlags;
            win64.cppFlags += " -std=c++11";
            targets.add(win64);
        }

        if (all || cmd.hasOption("win32")) {
            BuildTarget win32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Windows, false);
            win32.compilerPrefix = "";
            win32.compilerSuffix = "";
            win32.headerDirs = headers;
            win32.cFlags += cFlags;
            targets.add(win32);
        }

        BuildTarget mac64 = null;
        if (all || cmd.hasOption("mac64")) {
            mac64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.MacOsX, true);
            mac64.compilerPrefix = "";
            mac64.compilerSuffix = "";
            mac64.headerDirs = headers;
            mac64.cFlags += cFlags;
            targets.add(mac64);
        }


        if (all || cmd.hasOption("mac32")) {
            BuildTarget mac32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.MacOsX, false);
            mac32.compilerPrefix = "";
            mac32.compilerSuffix = "";
            mac32.headerDirs = headers;
            mac32.cFlags += cFlags;
            targets.add(mac32);
        }


        if (all || cmd.hasOption("ios32")) {
            BuildTarget ios32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.IOS, false);
            ios32.compilerPrefix = "";
            ios32.compilerSuffix = "";
            ios32.headerDirs = headers;
            ios32.cppFlags += " -stdlib=libc++";
            targets.add(ios32);
        }

        if (all || cmd.hasOption("linux32")) {
            BuildTarget linux32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Linux, false);
            linux32.compilerPrefix = "";
            linux32.compilerSuffix = "";
            linux32.headerDirs = headers;
//            linux32.linkerFlags="-shared -m32 -z execstack";
            linux32.cFlags += cFlags;
            targets.add(linux32);
        }

        if (all || cmd.hasOption("linux64")) {
            BuildTarget linux64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Linux, true);
            linux64.headerDirs = headers;
            linux64.linkerFlags = "-shared -m64 -z noexecstack";
            linux64.cFlags += cFlags;
            linux64.cExcludes = new String[]{"shell.c"};
            targets.add(linux64);
        }

        if (all || cmd.hasOption("android")) {
            BuildTarget android = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Android, false);
            android.headerDirs = headers;
            android.cFlags += cFlags;

            if (System.getProperty("os.name").startsWith("Windows")) {
                android.ndkHome = "C:/android-ndk-r16b";
                android.ndkSuffix = ".cmd";
            } else {
                android.ndkHome = "/Volumes/HDD_DATA/android-ndk-r16b";
            }

            targets.add(android);
        }


        BuildConfig config = new BuildConfig("LibPP");
        new AntScriptGenerator().generate(config, targets);

        FileDescriptor projectPath = new FileDescriptor("../");
        FileDescriptor buildLibsPath = projectPath.child("LibPPBuild/libs");


        //delete outdated files
        buildLibsPath.deleteDirectory();


        if (all || cmd.hasOption("linux32"))
            BuildExecutor.executeAnt("build-linux32.xml", "-v", jniPath);
        if (all || cmd.hasOption("linux64"))
            BuildExecutor.executeAnt("build-linux64.xml", "-v", jniPath);
        if (all || cmd.hasOption("win32")) BuildExecutor.executeAnt("build-windows32.xml", "-v", jniPath);
        if (all || cmd.hasOption("win64")) BuildExecutor.executeAnt("build-windows64.xml", "-v", jniPath);
        if (all || cmd.hasOption("mac64")) BuildExecutor.executeAnt("build-macosx64.xml", "-v", jniPath);
        if (all || cmd.hasOption("mac32")) BuildExecutor.executeAnt("build-macosx32.xml", "-v", jniPath);
        if (all || cmd.hasOption("ios32")) {
            BuildExecutor.executeAnt("build-ios32.xml", "-v", jniPath);
        }
        if (all || cmd.hasOption("android")) BuildExecutor.executeAnt("build-android32.xml", "-v", jniPath);

        syncPrecopmiledLibs();


        BuildExecutor.executeAnt("build.xml", "-v", jniPath);


        //##############################################
        // Test native SQLite
        //##############################################
        runTest();


        //copy libs to local modules


        FileDescriptor java = projectPath.child("LibPP/build/libs/LibPP-1.0.jar");

        FileDescriptor core = projectPath.child("core");
        FileDescriptor coreJar = projectPath.child("LibPP/build/libs");
        coreJar.copyTo(core);

        FileDescriptor desktop = projectPath.child("desktop/libs/");
        FileDescriptor test = projectPath.child("LibPP/testNatives/");
        FileDescriptor desktopNative = projectPath.child("LibPPBuild/libs/LibPP-platform-1.0-natives-desktop.jar");


        desktop.mkdirs();
        test.mkdirs();
        desktopNative.copyTo(desktop);
        desktopNative.copyTo(test);

        FileDescriptor androidNative_arm64 = projectPath.child("LibPPBuild/libs/arm64-v8a/libLibPP.so");
        FileDescriptor androidNative_arm = projectPath.child("LibPPBuild/libs/armeabi/libLibPP.so");
        FileDescriptor androidNative_armv7 = projectPath.child("LibPPBuild/libs/armeabi-v7a/libLibPP.so");
        FileDescriptor androidNative_x86 = projectPath.child("LibPPBuild/libs/x86/libLibPP.so");
        FileDescriptor androidNative_x86_64 = projectPath.child("LibPPBuild/libs/x86_64/libLibPP.so");

        FileDescriptor androidLibs = projectPath.child("android/libs/");

        try {
            androidLibs.mkdirs();
            androidNative_arm64.copyTo(androidLibs.child("arm64-v8a/libLibPP.so"));
            androidNative_arm.copyTo(androidLibs.child("armeabi/libLibPP.so"));
            androidNative_armv7.copyTo(androidLibs.child("armeabi-v7a/libLibPP.so"));
            androidNative_x86.copyTo(androidLibs.child("x86/libLibPP.so"));
            androidNative_x86_64.copyTo(androidLibs.child("x86_64/libLibPP.so"));
            java.copyTo(androidLibs.child(java.name()));
        } catch (Exception e) {

        }


        //copy to iOS
        try {
            FileDescriptor iOSLibs = projectPath.child("ios/libs/");

            FileDescriptor iOSNative = projectPath.child("LibPPBuild/libs/ios32/libLibPP.a");
            iOSLibs.mkdirs();
            java.copyTo(iOSLibs.child(java.name()));
            iOSNative.copyTo(iOSLibs.child(iOSNative.name()));
        } catch (Exception e) {

        }

    }

    private static void syncPrecopmiledLibs() {
        FileDescriptor libsPath = new FileDescriptor("../LibPPBuild/libs/");
        FileDescriptor precompiledLibsPath = new FileDescriptor("../LibPPBuild/precompiledLibs/");
        sync(libsPath, precompiledLibsPath, "windows64");
        sync(libsPath, precompiledLibsPath, "armeabi");
        sync(libsPath, precompiledLibsPath, "armeabi-v7a");
        sync(libsPath, precompiledLibsPath, "arm64-v8a");
        sync(libsPath, precompiledLibsPath, "ios32");
        sync(libsPath, precompiledLibsPath, "linux32");
        sync(libsPath, precompiledLibsPath, "linux64");
        sync(libsPath, precompiledLibsPath, "macosx32");
        sync(libsPath, precompiledLibsPath, "macosx64");
        sync(libsPath, precompiledLibsPath, "x86");
        sync(libsPath, precompiledLibsPath, "x86_64");
    }

    private static void sync(FileDescriptor libsPath, FileDescriptor precompiledLibsPath, String folder) {
        FileDescriptor lib = libsPath.child(folder);
        FileDescriptor pre = precompiledLibsPath.child(folder);

        if (folderExistAndNotEmpty(lib)) {
            //copy to precompiled!
            lib.copyTo(precompiledLibsPath);
            System.out.println("New    compiled :" + folder);
        } else {
            //get from precompiled if exist
            if (pre.exists()) {
                pre.copyTo(libsPath);
                System.out.println("Use precompiled :" + folder);
            }
        }
    }

    private static boolean folderExistAndNotEmpty(FileDescriptor folder) {

        if (!folder.exists() || !folder.isDirectory()) return false;
        return (folder.list().length > 0);
    }

    private static void runTest() {

        int randomTestRecCount = 20;
        int maxTexSize = 600;


        //delete alt test folder
        FileHandle clear = new FileHandle("test");
        clear.deleteDirectory();
        new JniGenSharedLibraryLoader("libs/LibPP-platform-1.0-natives-desktop.jar").load("LibPP");
        NativePacker.libLoaded = true;
        short[] valueArray = new short[randomTestRecCount * 7];


        // fill random test array
        int index = 0;
        for (int i = 0; i < randomTestRecCount; i++) {
            valueArray[index + 0] = (short) i; // index
            valueArray[index + 1] = 0; // x
            valueArray[index + 2] = 0; // y
            valueArray[index + 3] = (short) MathUtils.random(100, 300);// width
            valueArray[index + 4] = (short) MathUtils.random(100, 300); // height
            valueArray[index + 5] = 0;// flipped
            valueArray[index + 6] = 0;
            index += 7;
        }

        int[] result = NativePacker.packNative(valueArray, valueArray.length / 7,
                maxTexSize, false, true);


        //wait for c++ printf
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("#################################################################");

        int idx = 1;
        for (int i = 0; i < result[0]; i++) {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Page[").append(i).append("] w=").append(result[idx++]).append(" h=").append(result[idx++]);

            System.out.println(stringBuilder.toString());
            index = 0;
            for (int j = 0; j < randomTestRecCount; j++) {
                if (valueArray[index + 6] == i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("    ");
                    sb.append("rec index=").append(valueArray[index + 0]);
                    sb.append(" x=").append(valueArray[index + 1]);
                    sb.append(" y=").append(valueArray[index + 2]);
                    sb.append(" width=").append(valueArray[index + 3]);
                    sb.append(" height=").append(valueArray[index + 4]);
                    sb.append(" flipped=").append((valueArray[index + 5] > 0));
                    System.out.println(sb.toString());
                }
                index += 7;
            }
        }

    }

    private static String arrayToString(Object[] items) {

        if (items == null) return "NULL";

        if (items.length == 0) return "[]";
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < items.length; i++) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }


    private static CommandLine getCommandLine(String[] args) {
        Options options = new Options();


        Option all = new Option("a", "all", false, "compile for all platforms");
        all.setRequired(false);
        options.addOption(all);

        Option mac64 = new Option(null, "mac64", false, "compile for mac 64 bit");
        mac64.setRequired(false);
        options.addOption(mac64);

        Option mac32 = new Option(null, "mac32", false, "compile for mac 32 bit");
        mac32.setRequired(false);
        options.addOption(mac32);

        Option linux32 = new Option(null, "linux32", false, "compile for linux 32 bit");
        linux32.setRequired(false);
        options.addOption(linux32);

        Option linux64 = new Option(null, "linux64", false, "compile for linux 64 bit");
        linux64.setRequired(false);
        options.addOption(linux64);

        Option win32 = new Option(null, "win32", false, "compile for windows 32 bit");
        win32.setRequired(false);
        options.addOption(win32);

        Option win64 = new Option(null, "win64", false, "compile for windows 64 bit");
        win64.setRequired(false);
        options.addOption(win64);

        Option ios32 = new Option(null, "ios32", false, "compile for iOs");
        ios32.setRequired(false);
        options.addOption(ios32);

        Option android = new Option(null, "android", false, "compile for Android");
        android.setRequired(false);
        options.addOption(android);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SQLite native builder", options);

            System.exit(1);
            return null;
        }
        return cmd;
    }

}
