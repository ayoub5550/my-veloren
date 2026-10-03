// Scene bootstrap: builds the offline world, player and camera at runtime. Autopilot (desktop evidence runs):
//   VEL_AUTOPILOT=1 VEL_RESULTS=<file> [VEL_SHOTS=<dir>]  -> walks/jumps/attacks, writes key=value results, quits.
// SPDX-License-Identifier: GPL-3.0-or-later
using System;
using System.Collections;
using System.IO;
using System.Text;
using UnityEngine;

namespace MyVeloren
{
    public sealed class Boot : MonoBehaviour
    {
        public static bool ForceTouchUI;
        World world;
        PlayerController player;
        float fps = 60;
        string version = "dev";

        void Start()
        {
            Application.targetFrameRate = 60;
            QualitySettings.vSyncCount = 0;
            ForceTouchUI = Environment.GetEnvironmentVariable("VEL_TOUCHUI") == "1";
            var vt = Resources.Load<TextAsset>("version");
            if (vt != null) version = vt.text.Trim();

            var voxelMat = new Material(Shader.Find("MyVeloren/VoxelLit"));
            var waterMat = new Material(Shader.Find("MyVeloren/Water"));

            RenderSettings.fog = true;
            RenderSettings.fogMode = FogMode.Linear;
            RenderSettings.fogStartDistance = 60; RenderSettings.fogEndDistance = 170;
            RenderSettings.fogColor = new Color(0.62f, 0.78f, 0.93f);
            RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = new Color(0.55f, 0.62f, 0.72f);
            RenderSettings.ambientEquatorColor = new Color(0.45f, 0.47f, 0.45f);
            RenderSettings.ambientGroundColor = new Color(0.25f, 0.22f, 0.2f);

            var sun = new GameObject("Sun").AddComponent<Light>();
            sun.type = LightType.Directional;
            sun.color = new Color(1f, 0.95f, 0.85f);
            sun.intensity = 1.0f;
            sun.shadows = LightShadows.Hard;
            sun.transform.rotation = Quaternion.Euler(50, -30, 0);

            var cam = new GameObject("Camera").AddComponent<Camera>();
            cam.tag = "MainCamera";
            cam.clearFlags = CameraClearFlags.SolidColor;
            cam.backgroundColor = RenderSettings.fogColor;
            cam.farClipPlane = 180; cam.nearClipPlane = 0.2f; cam.fieldOfView = 60;

            world = new GameObject("World").AddComponent<World>();
            world.VoxelMat = voxelMat; world.WaterMat = waterMat;
            world.Generate();

            var pgo = new GameObject("Player");
            pgo.transform.position = world.Center + Vector3.up;
            var cc = pgo.AddComponent<CharacterController>();
            cc.height = 1.8f; cc.radius = 0.4f; cc.center = new Vector3(0, 0.9f, 0); cc.stepOffset = 1.05f; cc.slopeLimit = 60;
            var body = new GameObject("Humanoid").AddComponent<Humanoid>();
            body.transform.SetParent(pgo.transform, false);
            body.Mat = voxelMat;
            body.Build();
            player = pgo.AddComponent<PlayerController>();
            player.Body = body; player.Cam = cam; player.Yaw = 30;

            Debug.Log($"[MyVeloren] boot version={version} chunks={world.ChunkCount} trees={world.TreeCount} parts={body.PartCount}");
            if (Environment.GetEnvironmentVariable("VEL_AUTOPILOT") == "1") StartCoroutine(Autopilot(body));
        }

        void Update() { fps = Mathf.Lerp(fps, 1f / Mathf.Max(Time.unscaledDeltaTime, 1e-4f), 0.05f); }

        void OnGUI()
        {
            var st = new GUIStyle(GUI.skin.label) { fontSize = Mathf.Max(14, Screen.height / 40) };
            GUI.Label(new Rect(10, 6, Screen.width, 60), $"my-veloren {version} (Unity, offline)  {fps:0} fps", st);
        }

        IEnumerator Autopilot(Humanoid body)
        {
            var results = Environment.GetEnvironmentVariable("VEL_RESULTS");
            var shots = Environment.GetEnvironmentVariable("VEL_SHOTS");
            var sb = new StringBuilder();
            int shot = 0, errors = 0;
            Application.logMessageReceived += (c, s, t) => { if (t == LogType.Exception || t == LogType.Error) errors++; };
            IEnumerator Shot(string name)
            {
                if (string.IsNullOrEmpty(shots)) yield break;
                yield return new WaitForEndOfFrame();
                Directory.CreateDirectory(shots);
                ScreenCapture.CaptureScreenshot(Path.Combine(shots, $"{shot++:00}_{name}.png"));
                yield return null; yield return null;
            }
            yield return new WaitForSeconds(1f);
            var start = player.transform.position;
            yield return Shot("spawn");
            player.AutoMove = new Vector2(0, 1);
            yield return new WaitForSeconds(3f);
            player.Jump();
            yield return new WaitForSeconds(0.25f);
            bool airborne = !player.GetComponent<CharacterController>().isGrounded;
            yield return Shot("walk");
            player.AutoMove = Vector2.zero;
            player.Attack();
            yield return new WaitForSeconds(0.2f);
            bool swung = body.Attacking;
            player.Yaw += 140; player.Pitch = 30; player.Dist = 9;
            yield return new WaitForSeconds(1f);
            yield return Shot("overview");
            float travelled = player.Travelled;
            bool grounded = player.transform.position.y > World.Water - 2;
            bool ok = travelled > 3 && airborne && swung && grounded && errors == 0 && world.TreeCount > 50;
            sb.AppendLine("scenario=1");
            sb.AppendLine($"version={version}");
            sb.AppendLine($"chunks={world.ChunkCount}");
            sb.AppendLine($"trees={world.TreeCount}");
            sb.AppendLine($"figure_parts={body.PartCount}");
            sb.AppendLine($"travelled={travelled:0.0}");
            sb.AppendLine($"jumped_airborne={(airborne ? 1 : 0)}");
            sb.AppendLine($"attack_swing={(swung ? 1 : 0)}");
            sb.AppendLine($"on_ground={(grounded ? 1 : 0)}");
            sb.AppendLine($"start={start} end={player.transform.position}");
            sb.AppendLine($"shots={shot}");
            sb.AppendLine($"errors={errors}");
            sb.AppendLine($"result={(ok ? "pass" : "fail")}");
            if (!string.IsNullOrEmpty(results)) File.WriteAllText(results, sb.ToString());
            Debug.Log("[MyVeloren] autopilot\n" + sb);
            yield return new WaitForSeconds(0.5f);
            Application.Quit(ok ? 0 : 1);
        }
    }
}
