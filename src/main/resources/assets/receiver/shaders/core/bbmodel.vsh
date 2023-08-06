#version 150

#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;
in float Bone_ID;

uniform sampler2D Sampler2;

uniform ivec2 UV2;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 Bones[16];

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec4 pos = ModelViewMat * Bones[int(Bone_ID)] * vec4(Position, 1.0);
    gl_Position = ProjMat * pos;

    vertexDistance = length(pos.xyz);
    vec4 normal = Bones[int(Bone_ID)] * vec4(Normal, 0.0);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normal.xyz, Color) * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
