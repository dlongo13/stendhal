package games.stendhal.server.entity.mapstuff.portal;

import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.events.TurnListener;
import games.stendhal.server.core.events.TurnNotifier;
import games.stendhal.server.core.events.UseListener;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.npc.ChatCondition;
import games.stendhal.server.entity.npc.condition.AlwaysTrueCondition;
import games.stendhal.server.entity.npc.parser.ConversationParser;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;
import marauroa.common.game.RPClass;
import marauroa.common.game.Definition.Type;

public class Gate extends Entity implements UseListener, TurnListener {
	private static final String HORIZONTAL = "h";
	private static final String VERTICAL = "v";
	private static final String ORIENTATION = "orientation";
	private static final String IMAGE = "image";
	
	private static final String DEFAULT_IMAGE = "fence_gate";

	public static void generateGateRPClass() {
		if (!RPClass.hasRPClass("gate")) {
			final RPClass gate = new RPClass("gate");
			gate.isA("entity");
			gate.addAttribute(ORIENTATION, Type.STRING);
			gate.addAttribute(IMAGE, Type.STRING);
		}
	}
	
	/** Current state of the gate. */
	private boolean isOpen;
	/** Condition for allowing use of the gate. */
	private final ChatCondition condition;
	/** 
	 * Time the door should keep open before closing. 0 if it should
	 * not close automatically.
	 */
	private int autoCloseDelay;

	/**
	 * Create a new gate.
	 * 
	 * @param orientation gate orientation. Either "v" or "h".
	 * @param image image used for the gate
	 * @param condition conditions required for opening the gate, or <code>null</code>
	 * 	if no checking is required
	 */
	public Gate(final String orientation, String image, ChatCondition condition) {
		setRPClass("gate");
		put("type", "gate");
		setOrientation(orientation);
		setOpen(false);
		if (condition == null) {
			condition = new AlwaysTrueCondition();
		}
		this.condition = condition;
		if (image != null) {
			put(IMAGE, image);
		} else {
			put(IMAGE, DEFAULT_IMAGE);
		}
	}
	
	/**
	 * Create a new vertical gate.
	 */
	public Gate() {
		this(VERTICAL, null, null);
	}

	/**
	 * Set the orientation of the gate.
	 * 
	 * @param orientation "h" for horizontal, "v" for vertical
	 */
	private void setOrientation(final String orientation) {
		if (HORIZONTAL.equals(orientation)) {
			put(ORIENTATION, HORIZONTAL);
		} else {
			put(ORIENTATION, VERTICAL);
		}
	}

	/**
	 * Open the gate.
	 */
	protected void open() {
		setOpen(true);
	}

	/**
	 * Check if the gate is open.
	 * 
	 * @return true iff the gate is open
	 */
	protected boolean isOpen() {
		return isOpen;
	}

	/**
	 * Close the gate.
	 */
	protected void close() {
		setOpen(false);
	}

	public boolean onUsed(final RPEntity user) {
		if (this.nextTo(user) && isAllowed(user)) {
			setOpen(!isOpen());
			return true;
		}
		return false;
	}
	
	/**
	 * Make the gate close automatically after specified delay
	 * once it's been opened.
	 * 
	 * @param seconds time to keep the gate open
	 */
	protected void setAutoCloseDelay(int seconds) {
		autoCloseDelay = seconds;
	}
	
	/**
	 * Check if a player can use the gate.
	 * 
	 * @param user player trying to close or open the gate
	 * @return <code>true</code> iff the player is allowed to use the gate
	 */
	private boolean isAllowed(final RPEntity user) {
		Sentence sentence = ConversationParser.parse(user.get("text"));
		return condition.fire((Player) user, sentence, this);
	}

	/**
	 * Set the door open or closed.
	 * 
	 * @param open true if the door is opened, false otherwise
	 */
	private void setOpen(final boolean open) {
		final TurnNotifier turnNotifier = SingletonRepository.getTurnNotifier();
		
		if (open) {
			setResistance(0);
			if (autoCloseDelay != 0) {
				turnNotifier.notifyInSeconds(autoCloseDelay, this);
			}
		} else {
			// Closing the gate - check there's nobody on the way
			if (getZone() != null)  {
				for (Entity entity : getZone().getEntitiesAt(getX(), getY())) {
					if (entity.getResistance() > 0) {
						return;
					}
				}
			}
			setResistance(100);
			// Stop the notifier, so that the door does not slam in front
			// of someone who just opened it
			turnNotifier.dontNotify(this);
		}
		isOpen = open;
		notifyWorldAboutChanges();
	}

	public void onTurnReached(int currentTurn) {
		setOpen(false);
		/*
		 * If something was on the way, the closing failed.
		 * Try again after the usual delay. 
		 */
		if (isOpen) {
			final TurnNotifier turnNotifier = SingletonRepository.getTurnNotifier();
			turnNotifier.notifyInSeconds(autoCloseDelay, this);
		}
	}
}
