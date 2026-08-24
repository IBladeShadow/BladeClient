#version 150

uniform sampler2D CurrentSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    float Strength;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 cur = texture(CurrentSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);
    float s = clamp(Strength, 0.0, 0.95);
    vec3 rgb = mix(cur.rgb, prev.rgb, s);
    fragColor = vec4(rgb, 1.0);
}
