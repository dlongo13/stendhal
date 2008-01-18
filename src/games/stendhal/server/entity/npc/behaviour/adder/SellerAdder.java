package games.stendhal.server.entity.npc.behaviour.adder;

import games.stendhal.common.Grammar;
import games.stendhal.server.entity.npc.ConversationPhrases;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.behaviour.impl.SellerBehaviour;
import games.stendhal.server.entity.npc.fsm.Engine;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;

import org.apache.log4j.Logger;

public class SellerAdder {
	private static Logger logger = Logger.getLogger(SellerAdder.class);

	public void addSeller(SpeakerNPC npc, SellerBehaviour behaviour) {
		addSeller(npc, behaviour, true);
	}

	public void addSeller(SpeakerNPC npc, final SellerBehaviour behaviour,
			boolean offer) {
		Engine engine = npc.getEngine();

		if (offer) {
			engine.add(
					ConversationStates.ATTENDING,
					ConversationPhrases.OFFER_MESSAGES,
					null,
					ConversationStates.ATTENDING,
					"I sell "
							+ Grammar.enumerateCollection(behaviour.dealtItems())
							+ ".", null);
		}

		engine.add(ConversationStates.ATTENDING, "buy", null,
				ConversationStates.BUY_PRICE_OFFERED, null,
				new SpeakerNPC.ChatAction() {

					@Override
					public void fire(Player player, Sentence sentence,
							SpeakerNPC engine) {
						if (sentence.hasError()) {
							engine.say("Sorry, I did not understand you. "
									+ sentence.getErrorString());
							engine.setCurrentState(ConversationStates.ATTENDING);
						} else {
							// find out what the player wants to buy, and how much of it
							if (behaviour.findMatchingName(sentence)) {
    							// find out if the NPC sells this item, and if so,
    							// how much it costs.
    							if (behaviour.getAmount() > 1000) {
    								logger.warn("Decreasing very large amount of "
    										+ behaviour.getAmount() + " to 1 for player "
    										+ player.getName() + " talking to "
    										+ engine.getName() + " saying "
    										+ sentence);
    								behaviour.setAmount(1);
    							}

    							int price = behaviour.getUnitPrice(behaviour.chosenItem)
    									* behaviour.getAmount();

    							engine.say(Grammar.quantityplnoun(behaviour.getAmount(), behaviour.chosenItem)
    									+ " will cost " + price
    									+ ". Do you want to buy "
    									+ Grammar.itthem(behaviour.getAmount()) + "?");
    						} else {
    							if (behaviour.chosenItem == null) {
    								engine.say("Please tell me what you want to buy.");
    							} else {
    								engine.say("Sorry, I don't sell "
    										+ Grammar.plural(behaviour.chosenItem));
    							}

    							engine.setCurrentState(ConversationStates.ATTENDING);
    						}
						}
					}
				});

		engine.add(ConversationStates.BUY_PRICE_OFFERED,
				ConversationPhrases.YES_MESSAGES, null,
				ConversationStates.ATTENDING, "Thanks.",
				new SpeakerNPC.ChatAction() {
					@Override
					public void fire(Player player, Sentence sentence,
							SpeakerNPC engine) {
						String itemName = behaviour.chosenItem;
						logger.debug("Selling a " + itemName + " to player "
								+ player.getName());

						behaviour.transactAgreedDeal(engine, player);
					}
				});

		engine.add(ConversationStates.BUY_PRICE_OFFERED,
				ConversationPhrases.NO_MESSAGES, null,
				ConversationStates.ATTENDING, "Ok, how else may I help you?",
				null);
	}

}
