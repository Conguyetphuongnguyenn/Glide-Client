package me.eldodebug.soar.management.mods.impl;

import java.util.Calendar;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;

public class CalendarMod extends HUDMod {

	private static final int HEIGHT_SMALL = 97;
	private static final int HEIGHT_LARGE = 110;
	private static final int CALENDAR_WIDTH = 100;
	private static final float DAY_OFFSET = 13.4F;
	private static final int WEEKS_THRESHOLD = 5;
	
	private static final String[] DAY_OF_WEEK = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
	
	private int height;
	private Calendar cachedCalendar;
	private int lastMonth = -1;
	private String cachedMonthName = "";
	
	public CalendarMod() {
		super(TranslateText.CALENDAR, TranslateText.CALENDAR_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		if (nvg == null) return;
		
		nvg.setupAndDraw(this::drawNanoVG);
	}
	
	private void drawNanoVG() {
		if (cachedCalendar == null) {
			cachedCalendar = Calendar.getInstance();
		} else {
			cachedCalendar.setTimeInMillis(System.currentTimeMillis());
		}
		
		AccentColor currentColor = Glide.getInstance().getColorManager().getCurrentColor();
		
		float offsetX = 0;
		float offsetY = 0;
		int index = 1;
		int weekIndex = 0;
		
		int year = cachedCalendar.get(Calendar.YEAR);
		int month = cachedCalendar.get(Calendar.MONTH);
		int day = cachedCalendar.get(Calendar.DAY_OF_MONTH);
		int maxDay = cachedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (month != lastMonth) {
			cachedMonthName = getMonthByNumber(month);
			lastMonth = month;
		}
		
		Calendar firstDayCalendar = (Calendar) cachedCalendar.clone();
		firstDayCalendar.set(year, month, 1);
		
		this.drawBackground(CALENDAR_WIDTH, height);
		this.drawText(cachedMonthName + " " + year, 6, 6, 11, getHudFont(2));
		
		offsetX = 0;
		for (String s : DAY_OF_WEEK) {
			this.drawText(s, 6 + offsetX, 22, 6.5F, getHudFont(2));
			offsetX += DAY_OFFSET;
		}
		
		offsetX = 0;
		offsetY = 30.5F;
		index = firstDayCalendar.get(Calendar.DAY_OF_WEEK);
		offsetX = (index - 1) * DAY_OFFSET;
		
		for (int i = 1; i <= maxDay; i++) {
			if (i == day) {
				this.drawRoundedRect(4.5F + offsetX, offsetY, 10F, 10F, 5F);
			}
			
			this.drawCenteredText(
				String.valueOf(i), 
				10 + offsetX, 
				offsetY + 2.5F, 
				6, 
				getHudFont(1), 
				i == day ? currentColor.getInterpolateColor() : this.getFontColor()
			);
			
			offsetX += DAY_OFFSET;
			
			if (index % 7 == 0 && i != maxDay) {
				offsetY += DAY_OFFSET;
				offsetX = 0;
				weekIndex++;
			}
			
			index++;
		}
		
		height = weekIndex < WEEKS_THRESHOLD ? HEIGHT_SMALL : HEIGHT_LARGE;
		
		this.setWidth(CALENDAR_WIDTH);
		this.setHeight(height);
	}
	
	private String getMonthByNumber(int month) {
		switch (month) {
			case 0:
				return "January";
			case 1:
				return "February";
			case 2:
				return "March";
			case 3:
				return "April";
			case 4:
				return "May";
			case 5:
				return "June";
			case 6:
				return "July";
			case 7:
				return "August";
			case 8:
				return "September";
			case 9:
				return "October";
			case 10:
				return "November";
			case 11:
				return "December";
			default:
				throw new IllegalArgumentException("Invalid month: " + month);
		}
	}
}