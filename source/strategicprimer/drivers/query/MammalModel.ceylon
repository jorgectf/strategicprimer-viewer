import strategicprimer.model.common.map.fixtures {
    Quantity
}

"Models of (game statistics for) herding mammals."
shared final class MammalModel satisfies HerdModel {
    "How much time, per head, in minutes, must be spent to milk, or otherwise collect the
     food produced by the animals."
    shared actual Integer dailyTimePerHead;

    "The amount produced per head per turn."
    shared actual Quantity productionPerHead;

    "The number of pounds per gallon."
    shared actual Float poundsCoefficient = 8.6;

    "How much time, in minutes, must be spent on the entire herd or flock each turn,
     regardless of its size, in addition to each herder's time with individual animals."
    shared Integer dailyTimeFloor = 60;

    "A description of the model to show the user."
    shared actual String name;

    new ("The amount produced per head per turn, in gallons" Float production,
            Integer dailyTimePerHead, String nomen) {
        this.dailyTimePerHead = dailyTimePerHead;
        productionPerHead = Quantity(production, "gallons");
        name = nomen;
    }

    "The model for dairy cattle."
    shared new dairyCattle extends MammalModel(4.0, 40, "Dairy Cattle") { }

    "The model for other roughly-cattle-sized mammals. (Not for anything as large as
     elephants.)"
    shared new largeMammals extends MammalModel(3.0, 40, "Large Mammals") { }

    "The model for roughly-goat-sized mammals."
    shared new smallMammals extends MammalModel(1.5, 30, "Medium-Size Mammals") { }

    "How much time, in minutes, herders must spend on a flock with this many animals per
     herder."
    shared actual Integer dailyTime(Integer heads) =>
            heads * dailyTimePerHead + dailyTimeFloor;

    "How much time, in minutes, an expert herder must spend on a flock with this many
     animals per herder."
    shared Integer dailyExpertTime(Integer heads) =>
            heads * (dailyTimePerHead - 10) + dailyTimeFloor;
}
