package com.template.contracts;

import com.template.states.AppleStamp;
import com.template.states.TemplateState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import java.util.List;

// ************
// * Contract *
// ************
public class AppleStampContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.AppleStampContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a single transaction.*/
        //final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        //Extract the command from the transaction.
        final CommandData commandData = tx.getCommands().get(0).getValue();


        //Verify the transaction according to the intention of the transaction
        if (commandData instanceof Commands.Issue) {
            //Retrieve the output state of the transaction
            AppleStamp output = tx.outputsOfType(AppleStamp.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("This transaction should only output one AppleStamp state", tx.getOutputs().size() == 1);
                require.using("The output AppleStamp state should have clear description of the type of redeemable goods", !output.getStampDesc().equals(""));
                return null;
            });
        }else if(commandData instanceof BasketOfApplesContract.Commands.Redeem){
            //Transaction verification will happen in BasketOfApples Contract
        }else{
            throw new IllegalArgumentException("Incorrect type of AppleStamp Commands");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will only have one command.
        class Issue implements AppleStampContract.Commands {}
    }
}