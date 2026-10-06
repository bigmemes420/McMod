package com.example.client.config;

import com.example.ExampleMod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Persistent Rooty Menu color preferences. Saved under
 * {@code <gameDir>/config/modid-menu-theme.properties}.
 */
public final class MenuTheme {
	private static MenuTheme instance;

	public int topBar = 0xCC101018;
	public int topBarLine = 0xFF404050;
	public int title = 0xFFFFFFFF;
	public int panelHint = 0xFFAAAAAA;

	public int tabFill = 0xFF252530;
	public int tabOutline = 0xFF707080;
	public int tabText = 0xFFDDDDDD;
	public int tabHoverFill = 0xFF3A3A48;
	public int tabHoverOutline = 0xFFB0B0C0;
	public int tabPressedFill = 0xFF1A3A5A;
	public int tabPressedOutline = 0xFF7EC8FF;
	public int tabSelectedFill = 0xFF2E5A8A;
	public int tabSelectedOutline = 0xFF8EC8FF;
	public int tabSelectedText = 0xFFFFFFFF;
	public int tabDisabledFill = 0xFF2A2A2A;
	public int tabDisabledOutline = 0xFF555555;
	public int tabDisabledText = 0xFF888888;

	public int capsuleFill = 0xFF1E1E28;
	public int capsuleOutline = 0xFFE0E0E0;
	public int capsuleText = 0xFFE8E8E8;
	public int capsuleHoverFill = 0xFF2A4A6A;
	public int capsuleHoverOutline = 0xFFAAD4FF;
	public int capsulePressedFill = 0xFF1A3A5A;
	public int capsulePressedOutline = 0xFF7EC8FF;
	public int capsuleDisabledFill = 0xFF2A2A2A;
	public int capsuleDisabledOutline = 0xFF555555;
	public int capsuleDisabledText = 0xFF888888;

	/** Left-circle fill when a toggle capsule is ON. */
	public int accentOn = 0xFF3DDC84;

	private MenuTheme() {
	}

	public static MenuTheme get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static void reload() {
		instance = load();
	}

	public Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(ExampleMod.MOD_ID + "-menu-theme.properties");
	}

	public void save() {
		Properties props = new Properties();
		for (Map.Entry<String, Integer> entry : asMap().entrySet()) {
			props.setProperty(entry.getKey(), toHex(entry.getValue()));
		}
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				props.store(writer, "Rooty Menu theme colors (ARGB hex)");
			}
		} catch (IOException e) {
			ExampleMod.LOGGER.warn("Failed to save menu theme: {}", e.toString());
		}
	}

	public void resetDefaults() {
		instance = new MenuTheme();
		instance.save();
	}

	public void set(String key, int argb) {
		switch (key) {
			case "topBar" -> topBar = argb;
			case "topBarLine" -> topBarLine = argb;
			case "title" -> title = argb;
			case "panelHint" -> panelHint = argb;
			case "tabFill" -> tabFill = argb;
			case "tabOutline" -> tabOutline = argb;
			case "tabText" -> tabText = argb;
			case "tabHoverFill" -> tabHoverFill = argb;
			case "tabHoverOutline" -> tabHoverOutline = argb;
			case "tabSelectedFill" -> tabSelectedFill = argb;
			case "tabSelectedOutline" -> tabSelectedOutline = argb;
			case "tabSelectedText" -> tabSelectedText = argb;
			case "capsuleFill" -> capsuleFill = argb;
			case "capsuleOutline" -> capsuleOutline = argb;
			case "capsuleText" -> capsuleText = argb;
			case "capsuleHoverFill" -> capsuleHoverFill = argb;
			case "capsuleHoverOutline" -> capsuleHoverOutline = argb;
			case "accentOn" -> accentOn = argb;
			default -> {
			}
		}
	}

	public Map<String, Integer> asMap() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("topBar", topBar);
		map.put("topBarLine", topBarLine);
		map.put("title", title);
		map.put("panelHint", panelHint);
		map.put("tabFill", tabFill);
		map.put("tabOutline", tabOutline);
		map.put("tabText", tabText);
		map.put("tabHoverFill", tabHoverFill);
		map.put("tabHoverOutline", tabHoverOutline);
		map.put("tabSelectedFill", tabSelectedFill);
		map.put("tabSelectedOutline", tabSelectedOutline);
		map.put("tabSelectedText", tabSelectedText);
		map.put("capsuleFill", capsuleFill);
		map.put("capsuleOutline", capsuleOutline);
		map.put("capsuleText", capsuleText);
		map.put("capsuleHoverFill", capsuleHoverFill);
		map.put("capsuleHoverOutline", capsuleHoverOutline);
		map.put("accentOn", accentOn);
		return map;
	}

	public static String displayName(String key) {
		return switch (key) {
			case "topBar" -> "Top Bar";
			case "topBarLine" -> "Top Bar Line";
			case "title" -> "Title";
			case "panelHint" -> "Panel Hint";
			case "tabFill" -> "Tab Fill";
			case "tabOutline" -> "Tab Outline";
			case "tabText" -> "Tab Text";
			case "tabHoverFill" -> "Tab Hover Fill";
			case "tabHoverOutline" -> "Tab Hover Outline";
			case "tabSelectedFill" -> "Tab Selected Fill";
			case "tabSelectedOutline" -> "Tab Selected Outline";
			case "tabSelectedText" -> "Tab Selected Text";
			case "capsuleFill" -> "Capsule Fill";
			case "capsuleOutline" -> "Capsule Outline";
			case "capsuleText" -> "Capsule Text";
			case "capsuleHoverFill" -> "Capsule Hover Fill";
			case "capsuleHoverOutline" -> "Capsule Hover Outline";
			case "accentOn" -> "Accent (ON)";
			default -> key;
		};
	}

	private static MenuTheme load() {
		MenuTheme theme = new MenuTheme();
		Path path = theme.configPath();
		if (!Files.isRegularFile(path)) {
			return theme;
		}
		Properties props = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			props.load(reader);
		} catch (IOException e) {
			ExampleMod.LOGGER.warn("Failed to load menu theme: {}", e.toString());
			return theme;
		}
		for (String key : theme.asMap().keySet()) {
			String raw = props.getProperty(key);
			if (raw != null) {
				Integer parsed = parseHex(raw.trim());
				if (parsed != null) {
					theme.set(key, parsed);
				}
			}
		}
		return theme;
	}

	public static String toHex(int argb) {
		return String.format("%08X", argb);
	}

	public static Integer parseHex(String raw) {
		String s = raw.startsWith("#") ? raw.substring(1) : raw;
		if (s.startsWith("0x") || s.startsWith("0X")) {
			s = s.substring(2);
		}
		try {
			long value = Long.parseUnsignedLong(s, 16);
			if (s.length() <= 6) {
				value |= 0xFF000000L;
			}
			return (int) value;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
