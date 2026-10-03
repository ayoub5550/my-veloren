// MagicaVoxel (.vox) reader + face-culled mesher for Veloren assets.
// Palette semantics follow Veloren common/src/figure/mod.rs (MatSegment::from_vox): raw palette index-1 of
// 0/1/2/3/4/5/7 are materials (skin, hair, eye dark, eye light, skin dark, skin light, eye white) recoloured at load.
// Axis mapping: Veloren/MagicaVoxel (x right, y forward, z up) -> Unity (x, z, y).
// SPDX-License-Identifier: GPL-3.0-or-later
using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace MyVeloren
{
    public sealed class VoxModel
    {
        public int SX, SY, SZ;                   // Veloren axes
        public byte[] Index;                     // raw XYZI colour index (1..255), 0 = empty
        public Color32[] Palette = new Color32[256]; // Palette[raw-1]
        public string Name;

        public int Get(int x, int y, int z) =>
            (x < 0 || y < 0 || z < 0 || x >= SX || y >= SY || z >= SZ) ? 0 : Index[x + SX * (y + SY * z)];

        public static VoxModel Load(string key)
        {
            var ta = Resources.Load<TextAsset>("Vox/" + key);
            if (ta == null) throw new FileNotFoundException("missing Veloren vox resource: " + key);
            var m = Parse(ta.bytes);
            m.Name = key;
            return m;
        }

        public static VoxModel Parse(byte[] b)
        {
            if (b.Length < 8 || b[0] != 'V' || b[1] != 'O' || b[2] != 'X' || b[3] != ' ')
                throw new InvalidDataException("not a VOX file");
            var m = new VoxModel();
            bool haveSize = false, haveVox = false, havePal = false;
            int p = 8;
            // MAIN header
            p += 12;
            while (p + 12 <= b.Length)
            {
                string id = System.Text.Encoding.ASCII.GetString(b, p, 4);
                int n = BitConverter.ToInt32(b, p + 4);
                int c = BitConverter.ToInt32(b, p + 8);
                int d = p + 12;
                if (id == "SIZE" && !haveSize)
                {
                    m.SX = BitConverter.ToInt32(b, d); m.SY = BitConverter.ToInt32(b, d + 4); m.SZ = BitConverter.ToInt32(b, d + 8);
                    m.Index = new byte[m.SX * m.SY * m.SZ];
                    haveSize = true;
                }
                else if (id == "XYZI" && haveSize && !haveVox)
                {
                    int cnt = BitConverter.ToInt32(b, d);
                    for (int i = 0; i < cnt; i++)
                    {
                        int o = d + 4 + i * 4;
                        int x = b[o], y = b[o + 1], z = b[o + 2];
                        if (x < m.SX && y < m.SY && z < m.SZ) m.Index[x + m.SX * (y + m.SY * z)] = b[o + 3];
                    }
                    haveVox = true; // first model only (Veloren figures use one model per file)
                }
                else if (id == "RGBA")
                {
                    for (int i = 0; i < 256; i++)
                        m.Palette[i] = new Color32(b[d + i * 4], b[d + i * 4 + 1], b[d + i * 4 + 2], 255);
                    havePal = true;
                }
                p = d + n + (id == "MAIN" ? 0 : c);
                if (id == "MAIN") p = d + n; // children follow
            }
            if (!haveVox) throw new InvalidDataException("VOX has no XYZI chunk");
            if (!havePal) for (int i = 0; i < 256; i++) m.Palette[i] = new Color32(200, 200, 200, 255);
            return m;
        }
    }

    /// Material colours for Veloren figure palettes (humanoid_color_manifest.ron, Human / HumanOne defaults).
    public struct FigureColors
    {
        public Color32 Skin, SkinDark, SkinLight, Hair, EyeDark, EyeLight, EyeWhite;
        public Color32? GreyTint; // recolor_grey for "grayscale" armour

        public static FigureColors HumanDefault => new FigureColors
        {
            Skin = new Color32(228, 173, 146, 255), SkinDark = new Color32(222, 166, 142, 255),
            SkinLight = new Color32(239, 182, 153, 255), Hair = new Color32(176, 106, 41, 255),
            EyeDark = new Color32(54, 31, 11, 255), EyeLight = new Color32(73, 41, 13, 255),
            EyeWhite = new Color32(255, 255, 255, 255),
        };
    }

    public static class VoxMesher
    {
        static readonly Vector3Int[] Dirs = { new(1, 0, 0), new(-1, 0, 0), new(0, 1, 0), new(0, -1, 0), new(0, 0, 1), new(0, 0, -1) };

        /// Colour of one voxel (Veloren axes). colors==null -> plain palette (world structures).
        public static Color32 ColorOf(VoxModel m, int raw, FigureColors? colors)
        {
            int i = raw - 1;
            if (colors.HasValue)
            {
                var f = colors.Value;
                switch (i)
                {
                    case 0: return f.Skin; case 1: return f.Hair; case 2: return f.EyeDark; case 3: return f.EyeLight;
                    case 4: return f.SkinDark; case 5: return f.SkinLight; case 7: return f.EyeWhite;
                }
                var c = m.Palette[i];
                if (f.GreyTint.HasValue && c.r == c.g && c.g == c.b) return RecolorGrey(c, f.GreyTint.Value);
                return c;
            }
            return m.Palette[i];
        }

        // voxygen/src/scene/figure/load.rs recolor_grey (BASE_GREY 178, linear-space multiply)
        public static Color32 RecolorGrey(Color32 rgb, Color32 tint)
        {
            float L(float v) => Mathf.Pow(v, 2.2f);
            float S(float v) => Mathf.Pow(Mathf.Clamp01(v), 1f / 2.2f);
            float g = L(rgb.r / 178f);
            return new Color32((byte)(S(g * L(tint.r / 255f)) * 255), (byte)(S(g * L(tint.g / 255f)) * 255), (byte)(S(g * L(tint.b / 255f)) * 255), 255);
        }

        /// Build a mesh; offset is in Veloren voxel units (the manifest vox_spec offset), scale converts to metres.
        public static Mesh Build(VoxModel m, Vector3 offset, float scale, FigureColors? colors = null, bool flipX = false)
        {
            var verts = new List<Vector3>(); var norms = new List<Vector3>(); var cols = new List<Color32>(); var tris = new List<int>();
            for (int z = 0; z < m.SZ; z++)
            for (int y = 0; y < m.SY; y++)
            for (int x = 0; x < m.SX; x++)
            {
                int raw = m.Get(x, y, z);
                if (raw == 0) continue;
                var col = ColorOf(m, raw, colors);
                int vx = flipX ? m.SX - 1 - x : x;
                foreach (var d in Dirs)
                {
                    int dx = flipX ? -d.x : d.x;
                    if (m.Get(x + d.x, y + d.y, z + d.z) != 0) continue;
                    AddFace(verts, norms, cols, tris, new Vector3(vx, y, z) + offset, new Vector3Int(dx, d.y, d.z), scale, col);
                }
            }
            var mesh = new Mesh { indexFormat = UnityEngine.Rendering.IndexFormat.UInt32, name = m.Name };
            mesh.SetVertices(verts); mesh.SetNormals(norms); mesh.SetColors(cols); mesh.SetTriangles(tris, 0);
            mesh.RecalculateBounds();
            return mesh;
        }

        /// Unit cube face at voxel min-corner p (Veloren axes) facing d; appended in Unity axes.
        public static void AddFace(List<Vector3> v, List<Vector3> n, List<Color32> c, List<int> t, Vector3 p, Vector3Int d, float s, Color32 col)
        {
            // corners in Veloren axes
            Vector3 a, b, e, f;
            if (d.x != 0) { float x = d.x > 0 ? 1 : 0; a = new(x, 0, 0); b = new(x, 1, 0); e = new(x, 1, 1); f = new(x, 0, 1); }
            else if (d.y != 0) { float y = d.y > 0 ? 1 : 0; a = new(0, y, 0); b = new(1, y, 0); e = new(1, y, 1); f = new(0, y, 1); }
            else { float z = d.z > 0 ? 1 : 0; a = new(0, 0, z); b = new(1, 0, z); e = new(1, 1, z); f = new(0, 1, z); }
            Vector3 U(Vector3 q) => new Vector3(q.x, q.z, q.y) * s;
            var nu = new Vector3(d.x, d.z, d.y);
            int i = v.Count;
            v.Add(U(p + a)); v.Add(U(p + b)); v.Add(U(p + e)); v.Add(U(p + f));
            for (int k = 0; k < 4; k++) { n.Add(nu); c.Add(col); }
            // Unity front faces: cross(b-a, c-a) points along the normal
            if (Vector3.Dot(Vector3.Cross(v[i + 1] - v[i], v[i + 2] - v[i]), nu) >= 0)
            { t.Add(i); t.Add(i + 1); t.Add(i + 2); t.Add(i); t.Add(i + 2); t.Add(i + 3); }
            else
            { t.Add(i); t.Add(i + 2); t.Add(i + 1); t.Add(i); t.Add(i + 3); t.Add(i + 2); }
        }
    }
}
