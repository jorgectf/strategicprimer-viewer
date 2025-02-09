package drivers.turnrunning.applets;

import drivers.common.cli.ICLIHelper;

import common.map.fixtures.mobile.IUnit;
import common.map.fixtures.mobile.IWorker;

import common.map.fixtures.IResourcePile;

import java.math.BigDecimal;

import static lovelace.util.Decimalize.decimalize;

import drivers.turnrunning.ITurnRunningModel;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * We <em>deliberately</em> do not make a factory annotated for service discovery.
 */
public class ConsumptionApplet extends AbstractTurnApplet {
	private final ITurnRunningModel model;
	private final ICLIHelper cli;

	public ConsumptionApplet(final ITurnRunningModel model, final ICLIHelper cli) {
		super(model, cli);
		this.model = model;
		this.cli = cli;
		turn = model.getMap().getCurrentTurn();
		unit = model.getSelectedUnit();
	}

	private int turn;

	public int getTurn() {
		return turn;
	}

	public void setTurn(final int turn) {
		this.turn = turn;
	}

	private @Nullable IUnit unit;

	public @Nullable IUnit getUnit() {
		return unit;
	}

	public void setUnit(final @Nullable IUnit unit) {
		this.unit = unit;
	}

	private static final List<String> COMMANDS = Collections.singletonList("consumption");

	@Override
	public List<String> getCommands() {
		return COMMANDS;
	}

	@Override
	public String getDescription() {
		return "Determine the food consumed by a unit.";
	}

	private static String describeFood(final IResourcePile food) {
		if (food.getCreated() < 0) {
			return String.format("%.2f %s of %s", food.getQuantity().number().doubleValue(),
				food.getQuantity().units(), food.getContents());
		} else {
			return String.format("%.2f %s of %s (turn #%d)",  food.getQuantity().number().doubleValue(),
				food.getQuantity().units(), food.getContents(), food.getCreated());
		}
	}

	@Override
	public @Nullable String run() {
		final IUnit localUnit = unit;
		if (localUnit == null) {
			return null;
		}
		final long workers = localUnit.stream().filter(IWorker.class::isInstance).count();
		BigDecimal remainingConsumption = new BigDecimal(4 * workers);
		while (remainingConsumption.signum() > 0) { // TODO: extract loop body as a function?
			cli.println(String.format("%.1f pounds of consumption unaccounted-for",
				remainingConsumption.doubleValue()));
			final IResourcePile food = chooseFromList(getFoodFor(localUnit.owner(), turn),
				"Food stocks owned by player:", "No food stocks found", "Food to consume from:",
				ICLIHelper.ListChoiceBehavior.ALWAYS_PROMPT, ConsumptionApplet::describeFood); // TODO: should only count food *in the same place* (but unit movement away from HQ should ask user how much food to take along, and to choose what food in a similar manner to this)
			if (food == null) {
				return null;
			}
			if (food.getQuantity().number().doubleValue() <= remainingConsumption.doubleValue()) {
				final Boolean resp = cli.inputBooleanInSeries(String.format("Consume all of the %s?",
					food.getContents()), "consume-all-of");
				if (resp == null) {
					return null;
				} else if (resp) {
					model.reduceResourceBy(food, decimalize(food.getQuantity().number()),
						localUnit.owner());
					remainingConsumption = remainingConsumption.subtract(
						decimalize(food.getQuantity().number()));
					continue;
				} else { // TODO: extract this as a function?
					final BigDecimal amountToConsume = cli.inputDecimal(String.format(
						"How many pounds of the %s to consume:", food.getContents()));
					if (amountToConsume == null) {
						return null;
					}
					final BigDecimal minuend = amountToConsume.min(decimalize(
						food.getQuantity().number()));
					model.reduceResourceBy(food, minuend, localUnit.owner());
					remainingConsumption = remainingConsumption.subtract(minuend);
					continue;
				}
			} // else
			final Boolean resp = cli.inputBooleanInSeries(String.format("Eat all remaining %s from the %s?",
				remainingConsumption, food.getContents()), "all-remaining");
			if (resp == null) {
				return null;
			} else if (resp) {
				model.reduceResourceBy(food, remainingConsumption, localUnit.owner());
				remainingConsumption = decimalize(0);
			} else { // TODO: extract this as a function?
				final BigDecimal amountToConsume = cli.inputDecimal(String.format(
					"How many pounds of the %s to consume:", food.getContents()));
				if (amountToConsume == null) {
					return null;
				} else if (amountToConsume.compareTo(remainingConsumption) > 0) {
					model.reduceResourceBy(food, remainingConsumption, localUnit.owner());
					remainingConsumption = decimalize(0);
					continue;
				} else {
					model.reduceResourceBy(food, amountToConsume, localUnit.owner());
					remainingConsumption = remainingConsumption.subtract(amountToConsume);
					continue;
				}
			}
		}
		return ""; // FIXME: Optionally report on what workers ate
	}
}
