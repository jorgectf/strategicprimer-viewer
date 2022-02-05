package drivers.turnrunning.applets;

import drivers.common.cli.ICLIHelper;

import java.math.BigDecimal;

import static lovelace.util.Decimalize.decimalize;

import common.map.fixtures.IResourcePile;
import common.map.fixtures.Quantity;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/* package */ enum FoodType {
	Milk(2, 3, 4, null, decimalize(0.5), decimalize(8), "milk"),
	Meat(2, 3, 4, 8, decimalize(0.25), decimalize(2), "meat"),
	Grain(7, 10, null, null, decimalize(0.125), decimalize(1), "grain"),
	SlowFruit(6, 8, 12, null, decimalize(0.125), decimalize(1), "slow-spoiling fruit"),
	QuickFruit(3, 6, 8, null, decimalize(0.25), decimalize(1), "fast-spoiling fruit"),
	// TODO: Add additional cases
	Other(null, null, null, null, null, null, "other");

	@Nullable
	public static FoodType askFoodType(ICLIHelper cli, String foodKind) {
		for (FoodType type : FoodType.values()) {
			Boolean resp = cli.inputBooleanInSeries(String.format("Is it %s?", type),
				foodKind + type);
			if (resp == null) {
				return null; // EOF
			} else if (resp) {
				return type;
			}
		}
		return null;
	}

	@Nullable
	private final Integer keepsFor;

	@Nullable
	public Integer getKeepsFor() {
		return keepsFor;
	}

	@Nullable
	private final Integer keepsForIfCool;

	@Nullable
	public Integer getKeepsForIfCool() {
		return keepsForIfCool;
	}

	@Nullable
	private final Integer keepsForRefrigerated;

	@Nullable
	public Integer getKeepsForRefrigerated() {
		return keepsForRefrigerated;
	}

	@Nullable
	private final Integer keepsForFrozen;

	@Nullable
	public Integer getKeepsForFrozen() {
		return keepsForFrozen;
	}

	@Nullable
	private final BigDecimal fractionSpoilingDaily;

	@Nullable
	public BigDecimal getFractionSpoilingDaily() {
		return fractionSpoilingDaily;
	}

	@Nullable
	private final BigDecimal minimumSpoilage;

	@Nullable
	public BigDecimal getMinimumSpoilage() {
		return minimumSpoilage;
	}

	private final String string;

	@Override
	public String toString() {
		return string;
    	}

	private FoodType(@Nullable Integer keepsFor, @Nullable Integer keepsForIfCool,
			@Nullable Integer keepsForRefrig, @Nullable Integer keepsForFrozen,
			@Nullable BigDecimal fracSpoilingDaily, @Nullable BigDecimal minSpoilage,
			String str) {
		this.keepsFor = keepsFor;
		this.keepsForIfCool = keepsForIfCool;
		this.keepsForRefrigerated = keepsForRefrig;
		this.keepsForFrozen = keepsForFrozen;
		this.fractionSpoilingDaily = fracSpoilingDaily;
		this.minimumSpoilage = minSpoilage;
		string = str;
	}

	@Nullable
	public Boolean hasSpoiled(IResourcePile pile, int turn, ICLIHelper cli) {
		int age = turn - pile.getCreated();
		if (turn < 0 || pile.getCreated() < 0) { // Either corrupt turn information or non-spoiling rations
			return false;
		} else if (pile.getCreated() >= turn) { // Created this turn or in the future
			return false;
		} else if (keepsFor != null && age < keepsFor) {
			return false;
		} else if (keepsForIfCool != null && age < keepsForIfCool) {
			Boolean resp = cli.inputBooleanInSeries("Was this kept cool?", pile.getKind() + string + "cool");
			if (resp == null) {
				return null;
			} else if (resp) {
				return false;
			}
		}
		if (keepsForRefrigerated != null && age < keepsForRefrigerated) {
			Boolean resp = cli.inputBooleanInSeries("Was this kept refrigerated?",
				pile.getKind() + string + "refrig");
			if (resp == null) {
				return null;
			} else if (resp) {
				return false;
			}
		}
		if (keepsForFrozen != null && age < keepsForFrozen) {
			Boolean resp = cli.inputBooleanInSeries("Was this kept frozen?",
				pile.getKind() + string + "frozen");
			if (resp == null) {
				return null;
			} else if (resp) {
				return false;
			}
		}
		if (Stream.of(keepsFor, keepsForIfCool, keepsForRefrigerated, keepsForFrozen).allMatch(x -> x == null)) {
			return cli.inputBooleanInSeries("Has this spoiled?", pile.getKind() + string + "other");
		} else {
			return true;
		}
	}

	@Nullable
	public BigDecimal amountSpoiling(Quantity qty, ICLIHelper cli) {
		BigDecimal amt = decimalize(qty.getNumber());
		BigDecimal fractional = Optional.ofNullable(fractionSpoilingDaily).map(amt::multiply).orElse(null);
		return Stream.of(fractional, minimumSpoilage).filter(Objects::nonNull)
				.max(Comparator.naturalOrder()).orElseGet(() -> cli.inputDecimal("How many pounds spoil?"));
	}
}
