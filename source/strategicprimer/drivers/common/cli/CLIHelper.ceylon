import ceylon.decimal {
    Decimal,
    decimalNumber,
    parseDecimal
}
import ceylon.collection {
    MutableMap,
    HashMap
}
import strategicprimer.model.common.map {
    HasName
}
import lovelace.util.common {
    isNumeric,
    parseInt
}

"A helper class to help command-line apps interact with the user, encapsulating
 input and output streams."
shared final class CLIHelper(istream = process.readLine, ostream = process.write)
        satisfies ICLIHelper {
    "A way to read a line at a time, presumably from the user."
    String?() istream;

    "A consumer of output, presumably sending it to the user."
    Anything(String) ostream;

    "The current state of the yes-to-all/no-to-all possibility. Absent if not set,
     present if set, and the boolean value is what to return."
    MutableMap<String, Boolean> seriesState = HashMap<String, Boolean>();

    "Print a prompt, adding whitespace if the prompt didn't end with it."
    void writePrompt(String prompt) {
        ostream(prompt);
        if (exists last = prompt.last, !last.whitespace) {
            ostream(" ");
        }
    }

    "Print the specified string, then a newline."
    shared actual void println(String line) {
        ostream(line);
        ostream(operatingSystem.newline);
    }

    "Ask the user a yes-or-no question. Returns [[null]] on EOF."
    shared actual Boolean? inputBoolean(String prompt) {
        while (true) {
            switch(input = inputString(prompt)?.lowercased)
            case ("yes"|"true"|"y"|"t") { return true; }
            case ("no"|"false"|"n"|"f") { return false; }
            case (null) { return null; }
            else {
                ostream("""Please enter "yes", "no", "true", or "false",
                           or the first character of any of those.
                           """);
            }
        }
    }

    "Print a list of things by name and number."
    void printList<out Element>({Element*} list, String(Element) func) {
        for (index->item in list.indexed) {
            println("``index``: ``func(item)``");
        }
    }

    "Implementation of chooseFromList() and chooseStringFromList()."
    Integer->Element? chooseFromListImpl<Element>({Element*} items, String description,
            String none, String prompt, Boolean auto, String(Element) func)
            given Element satisfies Object {
        if (items.empty) {
            println(none);
            return -1->null;
        }
        println(description);
        if (auto, !items.rest.first exists) {
            assert (exists first = items.first);
            println("Automatically choosing only item, ``func(first)``.");
            return 0->first;
        } else {
            printList(items, func);
            if (exists retval = inputNumber(prompt)) {
                return retval->items.getFromFirst(retval);
            } else {
                return -2->null;
            }
        }
    }

    "Have the user choose an item from a list."
    shared actual Integer->Element? chooseFromList<out Element>(
            Element[]|List<Element> list, String description, String none,
            String prompt, Boolean auto) given Element satisfies HasName&Object =>
        chooseFromListImpl<Element>(list, description, none, prompt, auto,
            HasName.name);

    "Read input from the input stream repeatedly until a non-negative integer is entered,
     then return it. Returns null on EOF."
    shared actual Integer? inputNumber(String prompt) {
        variable Integer retval = -1;
        while (retval < 0) {
            writePrompt(prompt);
            if (exists input = istream()) {
                if (isNumeric(input)) {
                    assert (exists temp = parseInt(input));
                    retval = temp;
                }
            } else {
                return null;
            }
        }
        return retval;
    }

    "Read from the input stream repeatedly until a valid non-negative decimal number is
     entered, then return it. Returns null on EOF."
    shared actual Decimal? inputDecimal(String prompt) {
        variable Decimal retval = decimalNumber(-1);
        Decimal zero = decimalNumber(0);
        while (retval < zero) {
            writePrompt(prompt);
            if (exists input = istream()) {
                if (exists temp = parseDecimal(input.trimmed)) {
                    retval = temp;
                } else {
                    println("Invalid number.");
                }
            } else {
                return null;
            }
        }
        return retval;
    }

    "Read a line of input from the input stream. It is trimmed of leading and trailing
     whitespace. Returns [[null]] on EOF."
    shared actual String? inputString(String prompt) {
        writePrompt(prompt);
        return istream()?.trimmed;
    }

    "Ask the user a yes-or-no question, allowing yes-to-all or no-to-all to skip further
     questions."
    shared actual Boolean? inputBooleanInSeries(String prompt,
            String key, Boolean?(String) quitResultFactory) {
        if (exists retval = seriesState[key]) {
            writePrompt(prompt);
            println((retval) then "yes" else "no");
            return retval;
        } else {
            while (true) {
                String? input = inputString(prompt)?.lowercased;
                if (exists input, is Null result = quitResultFactory(input)) {
                    return result;
                }
                switch(input)
                case (null) {
                    return null;
                }
                case ("all"|"ya"|"ta"|"always") {
                    seriesState[key] = true;
                    return true;
                }
                case ("none"|"na"|"fa"|"never") {
                    seriesState[key] = false;
                    return false;
                }
                case ("yes"|"true"|"y"|"t") { return true; }
                case ("no"|"false"|"n"|"f") { return false; }
                else {
                    ostream(
                        """Please enter "yes", "no", "true", or "false", the first
                           character of any of those, or "all", "none", "always", or
                           "never" to use the same answer for all further questions
                           """);
                }
            }
        }
    }

    "Have the user choose an item from a list."
    shared actual Integer->String? chooseStringFromList(String[] items,
            String description, String none, String prompt, Boolean auto) =>
        chooseFromListImpl<String>(items, description, none, prompt, auto,
                identity);

    "Print the specified string."
    shared actual void print(String text) => ostream(text);
}
