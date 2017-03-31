import java.util {
    EventListener
}
import model.map {
    Point
}
"An interface for objects that want to know when the selected tile, or its location,
 changes."
shared interface SelectionChangeListener satisfies EventListener {
	"The selected tile's location changed."
	shared formal void selectedPointChanged(Point? previousSelection, Point newSelection);
}