package com.template.flows;

import com.template.contracts.BasketOfApplesContract;
import com.template.states.BasketOfApples;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Collections;

public class PackageApples {

    @InitiatingFlow
    @StartableByRPC
    public static class PackageApplesInitiator extends FlowLogic<SignedTransaction>{

        private String appleDescription ;
        private int weight;

        public PackageApplesInitiator(String appleDescription, int weight) {
            this.appleDescription = appleDescription;
            this.weight = weight;
        }

        @Override
        public SignedTransaction call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            BasketOfApples basket = new BasketOfApples(this.appleDescription, this.getOurIdentity(), this.weight);

            //Building transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(basket)
                    .addCommand(new BasketOfApplesContract.Commands.packToBasket(), this.getOurIdentity().getOwningKey());

            // Verify the transaction
            txBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

            return subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
        }
    }

}
