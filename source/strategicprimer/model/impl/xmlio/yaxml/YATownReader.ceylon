import ceylon.language.meta {
    classDeclaration
}

import javax.xml.namespace {
    QName
}
import javax.xml.stream.events {
    XMLEvent,
    StartElement,
    EndElement
}

import strategicprimer.model.common.idreg {
    IDRegistrar
}
import strategicprimer.model.common.map {
    IPlayerCollection,
    Player
}
import strategicprimer.model.common.map.fixtures.mobile.worker {
    raceFactory
}
import strategicprimer.model.common.map.fixtures.towns {
    TownStatus,
    TownSize,
    ITownFixture,
    Fortress,
    Village,
    AbstractTown,
    City,
    Fortification,
    Town,
    CommunityStats
}
import strategicprimer.model.common.xmlio {
    Warning
}
import strategicprimer.model.impl.xmlio.exceptions {
    MissingPropertyException,
    UnwantedChildException
}
import ceylon.random {
    DefaultRandom
}
import strategicprimer.model.common.map.fixtures {
    IMutableResourcePile
}
import ceylon.collection {
    Stack,
    LinkedList
}

"A reader for fortresses, villages, and other towns."
class YATownReader(Warning warner, IDRegistrar idRegistrar, IPlayerCollection players)
        extends YAAbstractReader<ITownFixture>(warner, idRegistrar) {
    value resourceReader = YAResourcePileReader(warner, idRegistrar);

    value memberReaders = [
        YAUnitReader(warner, idRegistrar, players),
        resourceReader, YAImplementReader(warner, idRegistrar)
    ];

    """If the tag has an "owner" parameter, return the player it indicates; otherwise
       trigger a warning and return the "independent" player."""
    Player getOwnerOrIndependent(StartElement element) {
        if (hasParameter(element, "owner")) {
            return players.getPlayer(getIntegerParameter(element, "owner"));
        } else {
            warner.handle(MissingPropertyException(element, "owner"));
            return players.independent;
        }
    }

    {String*} expectedCommunityStatsTags(String parent) {
        switch (parent)
        case ("population") {
            return ["expertise", "claim", "production", "consumption"];
        }
        case ("claim"|"expertise") { return []; }
        case ("production"|"consumption") { return ["resource"]; }
        else {
            throw AssertionError("Impossible CommunityStats parent tag");
        }
    }

    shared CommunityStats parseCommunityStats(StartElement element, QName parent,
            {XMLEvent*} stream) {
        requireTag(element, parent, "population");
        expectAttributes(element, "size");
        CommunityStats retval = CommunityStats(getIntegerParameter(element, "size"));
        variable String? current = null;
        Stack<StartElement> stack = LinkedList<StartElement>();
        stack.push(element);
        for (event in stream) {
            if (is EndElement event, event.name == element.name) {
                break;
            } else if (is StartElement event, isSupportedNamespace(event.name)) {
                switch (event.name.localPart.lowercased)
                case ("expertise") {
                    if (exists temp = current) {
                        assert (exists top = stack.top);
                        throw UnwantedChildException.listingExpectedTags(top.name, event,
                            expectedCommunityStatsTags(temp));
                    } else {
                        expectAttributes(event, "skill", "level");
                        retval.setSkillLevel(getParameter(event, "skill"),
                            getIntegerParameter(event, "level"));
                        stack.push(event);
                        current = event.name.localPart;
                    }
                }
                case ("claim") {
                    if (exists temp = current) {
                        assert (exists top = stack.top);
                        throw UnwantedChildException.listingExpectedTags(top.name, event,
                            expectedCommunityStatsTags(temp));
                    } else {
                        expectAttributes(event, "resource");
                        retval.addWorkedField(getIntegerParameter(event, "resource"));
                        stack.push(event);
                        current = event.name.localPart;
                    }
                }
                case ("production"|"consumption") {
                    if (exists temp = current) {
                        assert (exists top = stack.top);
                        throw UnwantedChildException.listingExpectedTags(top.name, event,
                            expectedCommunityStatsTags(temp));
                    } else {
                        expectAttributes(event);
                        current = event.name.localPart;
                        stack.push(event);
                    }
                }
                case ("resource") {
                    assert (exists top = stack.top);
                    Anything(IMutableResourcePile) lambda;
                    switch (current)
                    case ("production") {
                        lambda = retval.yearlyProduction.add;
                    }
                    case ("consumption") {
                        lambda = retval.yearlyConsumption.add;
                    }
                    else {
                        throw UnwantedChildException.listingExpectedTags(top.name, event,
                            expectedCommunityStatsTags(current else "population"));
                    }
                    lambda(resourceReader.read(event, top.name, stream));
                }
                else {
                    throw UnwantedChildException.listingExpectedTags(
                        stack.top?.name else element.name, event,
                        expectedCommunityStatsTags(current else "population"));
                }
            } else if (is EndElement event, exists top = stack.top,
                    event.name == top.name) {
                stack.pop();
                if (top == element) {
                    break;
                } else if (exists temp = current, top.name.localPart == temp) {
                    assert (exists next = stack.top);
                    if (next.name.localPart == "population") {
                        current = null;
                    } else {
                        current = next.name.localPart;
                    }
                }
            }
        }
        return retval;
    }

    ITownFixture parseVillage(StartElement element, {XMLEvent*} stream) {
        expectAttributes(element, "status", "name", "race", "image", "portrait",
            "id", "owner");
        requireNonEmptyParameter(element, "name", false);
        Integer idNum = getOrGenerateID(element);
        value status = TownStatus.parse(getParameter(element, "status"));
        if (is TownStatus status) {
            Village retval = Village(status, getParameter(element, "name", ""), idNum,
                getOwnerOrIndependent(element), getParameter(element, "race",
                    raceFactory.randomRace(DefaultRandom(idNum))));
            retval.image = getParameter(element, "image", "");
            retval.portrait = getParameter(element, "portrait", "");
            for (event in stream) {
                if (is StartElement event, isSupportedNamespace(event.name)) {
                    if (retval.population exists) {
                        throw UnwantedChildException(element.name, event);
                    } else {
                        retval.population = parseCommunityStats(event, element.name,
                            stream);
                    }
                } else if (isMatchingEnd(element.name, event)) {
                    break;
                }
            }
            return retval;
        } else {
            throw MissingPropertyException(element, "status", status);
        }
    }

    ITownFixture parseTown(StartElement element, {XMLEvent*} stream) {
        expectAttributes(element, "name", "status", "size", "dc", "id", "image", "owner",
            "portrait");
        requireNonEmptyParameter(element, "name", false);
        String name = getParameter(element, "name", "");
        value status = TownStatus.parse(getParameter(element, "status"));
        if (is TownStatus status) {
            value size = TownSize.parse(getParameter(element, "size"));
            if (is TownSize size) {
                Integer dc = getIntegerParameter(element, "dc");
                Integer id = getOrGenerateID(element);
                Player owner = getOwnerOrIndependent(element);
                AbstractTown retval;
                switch (element.name.localPart.lowercased)
                case ("town") { retval = Town(status, size, dc, name, id, owner); }
                case ("city") { retval = City(status, size, dc, name, id, owner); }
                case ("fortification") {
                    retval = Fortification(status, size, dc, name, id, owner);
                }
                else {
                    throw AssertionError("Unhandled town tag");
                }
                for (event in stream) {
                    if (is StartElement event, isSupportedNamespace(event.name)) {
                        if (retval.population exists) {
                            throw UnwantedChildException(element.name, event);
                        } else {
                            retval.population = parseCommunityStats(event, element.name,
                                stream);
                        }
                    } else if (isMatchingEnd(element.name, event)) {
                        break;
                    }
                }
                retval.image = getParameter(element, "image", "");
                retval.portrait = getParameter(element, "portrait", "");
                return retval;
            } else {
                throw MissingPropertyException(element, "size", size);
            }
        } else {
            throw MissingPropertyException(element, "status", status);
        }
    }

    ITownFixture parseFortress(StartElement element, {XMLEvent*} stream) {
        expectAttributes(element, "owner", "name", "size", "status", "id", "portrait",
            "image");
        requireNonEmptyParameter(element, "owner", false);
        requireNonEmptyParameter(element, "name", false);
        Fortress retval;
        value size = TownSize.parse(getParameter(element, "size", "small"));
        if (is TownSize size) {
            retval = Fortress(getOwnerOrIndependent(element),
                getParameter(element, "name", ""), getOrGenerateID(element),
                size);
        } else {
            throw MissingPropertyException(element, "size", size);
        }
        for (event in stream) {
            if (is StartElement event, isSupportedNamespace(event.name)) {
                String memberTag = event.name.localPart.lowercased;
                if (exists reader = memberReaders
                        .find((yar) => yar.isSupportedTag(memberTag))) {
                    retval.addMember(reader.read(event, element.name, stream));
                } else if (memberTag == "orders" || memberTag == "results" ||
                        memberTag == "science") {
                    // We're thinking about storing per-fortress "standing orders" or
                    // general regulations, building-progress results, and possibly
                    // scientific research progress within fortresses. To ease the
                    // transition, we *now* warn, instead of aborting, if the tags we
                    // expect to use for this appear in this position in the XML.
                    warner.handle(UnwantedChildException(element.name, event));
                    continue;
                } else {
                    throw UnwantedChildException(element.name, event);
                }
            } else if (isMatchingEnd(element.name, event)) {
                break;
            }
        }
        retval.image = getParameter(element, "image", "");
        retval.portrait = getParameter(element, "portrait", "");
        return retval;
    }

    void writeAbstractTown(Anything(String) ostream, AbstractTown obj, Integer tabs) {
        writeTag(ostream, obj.kind, tabs);
        writeProperty(ostream, "status", obj.status.string);
        writeProperty(ostream, "size", obj.townSize.string);
        writeProperty(ostream, "dc", obj.dc);
        writeNonemptyProperty(ostream, "name", obj.name);
        writeProperty(ostream, "id", obj.id);
        writeProperty(ostream, "owner", obj.owner.playerId);
        writeImageXML(ostream, obj);
        writeNonemptyProperty(ostream, "portrait", obj.portrait);
        if (exists temp = obj.population) {
            finishParentTag(ostream);
            writeCommunityStats(ostream, temp, tabs + 1);
            closeTag(ostream, tabs, obj.kind);
        } else {
            closeLeafTag(ostream);
        }
    }

    shared void writeCommunityStats(Anything(String) ostream,
            CommunityStats obj, Integer tabs) {
        writeTag(ostream, "population", tabs);
        writeProperty(ostream, "size", obj.population);
        finishParentTag(ostream);
        for (skill->level in obj.highestSkillLevels.sort(increasingKey)) {
            writeTag(ostream, "expertise", tabs + 1);
            writeProperty(ostream, "skill", skill);
            writeProperty(ostream, "level", level);
            closeLeafTag(ostream);
        }
        for (claim in obj.workedFields) {
            writeTag(ostream, "claim", tabs + 1);
            writeProperty(ostream, "resource", claim);
            closeLeafTag(ostream);
        }
        if (!obj.yearlyProduction.empty) {
            writeTag(ostream, "production", tabs + 1);
            finishParentTag(ostream);
            for (resource in obj.yearlyProduction) {
                resourceReader.write(ostream, resource, tabs + 2);
            }
            closeTag(ostream, tabs + 1, "production");
        }
        if (!obj.yearlyConsumption.empty) {
            writeTag(ostream, "consumption", tabs + 1);
            finishParentTag(ostream);
            for (resource in obj.yearlyConsumption) {
                resourceReader.write(ostream, resource, tabs + 2);
            }
            closeTag(ostream, tabs + 1, "consumption");
        }
        closeTag(ostream, tabs, "population");
    }

    shared actual Boolean isSupportedTag(String tag) =>
            ["village", "fortress", "town", "city", "fortification"]
                .contains(tag.lowercased);

    shared actual ITownFixture read(StartElement element, QName parent,
            {XMLEvent*} stream) {
        requireTag(element, parent, "village", "fortress", "town", "city",
            "fortification");
        switch (element.name.localPart.lowercased)
        case ("village") { return parseVillage(element, stream); }
        case ("fortress") { return parseFortress(element, stream); }
        else { return parseTown(element, stream); }
    }

    shared actual void write(Anything(String) ostream, ITownFixture obj, Integer tabs) {
        assert (is AbstractTown|Village|Fortress obj);
        switch (obj)
        case (is AbstractTown) {
            writeAbstractTown(ostream, obj, tabs);
        }
        case (is Village) {
            writeTag(ostream, "village", tabs);
            writeProperty(ostream, "status", obj.status.string);
            writeNonemptyProperty(ostream, "name", obj.name);
            writeProperty(ostream, "id", obj.id);
            writeProperty(ostream, "owner", obj.owner.playerId);
            writeProperty(ostream, "race", obj.race);
            writeImageXML(ostream, obj);
            writeNonemptyProperty(ostream, "portrait", obj.portrait);
            if (exists temp = obj.population) {
                finishParentTag(ostream);
                writeCommunityStats(ostream, temp, tabs + 1);
                closeTag(ostream, tabs, "village");
            } else {
                closeLeafTag(ostream);
            }
        }
        case (is Fortress) {
            writeTag(ostream, "fortress", tabs);
            writeProperty(ostream, "owner", obj.owner.playerId);
            writeNonemptyProperty(ostream, "name", obj.name);
            if (TownSize.small != obj.townSize) {
                writeProperty(ostream, "size", obj.townSize.string);
            }
            writeProperty(ostream, "id", obj.id);
            writeImageXML(ostream, obj);
            writeNonemptyProperty(ostream, "portrait", obj.portrait);
            ostream(">");
            if (!obj.empty) {
                ostream(operatingSystem.newline);
                for (member in obj) {
                    if (exists reader = memberReaders
                            .find((yar) => yar.canWrite(member))) {
                        reader.writeRaw(ostream, member, tabs + 1);
                    } else {
                        log.error("Unhandled FortressMember type ``
                            classDeclaration(member).name``");
                    }
                }
                indent(ostream, tabs);
            }
            ostream("</fortress>");
            ostream(operatingSystem.newline);
        }
    }

    shared actual Boolean canWrite(Object obj) => obj is ITownFixture;
}
