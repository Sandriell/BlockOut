package com.ldtteam.blockout.font;

import com.ldtteam.blockout.util.color.Color;
import com.ldtteam.blockout.util.color.ColorUtils;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.EmptyGlyph;
import net.minecraft.client.gui.fonts.Font;
import net.minecraft.client.gui.fonts.IGlyph;
import net.minecraft.client.gui.fonts.TexturedGlyph;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.List;

public class MultiColoredFontRenderer extends FontRenderer {

    private boolean dropShadow;
    private int state = 0;
    private int red;
    private int green;
    private int blue;

    public MultiColoredFontRenderer(final TextureManager textureManagerIn, final Font fontIn) {
        super(textureManagerIn, new MultiColoredFont(fontIn));
    }

    @Override
    public List<String> listFormattedStringToWidth(final String str, final int wrapWidth) {
        return super.listFormattedStringToWidth(str, wrapWidth);
    }

    @Override
    public String wrapFormattedStringToWidth(final String str, final int wrapWidth) {
        int i = this.sizeStringToWidth(str, wrapWidth);

        if(str.length() <= i) {
            return str;
        }
        else {
            String s = str.substring(0, i);
            char c0 = str.charAt(i);
            boolean flag = c0 == 32 || c0 == 10;
            String s1 = getCustomFormatFromString(s) + str.substring(i + (flag ? 1 : 0));
            return s + "\n" + this.wrapFormattedStringToWidth(s1, wrapWidth);
        }
    }

    public static String getCustomFormatFromString(String text) {
        String s = "";
        int i = 0;
        int j = text.length();

        while((i < j - 1)) {
            char c = text.charAt(i);
            // vanilla formatting
            if(c == 167) {

                char c0 = text.charAt(i + 1);

                if(c0 >= 48 && c0 <= 57 || c0 >= 97 && c0 <= 102 || c0 >= 65 && c0 <= 70) {
                    s = "\u00a7" + c0;
                    i++;
                }
                else if(c0 >= 107 && c0 <= 111 || c0 >= 75 && c0 <= 79 || c0 == 114 || c0 == 82) {
                    s = s + "\u00a7" + c0;
                    i++;
                }
            }
            // custom formatting
            else if((int) c >= ColorUtils.MARKER && (int) c <= ColorUtils.MARKER + 0xFF) {
                s = String.format("%s%s%s", c, text.charAt(i + 1), text.charAt(i + 2));
                i += 2;
            }
            i++;
        }

        return s;
    }

    @Override
    public int renderString(final String text, final float x, final float y, final int color, final boolean dropShadow, final Matrix4f matrix, final IRenderTypeBuffer buffer, final boolean p_228079_8_, final int p_228079_9_, final int p_228079_10_) {
        this.dropShadow = dropShadow;
        return super.renderString(this.preProcessString(text, new Color(color)), x, y, new Color(255,255,255,255).getRGB(), dropShadow, matrix, buffer, p_228079_8_, p_228079_9_, p_228079_10_);
    }

    @Override
    public void drawGlyph(final TexturedGlyph p_228077_1_, final boolean p_228077_2_, final boolean p_228077_3_, final float p_228077_4_, final float p_228077_5_, final float p_228077_6_, final Matrix4f p_228077_7_, final IVertexBuilder p_228077_8_, final float p_228077_9_, final float p_228077_10_, final float p_228077_11_, final float p_228077_12_, final int p_228077_13_) {
        if (p_228077_1_)

        super.drawGlyph(p_228077_1_, p_228077_2_, p_228077_3_, p_228077_4_, p_228077_5_, p_228077_6_, p_228077_7_, p_228077_8_, p_228077_9_, p_228077_10_, p_228077_11_, p_228077_12_, p_228077_13_);
    }

    private String preProcessString(final String text, final Color defaultColor)
    {
        StringBuilder workingString = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            final char charInString = text.charAt(index);

            if (charInString == 167 && index + 1 < text.length()) {
                TextFormatting textFormatting = TextFormatting.fromFormattingCode(text.charAt(index + 1));
                if (textFormatting != null) {
                    if (textFormatting.isNormalStyle()) {
                        workingString.append(defaultColor.encodeColor());
                    } else if (textFormatting.getColor() != null) {
                        int formattingColor = textFormatting.getColor();
                        workingString.append(new Color(formattingColor).encodeColor());
                    }
                }

                ++index;
            } else {
                workingString.append(charInString);
            }
        }

        return workingString.toString();
    }
}