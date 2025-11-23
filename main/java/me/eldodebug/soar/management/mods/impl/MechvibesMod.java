package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.impl.mechibes.SoundKey;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.utils.Multithreading;
import me.eldodebug.soar.utils.RandomUtils;
import me.eldodebug.soar.utils.Sound;
import net.minecraft.util.ResourceLocation;

public class MechvibesMod extends Mod {

    private static final int KEYBOARD_KEY_COUNT = 256;
    private static final ResourceLocation MOUSE_SOUND = new ResourceLocation("soar/mechvibes/mouse.wav");
    private static final float EPSILON = 0.001F;
    
    private enum KeyboardType {
        NK_CREAM("nk_cream"), MX_BLUE("mx_blue"), MX_SILVER("mx_silver"), RAZER_GREEN("razer_green"),
        HYPERX_AQUA("hyperX_aqua"), MX_BLACK("mx_black"), TOPRE_PURPLE("topre_purple");
        private final String id;
        KeyboardType(String id) { this.id = id; }
        public String getId() { return id; }
    }
    
    private Sound mouseLeftSound;
    private Sound mouseRightSound;
    private final Map<Integer, SoundKey> keyMap = new ConcurrentHashMap<>(KEYBOARD_KEY_COUNT);
    private final boolean[] keyStates = new boolean[KEYBOARD_KEY_COUNT];
    
    private float cachedKeyboardVolume;
    private KeyboardType cachedKeyboardType;
    private float cachedMouseVolume;
    private boolean mouseLeftPress, mouseRightPress;
    private volatile boolean loaded;
    private volatile boolean isLoadingAsync;
    
    private BooleanSetting keyboardSetting;
    private ComboSetting keyTypeSetting;
    private NumberSetting keyboardVolumeSetting;
    private BooleanSetting mouseSetting;
    private NumberSetting mouseVolumeSetting;
    
    public MechvibesMod() {
        super(TranslateText.MECHVIBES, TranslateText.MECHVIBES_DESCRIPTION, ModCategory.OTHER);
    }
    
    @Override
    public void setup() {
        this.keyboardSetting = new BooleanSetting(TranslateText.KEYBOARD, this, true);
        this.keyTypeSetting = new ComboSetting(TranslateText.TYPE, this, TranslateText.NK_CREAM, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.NK_CREAM), new Option(TranslateText.MX_BLUE),
                new Option(TranslateText.MX_SILVER), new Option(TranslateText.RAZER_GREEN),
                new Option(TranslateText.HYPERX_AQUA), new Option(TranslateText.MX_BLACK), 
                new Option(TranslateText.TOPRE_PURPLE)
            ))
        );
        this.keyboardVolumeSetting = new NumberSetting(TranslateText.KEYBOARD_VOLUME, this, 0.5, 0.0, 1.0, false);
        this.mouseSetting = new BooleanSetting(TranslateText.MOUSE, this, true);
        this.mouseVolumeSetting = new NumberSetting(TranslateText.MOUSE_VOLUME, this, 0.5, 0.0, 1.0, false);
        loaded = false;
        isLoadingAsync = false;
    }
    
    @Override
    public void onEnable() {
        if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
            this.setToggled(false);
            return;
        }
        super.onEnable();
        Glide.getInstance().getEventManager().register(this);
        KeyboardType type = getKeyboardType(keyTypeSetting.getOption());
        loadSoundsAsync(type);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        Glide.getInstance().getEventManager().unregister(this);
        keyMap.clear();
        Arrays.fill(keyStates, false);
        mouseLeftSound = null;
        mouseRightSound = null;
        loaded = false;
        isLoadingAsync = false;
        mouseLeftPress = false;
        mouseRightPress = false;
        cachedKeyboardType = null;
        cachedKeyboardVolume = 0;
        cachedMouseVolume = 0;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (!this.isToggled()) return;
        if (!loaded) return;
        
        KeyboardType currentType = getKeyboardType(keyTypeSetting.getOption());
        if (cachedKeyboardType != currentType) {
            cachedKeyboardType = currentType;
            reloadKeyboardSounds(currentType);
        }
        updateVolumes();
        if (keyboardSetting.isToggled()) handleKeyboardInput();
        if (mouseSetting.isToggled()) handleMouseInput();
    }
    
    private void updateVolumes() {
        float newKeyboardVol = keyboardVolumeSetting.getValueFloat();
        if (Math.abs(cachedKeyboardVolume - newKeyboardVol) > EPSILON) {
            cachedKeyboardVolume = newKeyboardVol;
            for (SoundKey key : keyMap.values()) if (key != null) key.setVolume(cachedKeyboardVolume);
        }
        float newMouseVol = mouseVolumeSetting.getValueFloat();
        if (Math.abs(cachedMouseVolume - newMouseVol) > EPSILON) {
            cachedMouseVolume = newMouseVol;
            if (mouseLeftSound != null) mouseLeftSound.setVolume(cachedMouseVolume);
            if (mouseRightSound != null) mouseRightSound.setVolume(cachedMouseVolume);
        }
    }
    
    private void handleKeyboardInput() {
        if (!Keyboard.isCreated()) return;
        try {
            for (Map.Entry<Integer, SoundKey> entry : keyMap.entrySet()) {
                Integer keyCode = entry.getKey();
                if (keyCode == null || keyCode < 0 || keyCode >= keyStates.length) continue;
                int key = keyCode;
                boolean isDown = Keyboard.isKeyDown(key);
                boolean wasDown = keyStates[key];
                if (isDown != wasDown) {
                    SoundKey soundKey = entry.getValue();
                    if (soundKey != null) {
                        if (isDown && !soundKey.isPressed()) {
                            soundKey.play();
                            soundKey.setPressed(true);
                        } else if (!isDown) soundKey.setPressed(false);
                    }
                    keyStates[key] = isDown;
                }
            }
        } catch (Exception e) {}
    }
    
    private void handleMouseInput() {
        if (!Mouse.isCreated()) return;
        try {
            boolean leftDown = Mouse.isButtonDown(0);
            if (leftDown != mouseLeftPress) {
                if (leftDown && mouseLeftSound != null) mouseLeftSound.play();
                mouseLeftPress = leftDown;
            }
            boolean rightDown = Mouse.isButtonDown(1);
            if (rightDown != mouseRightPress) {
                if (rightDown && mouseRightSound != null) mouseRightSound.play();
                mouseRightPress = rightDown;
            }
        } catch (Exception e) {}
    }
    
    private void loadSoundsAsync(KeyboardType type) {
        if (isLoadingAsync) return;
        isLoadingAsync = true;
        Multithreading.runAsync(() -> {
            try {
                loadKeyboardSounds(type.getId());
                Sound leftSound = new Sound();
                Sound rightSound = new Sound();
                leftSound.loadClip(MOUSE_SOUND);
                rightSound.loadClip(MOUSE_SOUND);
                mouseLeftSound = leftSound;
                mouseRightSound = rightSound;
                cachedKeyboardType = type;
                loaded = true;
            } catch (Exception e) { loaded = false; } 
            finally { isLoadingAsync = false; }
        });
    }
    
    private void reloadKeyboardSounds(KeyboardType type) {
        if (isLoadingAsync) return;
        keyMap.clear();
        Arrays.fill(keyStates, false);
        loaded = false;
        loadSoundsAsync(type);
    }
    
    private void loadKeyboardSounds(String type) {
        keyMap.put(Keyboard.KEY_TAB, new SoundKey(type, "tab"));
        keyMap.put(Keyboard.KEY_BACK, new SoundKey(type, "backspace"));
        keyMap.put(Keyboard.KEY_CAPITAL, new SoundKey(type, "capslock"));
        keyMap.put(Keyboard.KEY_RETURN, new SoundKey(type, "enter"));
        keyMap.put(Keyboard.KEY_SPACE, new SoundKey(type, "space"));
        keyMap.put(Keyboard.KEY_LSHIFT, new SoundKey(type, "shift"));
        keyMap.put(Keyboard.KEY_RSHIFT, new SoundKey(type, "shift"));
        for (int keyCode = 0; keyCode < KEYBOARD_KEY_COUNT; keyCode++) {
            if (!keyMap.containsKey(keyCode)) keyMap.put(keyCode, new SoundKey(type, String.valueOf(RandomUtils.getRandomInt(1, 5))));
        }
    }
    
    private KeyboardType getKeyboardType(Option option) {
        if (option == null) return KeyboardType.NK_CREAM;
        TranslateText type = option.getTranslate();
        if (type.equals(TranslateText.NK_CREAM)) return KeyboardType.NK_CREAM;
        if (type.equals(TranslateText.MX_BLUE)) return KeyboardType.MX_BLUE;
        if (type.equals(TranslateText.MX_SILVER)) return KeyboardType.MX_SILVER;
        if (type.equals(TranslateText.RAZER_GREEN)) return KeyboardType.RAZER_GREEN;
        if (type.equals(TranslateText.HYPERX_AQUA)) return KeyboardType.HYPERX_AQUA;
        if (type.equals(TranslateText.MX_BLACK)) return KeyboardType.MX_BLACK;
        if (type.equals(TranslateText.TOPRE_PURPLE)) return KeyboardType.TOPRE_PURPLE;
        return KeyboardType.NK_CREAM;
    }
}