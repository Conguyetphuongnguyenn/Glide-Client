package me.eldodebug.soar.ui.particle;

import java.util.Random;

import me.eldodebug.soar.utils.TimerUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Particle {

	// ✅ CRITICAL FIX: Static Random - chỉ tạo 1 lần cho tất cả particles
	private static final Random RANDOM = new Random();
	
	// ✅ Constants
	private static final float MIN_RANDOM_SIZE = 0.3F;
	private static final float MAX_RANDOM_SIZE = 0.6F;
	private static final float BASE_SIZE_OFFSET = 0.4F;
	private static final int MAX_SPEED = 5;
	private static final int INTERPOLATION_STEPS = 64;
	private static final float INTERPOLATION_P1_START = 1.02F;
	private static final float INTERPOLATION_P1_END = 1.0F;
	private static final float INTERPOLATION_P2_START = 1.02F;
	private static final float INTERPOLATION_P2_END = 1.0F;

	private final Minecraft mc = Minecraft.getMinecraft();
	private final TimerUtils timer = new TimerUtils();
	
	private float x;
    private float y;
    private final float size;
    
    // ✅ FIX: Dùng static Random thay vì tạo mới
    private final float ySpeed = RANDOM.nextInt(MAX_SPEED);
    private final float xSpeed = RANDOM.nextInt(MAX_SPEED);

    public Particle(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = genRandomSize();
    }

    // ✅ Extract to method với constant
    private float lint1(float f) {
        return INTERPOLATION_P1_START * (1.0F - f) + INTERPOLATION_P1_END * f;
    }

    private float lint2(float f) {
        return INTERPOLATION_P2_START + f * (INTERPOLATION_P2_END - INTERPOLATION_P2_START);
    }

    public void connect(float x, float y) {
        RenderUtils.connectPoints(getX(), getY(), x, y);
    }

    public void interpolation() {
        for (int n = 0; n <= INTERPOLATION_STEPS; ++n) {
            final float f = n / (float) INTERPOLATION_STEPS;
            final float p1 = lint1(f);
            final float p2 = lint2(f);

            if (p1 != p2) {
                y -= f;
                x -= f;
            }
        }
    }

    public void fall() {
    	if (mc == null) return;
    	
		ScaledResolution sr = new ScaledResolution(mc);
		
        y = y + ySpeed;
        x = x + xSpeed;

        // ✅ Bounds checking
        if (y > mc.displayHeight) {
            y = 1;
        }

        if (x > mc.displayWidth) {
            x = 1;
        }

        if (x < 1) {
            x = sr.getScaledWidth();
        }

        if (y < 1) {
            y = sr.getScaledHeight();
        }
    }

    // ✅ FIX: Dùng static Random, rõ ràng hơn
    private static float genRandomSize() {
        return MIN_RANDOM_SIZE + RANDOM.nextFloat() * (MAX_RANDOM_SIZE - MIN_RANDOM_SIZE) + BASE_SIZE_OFFSET;
    }

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getSize() {
		return size;
	}

	public float getySpeed() {
		return ySpeed;
	}

	public float getxSpeed() {
		return xSpeed;
	}

	public TimerUtils getTimer() {
		return timer;
	}
}