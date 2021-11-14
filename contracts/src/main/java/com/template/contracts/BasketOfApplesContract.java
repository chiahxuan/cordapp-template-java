package com.template.contracts;

import com.template.states.AppleStamp;
import com.template.states.BasketOfApples;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/*
*
* Farmer Bob creates the basket of apples. This intention is expressed by the packBasket command.
* Peter redeems the BasketOfApples state. This intention is expressed by the Redeem command.
*
* */
public class BasketOfApplesContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.BasketOfApplesContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        //get the command
        final CommandData commandData = tx.getCommands().get(0).getValue();

        // check for the intention
        if(commandData instanceof BasketOfApplesContract.Commands.packToBasket){ //intention 1
            //get the state obj of the transaction
            BasketOfApples output = tx.outputsOfType(BasketOfApples.class).get(0);

            requireThat(require->{
                require.using("This transaction should only output one BasketOfApples state",tx.getOutputs().size() ==1                );
                require.using("The output state of BasketOfApples should have a clear description of the apple product ",!output.getDescription().equals(""));
                require.using("The output state of BasketOfApples should have a non-zero weight",output.getWeight()>0);
                return null;
            });
        }else if(commandData instanceof BasketOfApplesContract.Commands.Redeem){ //intention 2
            AppleStamp input = tx.inputsOfType(AppleStamp.class).get(0);
            BasketOfApples output = tx.outputsOfType(BasketOfApples.class).get(0);

            requireThat(require->{
                require.using("This transaction should consume two states",tx.getInputStates().size() ==2);
                require.using("The issuer of the Apple Stamp should be the producing farm of this basket of apple",input.getIssuer().equals(output.getFarm()));
                require.using("The basket of apple should have weight more than 1",output.getWeight()>0);
                return null;
            });
        }else{
            throw new IllegalArgumentException("Incorrect type of BasketOfApples Commands");
        }

    }

    //indicate transaction's intent
    public interface Commands extends CommandData {
        class packToBasket implements BasketOfApplesContract.Commands{}
        class Redeem implements BasketOfApplesContract.Commands{}
    }


}
