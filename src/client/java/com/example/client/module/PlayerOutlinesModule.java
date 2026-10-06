package com.example.client.module;

/**
 * @deprecated Replaced by {@link PlayerEspModule}. Kept as a thin alias so old
 * config keys can migrate during one release.
 */
@Deprecated
public final class PlayerOutlinesModule {
	private PlayerOutlinesModule() {
	}

	public static boolean isEnabled() {
		return PlayerEspModule.isEnabled();
	}

	public static void setEnabled(boolean value) {
		PlayerEspModule.setEnabled(value);
	}

	public static void loadEnabled(boolean value) {
		PlayerEspModule.loadEnabled(value);
	}

	public static void toggle() {
		PlayerEspModule.setEnabled(!PlayerEspModule.isEnabled());
	}
}
