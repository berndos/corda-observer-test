package com.deondigital.states

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction


class TestContract : Contract {

    class TestCommand: CommandData

    data class TestState(val s: String, override val participants: List<AbstractParty>): ContractState

    override fun verify(tx: LedgerTransaction) {

    }



}
