package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.BasketOfApplesContract;
import com.template.states.AppleStamp;
import com.template.states.BasketOfApples;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class RedeemApples {

    @InitiatingFlow
    @StartableByRPC
    public static class RedeemApplesInitiator extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier  stampId ;
        private Party buyer;

        public RedeemApplesInitiator(UniqueIdentifier stampId, Party buyer) {
            this.stampId = stampId;
            this.buyer = buyer;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Reference to notary
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // 2. Query AppleStamp
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(stampId.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef appleStampStateAndRef = getServiceHub().getVaultService().queryBy(AppleStamp.class, inputCriteria).getStates().get(0);

            // 3. Query output basketOfApple
            QueryCriteria.VaultQueryCriteria outputCriteria = new QueryCriteria.VaultQueryCriteria()
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef basketOfAppleStateAndRef = getServiceHub().getVaultService().queryBy(BasketOfApples.class, outputCriteria).getStates().get(0);
            BasketOfApples originalBasketOfApple = (BasketOfApples) basketOfAppleStateAndRef.getState().getData();

            // 4. Modify output to address the owner change
            BasketOfApples output = originalBasketOfApple.changeOwner(buyer);

            // 5. Compositing Transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(appleStampStateAndRef)
                    .addInputState(basketOfAppleStateAndRef)
                    .addOutputState(output, BasketOfApplesContract.ID)
                    .addCommand(new BasketOfApplesContract.Commands.Redeem(),
                            Arrays.asList(getOurIdentity().getOwningKey(), this.buyer.getOwningKey()));

            // 6. Verify that transaction is valid
            txBuilder.verify(getServiceHub());

            // 7. Sign the transaction from your own side
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // 8. Send the state to the counterParty, and receive it back with their signature
            FlowSession otherPartySession = initiateFlow(buyer);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession))
            );

            // 9. Notarise and record the transaction in both parties' vaults.
            SignedTransaction result = subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
            return result;
        }

    }

    @InitiatedBy(RedeemApplesInitiator.class)
    public static class RedeemApplesResponder extends FlowLogic<Void>{
        private FlowSession counterpartySession;

        public RedeemApplesResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                }
            });

            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));

            return null;
        }
    }
}
