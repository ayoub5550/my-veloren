// SPDX-License-Identifier: GPL-3.0-or-later
Shader "MyVeloren/VoxelLit"
{
    Properties { _Tint ("Tint", Color) = (1,1,1,1) }
    SubShader
    {
        Tags { "RenderType"="Opaque" }
        LOD 200
        CGPROGRAM
        #pragma surface surf Lambert fullforwardshadows
        #pragma target 3.0
        fixed4 _Tint;
        struct Input { float4 color : COLOR; };
        void surf (Input IN, inout SurfaceOutput o) { o.Albedo = IN.color.rgb * _Tint.rgb; o.Alpha = 1; }
        ENDCG
    }
    Fallback "Diffuse"
}
