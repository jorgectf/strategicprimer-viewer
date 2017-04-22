import java.awt {
    Component,
    Frame,
    BorderLayout,
    Dimension
}
import lovelace.util.common {
    todo
}
import ceylon.regex {
    Regex,
    regex
}
import javax.swing {
    JEditorPane,
    ScrollPaneConstants,
    JScrollPane,
    JDialog
}
import lovelace.util.jvm {
    listenedButton,
    centeredHorizontalBox,
    platform
}
import strategicprimer.viewer.drivers {
    SPDialog
}
import ceylon.interop.java {
    javaString
}
Frame? asFrame(Component? parent) {
    if (is Frame parent) {
        return parent;
    } else {
        return null;
    }
}
"A dialog to explain what this program is, and the sources of code and graphics."
todo("FIXME: Credits for other images?") // FIXME
shared JDialog aboutDialog(Component? parentComponent, String? app) {
    JDialog retval = SPDialog(asFrame(parentComponent), "About");
    retval.setLayout(BorderLayout());
    assert (exists resource =
            `module strategicprimer.viewer`.resourceByPath("images/about.html"));
    Regex matcher = regex("App Name Here", true);
    String html = matcher.replace(resource.textContent(),
        app else "Strategic Primer Assistive Programs");
    JEditorPane pane = JEditorPane("text/html", html);
    pane.caretPosition = 0; // scroll to the top
    pane.editable = false;
    JScrollPane scrollPane;
    if (platform.systemIsMac) {
        scrollPane = JScrollPane(pane,
            ScrollPaneConstants.verticalScrollbarAlways,
            ScrollPaneConstants.horizontalScrollbarAlways);
    } else {
        scrollPane = JScrollPane(pane,
            ScrollPaneConstants.verticalScrollbarAsNeeded,
            ScrollPaneConstants.horizontalScrollbarAsNeeded);
    }
    scrollPane.minimumSize =Dimension(300, 400);
    scrollPane.preferredSize =Dimension(400, 500);
    retval.add(scrollPane, javaString(BorderLayout.center));
    retval.add(centeredHorizontalBox(listenedButton("Close",
                (event) => retval.dispose())), javaString(BorderLayout.pageEnd));
    retval.pack();
    return retval;
}