package games.stendhal.server.entity.spell.effect;

import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.player.Player;

public class HealingEffect extends AbstractEffect {

	public HealingEffect(int amount, int atk, int def, double lifesteal, int rate,
			int regen) {
		super(amount, atk, def, lifesteal, rate, regen);
	}

	public void act(Player caster, Entity target) {
		int oldHP = caster.getHP();
		int baseHP = caster.getBaseHP();
		caster.setHP(Math.max(oldHP + getAmount(), baseHP));
	}

}
