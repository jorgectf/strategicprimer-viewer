package drivers.turnrunning.applets;

import common.idreg.IDRegistrar;
import common.map.Point;
import common.map.fixtures.IResourcePile;
import common.map.fixtures.mobile.IUnit;
import common.map.fixtures.towns.IFortress;
import drivers.common.cli.ICLIHelper;
import drivers.exploration.ExplorationCLIHelper;
import drivers.turnrunning.ITurnRunningModel;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

import static lovelace.util.Decimalize.decimalize;

/* package */ class MovementApplet extends AbstractTurnApplet {
	public MovementApplet(final ITurnRunningModel model, final ICLIHelper cli, final IDRegistrar idf) {
		super(model, cli);
		this.model = model;
		this.cli = cli;
		this.idf = idf;
		explorationCLI = new ExplorationCLIHelper(model, cli);
		model.addMovementCostListener(explorationCLI);
	}

	private final ITurnRunningModel model;
	private final ICLIHelper cli;
	private final IDRegistrar idf;
	private final ExplorationCLIHelper explorationCLI;

	private static final List<String> COMMANDS = Collections.singletonList("move");

	@Override
	public List<String> getCommands() {
		return COMMANDS;
	}

	@Override
	public String getDescription() {
		return "move a unit";
	}

	private void packFood(final @Nullable IFortress fortress, final IUnit unit) {
		if (fortress == null) {
			return;
		}
		final List<IResourcePile> resources = fortress.stream()
				.filter(IResourcePile.class::isInstance).map(IResourcePile.class::cast).collect(Collectors.toList());
		while (true) {
			final IResourcePile chosen =
				chooseFromList(resources, String.format("Resources in %s:", fortress.getName()), "No resources in fortress.",
					"Resource to take (from):", ICLIHelper.ListChoiceBehavior.ALWAYS_PROMPT);
			if (chosen == null) {
				break;
			}
			final Boolean takeAll = cli.inputBooleanInSeries("Take it all?");
			if (takeAll == null) {
				return; // TODO: Find a way to propagate the EOF to caller
			}
			else if (takeAll) {
				model.transferResource(chosen, unit, decimalize(chosen.getQuantity().number()),
					idf::createID);
				resources.remove(chosen);
			} else {
				final BigDecimal amount = cli.inputDecimal(String.format("Amount to take (in %s):",
					chosen.getQuantity().units()));
				if (amount != null && amount.signum() > 0) {
					model.transferResource(chosen, unit, amount, idf::createID);
					resources.clear();
					fortress.stream().filter(IResourcePile.class::isInstance).map(IResourcePile.class::cast)
						.forEach(resources::add);
				}
			}
		}
	}

	@Override
	public @Nullable String run() {
		final StringBuilder buffer = new StringBuilder();
		model.addSelectionChangeListener(explorationCLI);
		final IUnit mover = model.getSelectedUnit();
		if (mover == null) {
			cli.println("No currently selected unit");
			return "";
		}
		// Ask the user about total MP, through explorationCLI listening for the selection-change event
		model.setSelectedUnit(mover);
		while (explorationCLI.getMovement() > 0) {
			final Point oldPosition = model.getSelectedUnitLocation();
			explorationCLI.moveOneStep();
			final Point newPosition = model.getSelectedUnitLocation();
			final IFortress startingFort = model.getMap().getFixtures(oldPosition).stream()
				.filter(IFortress.class::isInstance).map(IFortress.class::cast)
				.filter(f -> f.owner().equals(mover.owner())).findAny().orElse(null);
			if (startingFort != null && model.getMap().getFixtures(newPosition).stream()
					.filter(IFortress.class::isInstance).map(IFortress.class::cast)
					.noneMatch(f -> f.owner().equals(mover.owner()))) {
				final Boolean pack = cli.inputBooleanInSeries("Leaving a fortress. Take provisions along?");
				if (pack == null) {
					return null;
				} else if (pack) {
					packFood(startingFort, mover);
				}
			}
			final String addendum = cli.inputMultilineString("Add to results:");
			if (addendum == null) {
				return null;
			}
			buffer.append(addendum);
		}
		// We don't want to be asked about MP for any other applets
		model.removeSelectionChangeListener(explorationCLI);
		return buffer.toString();
	}
}
