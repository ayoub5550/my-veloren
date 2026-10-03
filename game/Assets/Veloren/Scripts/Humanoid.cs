// Veloren humanoid (Human, Male) assembled from real figure/armour .vox parts.
// Offsets: humanoid_*_manifest.ron vox_spec offsets; bone positions: voxygen/anim/src/character/mod.rs SkeletonAttr
// (Human Male: head (-2.3, 9.5) x0.9 scale, chest (0, 8), belt (0,-2), shorts (0,-5), hand (7,-0.25,0.5), foot (3.4,0.5,2)).
// Animation is a simplified procedural walk/idle cycle, NOT a port of anim/src/character/run.rs.
// SPDX-License-Identifier: GPL-3.0-or-later
using UnityEngine;

namespace MyVeloren
{
    public sealed class Humanoid : MonoBehaviour
    {
        public const float Scale = 1.8f / 25f; // figure voxel -> metres
        public Material Mat;
        Transform chest, head, belt, shorts, handL, handR, footL, footR, sword;
        public int PartCount { get; private set; }
        public float Speed01; // set by controller (0 idle .. 1 run)
        public bool Attacking;
        float phase, attackT;

        static Vector3 U(float x, float y, float z) => new Vector3(x, z, y) * Scale; // Veloren -> Unity

        Transform Bone(string name, Transform parent, Vector3 pos)
        {
            var t = new GameObject(name).transform;
            t.SetParent(parent, false);
            t.localPosition = pos;
            return t;
        }

        void Part(Transform bone, string key, Vector3 off, FigureColors colors, bool flip = false, float scale = 1f)
        {
            var m = VoxModel.Load(key);
            var go = new GameObject(key);
            go.transform.SetParent(bone, false);
            go.transform.localScale = Vector3.one * scale;
            go.AddComponent<MeshFilter>().sharedMesh = VoxMesher.Build(m, off, Scale, colors, flip);
            go.AddComponent<MeshRenderer>().sharedMaterial = Mat;
            PartCount++;
        }

        public void Build()
        {
            var skin = FigureColors.HumanDefault;
            var chestC = skin; chestC.GreyTint = new Color32(44, 74, 109, 255);  // "Blue" chest
            var pantsC = skin; pantsC.GreyTint = new Color32(28, 66, 109, 255);  // pants default colour
            var torso = transform;
            chest = Bone("chest", torso, U(0, 0, 8));
            head = Bone("head", chest, U(0, -2.3f, 9.5f));
            belt = Bone("belt", chest, U(0, 0, -2));
            shorts = Bone("shorts", chest, U(0, 0, -5));
            handL = Bone("hand_l", chest, U(-7, -0.25f, 0.5f));
            handR = Bone("hand_r", chest, U(7, -0.25f, 0.5f));
            footL = Bone("foot_l", torso, U(-3.4f, 0.5f, 2));
            footR = Bone("foot_r", torso, U(3.4f, 0.5f, 2));

            // head manifest (Human, Male): offset (-7,-2.5,-2) + part offsets
            var ho = new Vector3(-7, -2.5f, -2);
            Part(head, "figure/head/human/male", ho + new Vector3(0, 2, 0), skin, false, 0.9f);
            Part(head, "figure/eyes/general/male_default-0", ho + new Vector3(3, 9, 2), skin, false, 0.9f);
            Part(head, "figure/hair/human/male-1", ho + new Vector3(2, 1, 0), skin, false, 0.9f);
            Part(chest, "armor/misc/chest/grayscale", new Vector3(-7, -3.5f, 2), chestC);
            Part(belt, "armor/misc/belt/dark", new Vector3(-4, -3.5f, 2), skin);
            Part(shorts, "armor/misc/pants/grayscale", new Vector3(-5, -3.5f, 1), pantsC);
            Part(handL, "figure/body/hand", new Vector3(-1.5f, -1.5f, -2.5f), skin, true);
            Part(handR, "figure/body/hand", new Vector3(-1.5f, -1.5f, -2.5f), skin);
            Part(footL, "armor/misc/foot/dark", new Vector3(-2.5f, -3.5f, -2), skin, true);
            Part(footR, "armor/misc/foot/dark", new Vector3(-2.5f, -3.5f, -2), skin);

            // starter sword held in the right hand, blade forward/up
            sword = Bone("main", handR, U(0, 0, 0));
            var sm = VoxModel.Load("weapon/sword/starter");
            var sgo = new GameObject("weapon/sword/starter");
            sgo.transform.SetParent(sword, false);
            sgo.AddComponent<MeshFilter>().sharedMesh = VoxMesher.Build(sm, new Vector3(-sm.SX / 2f, -sm.SY / 2f, -3), Scale, null);
            sgo.AddComponent<MeshRenderer>().sharedMaterial = Mat;
            sword.localRotation = Quaternion.Euler(60, 0, 0);
            PartCount++;
        }

        public void Attack() { if (attackT <= 0) attackT = 0.45f; }

        void Update()
        {
            if (chest == null) return;
            phase += Time.deltaTime * Mathf.Lerp(2f, 11f, Speed01);
            float s = Mathf.Sin(phase), c = Mathf.Cos(phase), amp = Speed01;
            footL.localPosition = U(-3.4f, 0.5f + s * 4f * amp, 2 + Mathf.Max(0, c) * 1.5f * amp);
            footR.localPosition = U(3.4f, 0.5f - s * 4f * amp, 2 + Mathf.Max(0, -c) * 1.5f * amp);
            footL.localRotation = Quaternion.Euler(-s * 25 * amp, 0, 0);
            footR.localRotation = Quaternion.Euler(s * 25 * amp, 0, 0);
            chest.localPosition = U(0, 0, 8 + Mathf.Abs(c) * 0.6f * amp + Mathf.Sin(phase * 0.5f) * 0.2f * (1 - amp));
            chest.localRotation = Quaternion.Euler(8 * amp, s * 6 * amp, 0);
            head.localRotation = Quaternion.Euler(-6 * amp, -s * 4 * amp, 0);
            handL.localPosition = U(-7, -0.25f - s * 3f * amp, 0.5f);
            if (attackT > 0)
            {
                attackT -= Time.deltaTime;
                float k = 1 - attackT / 0.45f;
                handR.localPosition = U(6, 3 + Mathf.Sin(k * Mathf.PI) * 4, 3 - k * 3);
                sword.localRotation = Quaternion.Euler(Mathf.Lerp(-40, 130, k), 0, 0);
                Attacking = true;
            }
            else
            {
                handR.localPosition = U(7, -0.25f + s * 3f * amp, 0.5f);
                sword.localRotation = Quaternion.Euler(60, 0, 0);
                Attacking = false;
            }
        }
    }
}
