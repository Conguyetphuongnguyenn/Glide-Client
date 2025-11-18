package me.eldodebug.soar.utils.animation.md3;

public class MD3Animation {
	
	private static final float STANDARD_DURATION = 300.0F;
	private static final float EMPHASIZED_DURATION = 500.0F;
	
	private float value;
	private float target;
	private long startTime;
	private float duration;
	private Easing easing;
	private boolean running;
	
	public MD3Animation(Easing easing, float duration) {
		this.easing = easing;
		this.duration = duration;
		this.value = 0.0F;
		this.target = 0.0F;
		this.running = false;
	}
	
	public void animateTo(float target) {
		this.target = target;
		this.startTime = System.currentTimeMillis();
		this.running = true;
	}
	
	public void update() {
		if (!running) return;
		
		long elapsed = System.currentTimeMillis() - startTime;
		float progress = Math.min(1.0F, elapsed / duration);
		
		if (progress >= 1.0F) {
			value = target;
			running = false;
			return;
		}
		
		float eased = easing.ease(progress);
		value = lerp(value, target, eased);
	}
	
	private float lerp(float start, float end, float t) {
		return start + (end - start) * t;
	}
	
	public float getValue() {
		update();
		return value;
	}
	
	public void setValue(float value) {
		this.value = value;
		this.target = value;
		this.running = false;
	}
	
	public boolean isDone() {
		return !running;
	}
	
	public static MD3Animation standard() {
		return new MD3Animation(Easing.STANDARD, STANDARD_DURATION);
	}
	
	public static MD3Animation emphasized() {
		return new MD3Animation(Easing.EMPHASIZED, EMPHASIZED_DURATION);
	}
	
	public static MD3Animation decelerate() {
		return new MD3Animation(Easing.DECELERATE, STANDARD_DURATION);
	}
	
	public enum Easing {
		STANDARD,
		EMPHASIZED,
		DECELERATE;
		
		public float ease(float t) {
			switch (this) {
				case STANDARD:
					return cubicBezier(t, 0.2F, 0.0F, 0.0F, 1.0F);
				case EMPHASIZED:
					return cubicBezier(t, 0.05F, 0.7F, 0.1F, 1.0F);
				case DECELERATE:
					return cubicBezier(t, 0.0F, 0.0F, 0.2F, 1.0F);
				default:
					return t;
			}
		}
		
		private float cubicBezier(float t, float p0, float p1, float p2, float p3) {
			float u = 1.0F - t;
			float tt = t * t;
			float uu = u * u;
			float uuu = uu * u;
			float ttt = tt * t;
			
			return uuu * p0 + 3 * uu * t * p1 + 3 * u * tt * p2 + ttt * p3;
		}
	}
}