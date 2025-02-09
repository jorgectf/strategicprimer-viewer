package drivers.turnrunning.applets;

import common.idreg.IDRegistrar;

import drivers.common.cli.ICLIHelper;

import drivers.turnrunning.ITurnRunningModel;

import com.google.auto.service.AutoService;

@AutoService(TurnAppletFactory.class)
public class OtherAppletFactory implements TurnAppletFactory {
	@Override
	public TurnApplet create(final ITurnRunningModel model, final ICLIHelper cli, final IDRegistrar idf) {
		return new OtherApplet();
	}
}
