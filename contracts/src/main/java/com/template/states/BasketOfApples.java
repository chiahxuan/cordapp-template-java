package com.template.states;

import com.template.contracts.BasketOfApplesContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(BasketOfApplesContract.class)
public class BasketOfApples implements ContractState {

    private String description;// Brand or type
    private Party farm; //Origin of the apple
    private Party owner; // The person who exchange the basket of apple with the stamp
    private int weight;

    //Parameter required by all Corda states to indicate storing parties
    private List<AbstractParty> participants;

    //Constructors
    //Basket of Apple creation. Only farm name is stored.
    public BasketOfApples(String description, Party farm, int weight) {
        this.description = description;
        this.farm = farm;
        this.owner=farm;
        this.weight = weight;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(farm);
    }

    //Constructor for object creation during transaction
    @ConstructorForDeserialization
    public BasketOfApples(String description, Party farm, Party owner, int weight) {
        this.description = description;
        this.farm = farm;
        this.owner = owner;
        this.weight = weight;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(farm);
        this.participants.add(owner);
    }


    public String getDescription() {
        return description;
    }

    public Party getFarm() {
        return farm;
    }

    public Party getOwner() {
        return owner;
    }

    public int getWeight() {
        return weight;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

    public BasketOfApples changeOwner(Party buyer){
        BasketOfApples newOwnerState = new BasketOfApples(this.description, this.farm, buyer, this.weight);
        return newOwnerState;
    }
}
