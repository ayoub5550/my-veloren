// SPDX-License-Identifier: GPL-3.0-or-later
Shader "MyVeloren/Water"
{
    Properties { _Color ("Color", Color) = (0.15,0.35,0.55,0.65) }
    SubShader
    {
        Tags { "Queue"="Transparent" "RenderType"="Transparent" }
        Blend SrcAlpha OneMinusSrcAlpha
        ZWrite Off
        CGPROGRAM
        #pragma surface surf Lambert alpha:fade
        fixed4 _Color;
        struct Input { float3 worldPos; };
        void surf (Input IN, inout SurfaceOutput o)
        {
            float w = sin(IN.worldPos.x * 0.35 + _Time.y) * cos(IN.worldPos.z * 0.3 + _Time.y * 0.8) * 0.04;
            o.Albedo = _Color.rgb + w; o.Alpha = _Color.a;
        }
        ENDCG
    }
}
