#version 150

uniform sampler2D InSampler;

layout(std140) uniform SaturationConfig {
    float Amount;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    float s = clamp(Amount, 0.0, 2.0);
    vec3 rgb = mix(vec3(luma), color.rgb, s);
    fragColor = vec4(rgb, color.a);
}
