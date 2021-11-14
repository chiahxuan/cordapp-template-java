package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.AppleStampContract;
import com.template.states.AppleStamp;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CreateAndIssueAppleStamp {

    //Initiator
    @InitiatingFlow
    @StartableByRPC
    public static class CreateAndIssueAppleStampInitiator extends FlowLogic<SignedTransaction>{

        private String stampDescription;
        private Party holder;

        public CreateAndIssueAppleStampInitiator(String stampDescription, Party holder) {
            this.stampDescription = stampDescription;
            this.holder = holder;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            //get notary
            //first way - using index
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            //second way - using notary name specification, but how to see what are the notaries in the app?
//            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("Notary Name"));

            //Building the output AppleStamp state
            UniqueIdentifier uniqueID = new UniqueIdentifier();
            AppleStamp newStamp = new AppleStamp(this.stampDescription, this.getOurIdentity(), this.holder, uniqueID );

            //Compositing Transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(newStamp)
                    .addCommand(new AppleStampContract.Commands.Issue(),
                            Arrays.asList(getOurIdentity().getOwningKey(), holder.getOwningKey() ));

            //Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            //transaction got verified, thus can proceed to sign
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature
            FlowSession otherPartySession = initiateFlow(holder);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession)));

            //Notarise and record the transaction in both parties' -s
            return subFlow(new FinalityFlow(fullySignedTx, otherPartySession));
        }
    }

    //Responder
    @InitiatedBy(CreateAndIssueAppleStampInitiator.class)
    public static class CreateAndIssueAppleStampResponder extends FlowLogic<Void>{

        private FlowSession counterpartySession;

        public CreateAndIssueAppleStampResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
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
