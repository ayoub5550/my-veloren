// Local, deterministic voxel terrain (dev1 feasibility): seeded height field in 1 m blocks, chunked meshes with
// colliders, water plane and Veloren tree structures (assets/world/tree/*). NOT Veloren's worldgen (world/ crate);
// see docs/PARITY-MATRIX.md. SPDX-License-Identifier: GPL-3.0-or-later
using System.Collections.Generic;
using UnityEngine;

namespace MyVeloren
{
    public sealed class World : MonoBehaviour
    {
        public const int Chunk = 32, Chunks = 8, Size = Chunk * Chunks, Water = 14;
        public int Seed = 1337;
        public Material VoxelMat, WaterMat;
        int[,] height;
        public int TreeCount { get; private set; }
        public int ChunkCount { get; private set; }

        public int H(int x, int z) => height[Mathf.Clamp(x, 0, Size - 1), Mathf.Clamp(z, 0, Size - 1)];
        public Vector3 Center => new Vector3(Size / 2f, H(Size / 2, Size / 2) + 1, Size / 2f);

        public void Generate()
        {
            var rng = new System.Random(Seed);
            float ox = rng.Next(0, 10000), oz = rng.Next(0, 10000);
            height = new int[Size, Size];
            for (int x = 0; x < Size; x++)
            for (int z = 0; z < Size; z++)
            {
                float nx = (x + ox) / 96f, nz = (z + oz) / 96f;
                float hills = Mathf.PerlinNoise(nx, nz) * 0.6f + Mathf.PerlinNoise(nx * 2.1f, nz * 2.1f) * 0.28f + Mathf.PerlinNoise(nx * 5f, nz * 5f) * 0.12f;
                float mount = Mathf.Pow(Mathf.PerlinNoise(nx * 0.5f + 7, nz * 0.5f + 3), 3f) * 40f;
                // island falloff keeps the play area bounded by water
                float dx = x / (float)Size - 0.5f, dz = z / (float)Size - 0.5f;
                float fall = Mathf.Clamp01(1.4f - Mathf.Sqrt(dx * dx + dz * dz) * 3.2f);
                height[x, z] = Mathf.Max(2, Mathf.RoundToInt((6 + hills * 26f + mount) * fall + 4));
            }
            for (int cx = 0; cx < Chunks; cx++)
            for (int cz = 0; cz < Chunks; cz++)
                BuildChunk(cx, cz);
            var water = GameObject.CreatePrimitive(PrimitiveType.Quad);
            Destroy(water.GetComponent<Collider>());
            water.name = "Water";
            water.transform.SetParent(transform);
            water.transform.position = new Vector3(Size / 2f, Water + 0.8f, Size / 2f);
            water.transform.rotation = Quaternion.Euler(90, 0, 0);
            water.transform.localScale = new Vector3(Size * 3, Size * 3, 1);
            water.GetComponent<MeshRenderer>().sharedMaterial = WaterMat;
            PlaceTrees(rng);
        }

        Color32 BlockColor(int x, int y, int z, int top)
        {
            uint hsh = (uint)(x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ Seed);
            float j = ((hsh % 1000) / 1000f - 0.5f) * 0.08f;
            Color c;
            int depth = top - y;
            float slope = Mathf.Max(Mathf.Abs(H(x + 1, z) - H(x - 1, z)), Mathf.Abs(H(x, z + 1) - H(x, z - 1)));
            if (top <= Water + 1) c = depth < 3 ? new Color(0.84f, 0.76f, 0.52f) : new Color(0.55f, 0.5f, 0.42f); // sand
            else if (top > 46) c = depth < 1 ? new Color(0.93f, 0.95f, 0.97f) : new Color(0.5f, 0.5f, 0.52f); // snow / rock
            else if (slope >= 4 || depth > 3) c = new Color(0.47f, 0.45f, 0.42f); // stone
            else if (depth == 0) c = Color.Lerp(new Color(0.28f, 0.55f, 0.18f), new Color(0.42f, 0.6f, 0.2f), Mathf.PerlinNoise(x * 0.05f, z * 0.05f));
            else c = new Color(0.45f, 0.32f, 0.2f); // dirt
            return new Color(c.r + j, c.g + j, c.b + j);
        }

        void BuildChunk(int cx, int cz)
        {
            var v = new List<Vector3>(); var n = new List<Vector3>(); var col = new List<Color32>(); var t = new List<int>();
            for (int x = cx * Chunk; x < (cx + 1) * Chunk; x++)
            for (int z = cz * Chunk; z < (cz + 1) * Chunk; z++)
            {
                int top = height[x, z] - 1;
                // Veloren axes: (x, y=z_unity, z=up)
                VoxMesher.AddFace(v, n, col, t, new Vector3(x, z, top), new Vector3Int(0, 0, 1), 1f, BlockColor(x, top, z, top));
                foreach (var (dx, dz) in new[] { (1, 0), (-1, 0), (0, 1), (0, -1) })
                {
                    int nh = (x + dx < 0 || z + dz < 0 || x + dx >= Size || z + dz >= Size) ? 0 : height[x + dx, z + dz];
                    for (int y = nh; y <= top; y++)
                        VoxMesher.AddFace(v, n, col, t, new Vector3(x, z, y), new Vector3Int(dx, dz, 0), 1f, BlockColor(x, y, z, top));
                }
            }
            var mesh = new Mesh { indexFormat = UnityEngine.Rendering.IndexFormat.UInt32, name = $"chunk_{cx}_{cz}" };
            mesh.SetVertices(v); mesh.SetNormals(n); mesh.SetColors(col); mesh.SetTriangles(t, 0); mesh.RecalculateBounds();
            var go = new GameObject(mesh.name);
            go.transform.SetParent(transform, false);
            go.AddComponent<MeshFilter>().sharedMesh = mesh;
            go.AddComponent<MeshRenderer>().sharedMaterial = VoxelMat;
            go.AddComponent<MeshCollider>().sharedMesh = mesh;
            ChunkCount++;
        }

        static readonly string[] TreeKeys =
        {
            "world/tree/pine_green/1", "world/tree/pine_green/2", "world/tree/pine_green/3", "world/tree/pine_green/4",
            "world/tree/pine_green/5", "world/tree/pine_green/6", "world/tree/pine_green/7", "world/tree/pine_green/8",
            "world/tree/temperate_small/1", "world/tree/temperate_small/2", "world/tree/temperate_small/3",
            "world/tree/temperate_small/4", "world/tree/temperate_small/5", "world/tree/temperate_small/6",
            "world/tree/oak_stump/1", "world/tree/oak_stump/2", "world/tree/oak_stump/3",
        };

        void PlaceTrees(System.Random rng)
        {
            var meshes = new Mesh[TreeKeys.Length];
            var sizes = new Vector3Int[TreeKeys.Length];
            for (int i = 0; i < TreeKeys.Length; i++)
            {
                var m = VoxModel.Load(TreeKeys[i]);
                // trunk base: centre of x/y footprint, lowest solid voxel at z=0 sits one block into the ground
                meshes[i] = VoxMesher.Build(m, new Vector3(-m.SX / 2f, -m.SY / 2f, -1), 1f);
                sizes[i] = new Vector3Int(m.SX, m.SY, m.SZ);
            }
            var c = Center;
            for (int tries = 0; tries < 4000 && TreeCount < 140; tries++)
            {
                int x = rng.Next(8, Size - 8), z = rng.Next(8, Size - 8);
                int h = height[x, z];
                if (h <= Water + 2 || h > 44) continue;
                if (Mathf.Abs(H(x + 2, z) - H(x - 2, z)) > 3 || Mathf.Abs(H(x, z + 2) - H(x, z - 2)) > 3) continue;
                if (new Vector2(x - c.x, z - c.z).magnitude < 10) continue; // keep spawn clear
                bool pine = h > 26 || Mathf.PerlinNoise(x * 0.02f, z * 0.02f) > 0.55f;
                int k = pine ? rng.Next(0, 8) : rng.Next(8, TreeKeys.Length);
                var go = new GameObject("tree_" + TreeKeys[k]);
                go.transform.SetParent(transform, false);
                go.transform.position = new Vector3(x + 0.5f, h, z + 0.5f);
                go.transform.rotation = Quaternion.Euler(0, rng.Next(0, 4) * 90, 0);
                go.AddComponent<MeshFilter>().sharedMesh = meshes[k];
                go.AddComponent<MeshRenderer>().sharedMaterial = VoxelMat;
                var cap = go.AddComponent<CapsuleCollider>();
                cap.radius = 0.8f; cap.height = sizes[k].z * 0.6f; cap.center = new Vector3(0, cap.height / 2, 0);
                TreeCount++;
            }
        }
    }
}
