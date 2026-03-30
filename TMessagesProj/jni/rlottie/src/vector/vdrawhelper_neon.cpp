#if defined(__ARM_NEON__) || defined(__ARM64_NEON__)

#include "vdrawhelper.h"

#include <arm_neon.h>

void memfill32(uint32_t *dest, uint32_t value, int length)
{
    uint32x4_t v = vdupq_n_u32(value);
    int i = 0;
    for (; i + 4 <= length; i += 4)
        vst1q_u32(dest + i, v);
    for (; i < length; i++)
        dest[i] = value;
}

void comp_func_solid_SourceOver_neon(uint32_t *dest, int length, uint32_t color,
                                     uint32_t const_alpha)
{
    if (const_alpha != 255) color = BYTE_MUL(color, const_alpha);

    uint32_t ialpha = 255 - (color >> 24);
    if (ialpha == 0) {
        memfill32(dest, color, length);
        return;
    }

    uint8x8_t vcolor = vreinterpret_u8_u32(vdup_n_u32(color));
    uint8x8_t via = vdup_n_u8(ialpha);

    int i = 0;
    for (; i + 2 <= length; i += 2) {
        uint8x8_t vdst = vreinterpret_u8_u32(vld1_u32(dest + i));
        uint16x8_t prod = vmull_u8(vdst, via);
        uint8x8_t blended = vshrn_n_u16(prod, 8);
        uint8x8_t result = vadd_u8(vcolor, blended);
        vst1_u32(dest + i, vreinterpret_u32_u8(result));
    }
    for (; i < length; i++) {
        dest[i] = color + BYTE_MUL(dest[i], ialpha);
    }
}
#endif
