package me.eldodebug.soar.utils;

import java.awt.Color;
import java.util.regex.Pattern;

import net.minecraft.client.renderer.GlStateManager;

public class ColorUtils {

	// ✅ Constants
	private static final float COLOR_COMPONENT_MAX = 255.0F;
	private static final int FULL_CIRCLE_DEGREES = 360;
	private static final int HALF_CIRCLE_DEGREES = 180;
	private static final int COLOR_SHIFT_16 = 16;
	private static final int COLOR_SHIFT_8 = 8;
	private static final int COLOR_SHIFT_24 = 24;
	private static final int COLOR_MASK = 255;
	
	// ✅ Cached Pattern for color code removal (MASSIVE PERFORMANCE BOOST)
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fklmnor]");

    public static Color getRainbow(int index, double speed, int alpha) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % FULL_CIRCLE_DEGREES);
        float hue = angle / (float) FULL_CIRCLE_DEGREES;
        int color = Color.HSBtoRGB(hue, 1.0F, 1.0F);
        Color c = new Color(color);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
    
    public static Color interpolateColors(int speed, int index, Color start, Color end) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % FULL_CIRCLE_DEGREES);
        angle = (angle >= HALF_CIRCLE_DEGREES ? FULL_CIRCLE_DEGREES - angle : angle) * 2;
        
        return interpolateColorHue(start, end, angle / (float) FULL_CIRCLE_DEGREES);
    }
    
    private static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(
            MathUtils.interpolateFloat(color1HSB[0], color2HSB[0], amount),
            MathUtils.interpolateFloat(color1HSB[1], color2HSB[1], amount),
            MathUtils.interpolateFloat(color1HSB[2], color2HSB[2], amount)
        );

        return new Color(
            resultColor.getRed(),
            resultColor.getGreen(),
            resultColor.getBlue(),
            MathUtils.interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
        );
    }
    
	public static float getHue(Color color) {
		return rgbToHsb(color)[0];
	}
	
	public static float getSaturation(Color color) {
		return rgbToHsb(color)[1];
	}
	
	public static float getBrightness(Color color) {
		return rgbToHsb(color)[2];
	}
	
	private static float[] rgbToHsb(Color color) {
		if (color == null) {
			return new float[]{0.0F, 0.0F, 0.0F};
		}
		
        float[] hsv = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
        return hsv;
	}
	
	// ✅ MASSIVE FIX: Từ 23 lần replaceAll() → 1 lần regex replace
	public static String removeColorCode(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		return COLOR_CODE_PATTERN.matcher(text).replaceAll("");
	}
	
	public static void setColor(int color, float alpha) {
        float r = (float) (color >> COLOR_SHIFT_16 & COLOR_MASK) / COLOR_COMPONENT_MAX;
        float g = (float) (color >> COLOR_SHIFT_8 & COLOR_MASK) / COLOR_COMPONENT_MAX;
        float b = (float) (color & COLOR_MASK) / COLOR_COMPONENT_MAX;
        
        GlStateManager.color(r, g, b, alpha);
	}
	
	public static Color getColorByInt(int color) {
        float r = (float) (color >> COLOR_SHIFT_16 & COLOR_MASK) / COLOR_COMPONENT_MAX;
        float g = (float) (color >> COLOR_SHIFT_8 & COLOR_MASK) / COLOR_COMPONENT_MAX;
        float b = (float) (color & COLOR_MASK) / COLOR_COMPONENT_MAX;
        float a = (float) (color >> COLOR_SHIFT_24 & COLOR_MASK) / COLOR_COMPONENT_MAX;
        
        return new Color(r, g, b, a);
	}
	
	public static void setColor(int color) {
		float alpha = (float) (color >> COLOR_SHIFT_24 & COLOR_MASK) / COLOR_COMPONENT_MAX;
		setColor(color, alpha);
	}
	
	public static void resetColor() {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
	
	public static Color applyAlpha(Color color, int alpha) {
		if (color == null) {
			return new Color(0, 0, 0, alpha);
		}
		
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
	
	public static float getAlphaByInt(int color) {
        return (float) (color >> COLOR_SHIFT_24 & COLOR_MASK) / COLOR_COMPONENT_MAX;
	}
}