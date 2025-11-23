package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;

public class RawInputMod extends Mod {

	private static RawInputMod instance;
	private ArrayList<Mouse> mouseList = new ArrayList<Mouse>();
	
	// ✅ CHANGED: Thread -> RawInputThread
	private RawInputThread thread;
	
	private boolean initialised, available;
	private volatile float dx, dy;
	private volatile boolean running;
	
	public RawInputMod() {
		super(TranslateText.RAW_INPUT, TranslateText.RAW_INPUT_DESCRIPTION, ModCategory.OTHER);
		instance = this;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		if(!initialised) {
			initialised = true;
			available = true;
			try {
				ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
				if (env.isSupported()) {
					for (Controller controller : env.getControllers()) {
						if(controller instanceof Mouse) mouseList.add((Mouse) controller);
					}
				} else available = false;
			} catch(Exception e) { available = false; }
		}
		
		running = true;
		
		// ✅ CHANGED: new Thread -> new RawInputThread
		thread = new RawInputThread();
		thread.setName("RawInput Thread");
		thread.start();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		running = false;
		try {
			if (thread != null && thread.isAlive()) {
				thread.join(100);
			}
		} catch (InterruptedException e) {}
	}
	
	// ✅ EXISTING METHODS
	public static RawInputMod getInstance() { return instance; }
	public float getDx() { return dx; }
	public float getDy() { return dy; }
	public boolean isAvailable() { return available; }
	
	// ✅ ADDED: getThread() method
	/**
	 * Get the raw input thread
	 * @return RawInputThread instance or null
	 */
	public RawInputThread getThread() {
		return this.thread;
	}
	
	public void reset() {
		this.dx = 0;
		this.dy = 0;
	}
	
	// ✅ ADDED: Inner class RawInputThread
	/**
	 * Custom thread class for raw input handling
	 */
	public class RawInputThread extends Thread {
		
		@Override
		public void run() {
			while(running) {
				available = !mouseList.isEmpty();
				for (Mouse mouse : mouseList) {
					if (!mouse.poll()) continue;
					float dx = mouse.getX().getPollData();
					float dy = mouse.getY().getPollData();
					if (org.lwjgl.input.Mouse.isGrabbed()) {
						RawInputMod.this.dx += dx;
						RawInputMod.this.dy += dy;
					}
				}
				try { 
					Thread.sleep(1); 
				} catch (InterruptedException e) {
					break; // Exit on interrupt
				}
			}
		}
		
		/**
		 * Reset mouse delta values
		 */
		public void reset() {
			RawInputMod.this.reset();
		}
	}
}