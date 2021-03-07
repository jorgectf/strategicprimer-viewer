import strategicprimer.model.common.map {
    IFixture,
    IMapNG
}
import ceylon.collection {
    MutableMap,
    HashMap
}
import strategicprimer.model.common.xmlio {
    Warning
}

object dbMemoizer {
    MutableMap<[IMapNG, Integer], IFixture> cache =
            HashMap<[IMapNG, Integer], IFixture>();

    shared IFixture findById(IMapNG map, Integer id, MapContentsReader context,
            Warning warner) {
        if (exists retval = cache[[map, id]]) {
            return retval;
        } else {
            assert (exists retval = context.findByIdImpl(map.fixtures.items, id));
            cache[[map, id]] = retval;
            return retval;
        }
    }
}
