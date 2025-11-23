package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRenderScoreboard;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.GlUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

public class ScoreboardMod extends HUDMod {

	private static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);
	private static final Color COLOR_BACKGROUND = ColorUtils.getColorByInt(1342177280);
	private static final Color COLOR_HEADER = ColorUtils.getColorByInt(1610612736);
	private static final int COLOR_TEXT = 553648127;
	private static final int MAX_SCORES = 15;
	
	private ScoreObjective objective;
	private BooleanSetting backgroundSetting;
	private BooleanSetting numberSetting;
	private BooleanSetting shadowSetting;
	private final StringBuilder scoreBuilder = new StringBuilder(32);
	
	public ScoreboardMod() {
		super(TranslateText.SCOREBOARD, TranslateText.SCOREBOARD_DESCRIPTION);
	}
	
	@Override
	public void setup() {
		this.backgroundSetting = new BooleanSetting(TranslateText.BACKGROUND, this, true);
		this.numberSetting = new BooleanSetting(TranslateText.NUMBER, this, true);
		this.shadowSetting = new BooleanSetting(TranslateText.SHADOW, this, false);
	}
	
	@Override
	public void onEnable() {
		if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
			this.setToggled(false);
			return;
		}
		super.onEnable();
		Glide.getInstance().getEventManager().register(this);
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		Glide.getInstance().getEventManager().unregister(this);
		objective = null;
		scoreBuilder.setLength(0);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		if (!this.isToggled()) return;
		if (mc.isSingleplayer()) { objective = null; return; }
		if (objective == null) return;
		Scoreboard scoreboard = objective.getScoreboard();
		if (scoreboard == null) return;
		
		Collection<Score> allScores = scoreboard.getSortedScores(objective);
		if (allScores == null || allScores.isEmpty()) return;
		
		List<Score> filteredScores = Lists.newArrayList(Iterables.filter(allScores, score -> score != null && score.getPlayerName() != null && !score.getPlayerName().startsWith("#")));
		if (filteredScores.isEmpty()) return;
		Collections.reverse(filteredScores);
		
		Collection<Score> displayScores;
		if (filteredScores.size() > MAX_SCORES) displayScores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - MAX_SCORES));
		else displayScores = filteredScores;
		
		int maxWidth = fr.getStringWidth(objective.getDisplayName());
		boolean showNumbers = numberSetting.isToggled();
		
		for (Score score : displayScores) {
			ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
			String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
			int width = fr.getStringWidth(name);
			if (showNumbers) {
				scoreBuilder.setLength(0);
				scoreBuilder.append(": ").append(EnumChatFormatting.RED).append(score.getScorePoints());
				width += fr.getStringWidth(scoreBuilder.toString());
			}
			maxWidth = Math.max(maxWidth, width);
		}
		
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		if (nvg != null && shadowSetting.isToggled()) {
			nvg.setupAndDraw(() -> this.drawShadow(0, 0, this.getWidth() / this.getScale(), this.getHeight() / this.getScale(), 0));
		}
		
		int posX = this.getX();
		int posY = this.getY();
		int fontHeight = fr.FONT_HEIGHT;
		int padding = 2;
		int barWidth = maxWidth + 4;
		Color bgColor = backgroundSetting.isToggled() ? COLOR_BACKGROUND : COLOR_TRANSPARENT;
		Color headerColor = backgroundSetting.isToggled() ? COLOR_HEADER : COLOR_TRANSPARENT;
		
		GlUtils.startScale(posX, posY, this.getScale());
		int index = 0;
		
		for (Score score : displayScores) {
			index++;
			int yOffset = index * fontHeight + 1;
			ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
			String playerName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
			RenderUtils.drawRect(posX, posY + yOffset, barWidth, fontHeight, bgColor);
			fr.drawString(playerName, posX + padding, posY + yOffset, COLOR_TEXT);
			
			if (showNumbers) {
				scoreBuilder.setLength(0);
				scoreBuilder.append(EnumChatFormatting.RED).append(score.getScorePoints());
				String scoreText = scoreBuilder.toString();
				int scoreX = posX + barWidth - padding - fr.getStringWidth(scoreText);
				fr.drawString(scoreText, scoreX, posY + yOffset, COLOR_TEXT);
			}
		}
		
		if (index > 0) {
			String displayName = objective.getDisplayName();
			RenderUtils.drawRect(posX, posY, barWidth, fontHeight, headerColor);
			RenderUtils.drawRect(posX, posY + fontHeight, barWidth, 1, headerColor);
			int titleX = posX + padding + maxWidth / 2 - fr.getStringWidth(displayName) / 2;
			fr.drawString(displayName, titleX, posY + 1, COLOR_TEXT);
		}
		
		GlUtils.stopScale();
		this.setWidth(barWidth);
		this.setHeight((index * fontHeight) + 10);
	}
	
	@EventTarget
	public void onRenderScoreboard(EventRenderScoreboard event) {
		if (!this.isToggled()) return;
		event.setCancelled(true);
		objective = event.getObjective();
	}
}