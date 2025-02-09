package drivers.turnrunning.applets;

import java.util.List;
import drivers.common.cli.Applet;

import org.jetbrains.annotations.Nullable;

public interface TurnApplet extends Applet {
	@Override
	List<String> getCommands();

	@Override
	String getDescription();

	@Nullable String run();

	@Override
	default void invoke() {
		run();
	}

	default String inHours(final int minutes) {
		if (minutes < 0) {
			return "negative " + inHours(-minutes);
		} else if (minutes == 0) {
			return "no time";
		} else if (minutes == 1) {
			return "1 minute";
		} else if (minutes < 60) {
			return String.format("%d minutes", minutes);
		} else if (minutes < 120) {
			return "1 hour, " + inHours(minutes % 60);
		} else if (minutes % 60 == 0) {
			return String.format("%d hours", minutes / 60);
		} else {
			return String.format("%d hours, %s", minutes / 60, inHours(minutes % 60));
		}
	}
}
