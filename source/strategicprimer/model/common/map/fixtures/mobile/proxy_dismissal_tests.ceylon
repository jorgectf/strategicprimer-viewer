import ceylon.test {
    test,
    assertEquals,
    assertNotEquals
}
import strategicprimer.model.common.map {
    IFixture,
    Player,
    PlayerImpl
}
import lovelace.util.common {
    matchingValue
}
import strategicprimer.model.common.map.fixtures.mobile {
    IUnit,
    Unit,
    ProxyFor,
    IWorker,
    Worker
}

"A test that dismissing workers from units works when the situation is
 complicated by proxies."
test
void testProxyDismissal() {
    Player player = PlayerImpl(1, "player");
    IMutableUnit firstUnit = Unit(player, "unitKind", "unitName", 2);
    IMutableUnit secondUnit = Unit(player, "unitKind", "unitName", 2);
    IMutableUnit thirdUnit = Unit(player, "unitKind", "unitName", 2);
    IWorker firstWorkerOne = Worker("first", "firstRace", 3);
    IWorker secondWorkerOne = Worker("first", "firstRace", 3);
    IWorker thirdWorkerOne = Worker("first", "firstRace", 3);
    IWorker firstWorkerTwo = Worker("second", "secondRace", 4);
    IWorker secondWorkerTwo = Worker("second", "secondRace", 4);
    IWorker thirdWorkerTwo = Worker("second", "secondRace", 4);
    IWorker firstWorkerThree = Worker("third", "thirdRace", 5);
    IWorker secondWorkerThree = Worker("third", "thirdRace", 5);
    IWorker thirdWorkerThree = Worker("third", "thirdRace", 5);
    firstUnit.addMember(firstWorkerOne);
    firstUnit.addMember(firstWorkerTwo);
    firstUnit.addMember(firstWorkerThree);
    secondUnit.addMember(secondWorkerOne);
    secondUnit.addMember(secondWorkerTwo);
    secondUnit.addMember(secondWorkerThree);
    thirdUnit.addMember(thirdWorkerOne);
    thirdUnit.addMember(thirdWorkerTwo);
    thirdUnit.addMember(thirdWorkerThree);
    IMutableUnit&ProxyFor<IUnit> proxyUnit = ProxyUnit.fromParallelMaps(2);
    proxyUnit.addProxied(firstUnit);
    proxyUnit.addProxied(secondUnit);
    assertEquals(firstUnit, secondUnit, "Two units in proxy are initially equal");
    assertEquals(firstUnit, thirdUnit, "Units in and not in proxy are initially equal");
    assert (is ProxyFor<out IWorker> proxiedWorker =
            proxyUnit.find(matchingValue(4, IFixture.id)));
    proxyUnit.removeMember(proxiedWorker);
    assertEquals(firstUnit, secondUnit,
        "Two units in proxy are still equal after removing via proxy-worker");
    assertNotEquals(firstUnit, thirdUnit,
        "Units in and not in proxy not equal after removing via proxy but not directly");
    thirdUnit.removeMember(thirdWorkerTwo);
    assertEquals(firstUnit, thirdUnit,
        "Units not in and not in proxy equal again after removing corresponding worker");
}
