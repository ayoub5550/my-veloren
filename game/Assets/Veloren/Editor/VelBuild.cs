// Batch entry points: Unity -batchmode -quit -projectPath game -executeMethod MyVeloren.EditorTools.VelBuild.<Compile|BuildLinux|BuildAndroid>
// Env: VEL_VERSION_CODE, VEL_OUT, VEL_KEYSTORE / VEL_KEYSTORE_PASS / VEL_KEY_ALIAS (signing key kept OUTSIDE the repo).
// SPDX-License-Identifier: GPL-3.0-or-later
using System;
using System.IO;
using System.Security.Cryptography;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;

namespace MyVeloren.EditorTools
{
    public static class VelBuild
    {
        const string Scene = "Assets/Scenes/Main.unity";
        const string PackageId = "com.ayoub.myveloren";

        static string Version() => File.Exists("VERSION") ? File.ReadAllText("VERSION").Trim() : "0.1.0-dev.0";

        public static void Configure()
        {
            Directory.CreateDirectory("Assets/Scenes");
            if (!File.Exists(Scene))
            {
                var sc = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
                new GameObject("Boot").AddComponent<MyVeloren.Boot>();
                EditorSceneManager.SaveScene(sc, Scene);
            }
            EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(Scene, true) };
            Directory.CreateDirectory("Assets/Veloren/Resources");
            File.WriteAllText("Assets/Veloren/Resources/version.txt", Version());
            AssetDatabase.Refresh();
            PlayerSettings.companyName = "ayoub5550";
            PlayerSettings.productName = "My Veloren";
            PlayerSettings.bundleVersion = Version();
            PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, PackageId);
            PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Standalone, PackageId);
            PlayerSettings.colorSpace = ColorSpace.Gamma;
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.AutoRotation;
            PlayerSettings.allowedAutorotateToLandscapeLeft = true;
            PlayerSettings.allowedAutorotateToLandscapeRight = true;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
            PlayerSettings.SplashScreen.backgroundColor = Color.black;
            PlayerSettings.defaultScreenWidth = 1600; PlayerSettings.defaultScreenHeight = 720;
            PlayerSettings.fullScreenMode = FullScreenMode.FullScreenWindow;
            PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
            PlayerSettings.SetIl2CppCompilerConfiguration(BuildTargetGroup.Android, Il2CppCompilerConfiguration.Release);
            PlayerSettings.SetManagedStrippingLevel(BuildTargetGroup.Android, ManagedStrippingLevel.Low);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.Android.minSdkVersion = (AndroidSdkVersions)26;
            PlayerSettings.Android.targetSdkVersion = (AndroidSdkVersions)34;
            PlayerSettings.Android.bundleVersionCode = int.Parse(Environment.GetEnvironmentVariable("VEL_VERSION_CODE") ?? "10");
            PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android, false);
            PlayerSettings.SetGraphicsAPIs(BuildTarget.Android, new[] { UnityEngine.Rendering.GraphicsDeviceType.OpenGLES3 });
            PlayerSettings.Android.forceInternetPermission = false;
            var ks = Environment.GetEnvironmentVariable("VEL_KEYSTORE");
            if (!string.IsNullOrEmpty(ks) && File.Exists(ks))
            {
                PlayerSettings.Android.useCustomKeystore = true;
                PlayerSettings.Android.keystoreName = ks;
                PlayerSettings.Android.keystorePass = Environment.GetEnvironmentVariable("VEL_KEYSTORE_PASS");
                PlayerSettings.Android.keyaliasName = Environment.GetEnvironmentVariable("VEL_KEY_ALIAS") ?? "myveloren";
                PlayerSettings.Android.keyaliasPass = Environment.GetEnvironmentVariable("VEL_KEYSTORE_PASS");
            }
            AssetDatabase.SaveAssets();
            Debug.Log("[VelBuild] configured version=" + Version() + " vc=" + PlayerSettings.Android.bundleVersionCode);
        }

        public static void Compile() { Configure(); Debug.Log("[VelBuild] compile ok"); }

        public static void BuildLinux()
        {
            Configure();
            var r = BuildPipeline.BuildPlayer(new BuildPlayerOptions { scenes = new[] { Scene }, locationPathName = "Builds/linux/myveloren.x86_64", target = BuildTarget.StandaloneLinux64, targetGroup = BuildTargetGroup.Standalone });
            Finish(r, "Builds/linux/myveloren.x86_64");
        }

        public static void BuildAndroid()
        {
            Configure();
            var outPath = Environment.GetEnvironmentVariable("VEL_OUT") ?? "Builds/my-veloren.apk";
            Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(outPath)));
            var r = BuildPipeline.BuildPlayer(new BuildPlayerOptions { scenes = new[] { Scene }, locationPathName = outPath, target = BuildTarget.Android, targetGroup = BuildTargetGroup.Android });
            PlayerSettings.Android.useCustomKeystore = false; PlayerSettings.Android.keystoreName = ""; PlayerSettings.Android.keyaliasName = "";
            AssetDatabase.SaveAssets();
            Finish(r, outPath);
        }

        static void Finish(BuildReport r, string outPath)
        {
            Debug.Log($"[VelBuild] result={r.summary.result} errors={r.summary.totalErrors} size={r.summary.totalSize} out={outPath}");
            if (r.summary.result != BuildResult.Succeeded) { EditorApplication.Exit(1); return; }
            if (File.Exists(outPath))
            {
                using var s = File.OpenRead(outPath);
                Debug.Log("[VelBuild] sha256=" + BitConverter.ToString(SHA256.Create().ComputeHash(s)).Replace("-", "").ToLowerInvariant());
            }
        }
    }
}
