package com.deondigital.flows

import co.paralleluniverse.fibers.Suspendable
import com.deondigital.states.TestContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
abstract class FinalityInitiator<T>: FlowLogic<T>()

@InitiatedBy(FinalityInitiator::class)
class FinalityReceiver(val otherSideSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val stx = subFlow(ReceiveFinalityFlow(otherSideSession = otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        println("received ${stx.tx}")
        return stx
    }
}

@StartableByRPC
class IssueTestStateFlow(val participants: List<Party>, val notary: Party, val s: String): FinalityInitiator<StateAndRef<TestContract.TestState>>() {
    @Suspendable
    override fun call(): StateAndRef<TestContract.TestState> {
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addOutputState(TestContract.TestState(s, participants))
        txBuilder.addCommand(TestContract.TestCommand(), ourIdentity.owningKey)
        val sessions = (participants - ourIdentity).map(::initiateFlow)
        val finalizedTx = subFlow(FinalityFlow(serviceHub.signInitialTransaction(txBuilder), sessions))
        return finalizedTx.tx.outRef(0)
    }

}

@StartableByRPC
class UpdateTestStateFlow(val stateAndRef: StateAndRef<TestContract.TestState>,
                          val refState: StateAndRef<TestContract.TestState>?,
                          val participants: List<Party>,
                          val s: String): FinalityInitiator<StateAndRef<TestContract.TestState>>() {
    @Suspendable
    override fun call(): StateAndRef<TestContract.TestState> {
        val txBuilder = TransactionBuilder(stateAndRef.state.notary)
        txBuilder.addInputState(stateAndRef)
        refState?.apply { txBuilder.addReferenceState(referenced()) }
        txBuilder.addOutputState(TestContract.TestState(s, participants))
        txBuilder.addCommand(TestContract.TestCommand(), ourIdentity.owningKey)
        val sessions = ((stateAndRef.state.data.participants.map { it as Party } + participants).toSet() - ourIdentity)
                .map(::initiateFlow)
        val finalizedTx = subFlow(FinalityFlow(serviceHub.signInitialTransaction(txBuilder), sessions))
        return finalizedTx.tx.outRef(0)
    }

}