package drivers.worker_mgmt.orderspanel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import lovelace.util.Platform;

/* package */ class EnterListener extends KeyAdapter {
	public EnterListener(Runnable delegate) {
		this.delegate = delegate;
	}

	private final Runnable delegate;

	@Override
	public void keyPressed(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ENTER && Platform.isHotKeyPressed(event)) {
			delegate.run();
		}
	}
}
