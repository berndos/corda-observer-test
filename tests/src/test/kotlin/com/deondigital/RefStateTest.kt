package com.deondigital

import com.deondigital.flows.IssueTestStateFlow
import com.deondigital.flows.UpdateTestStateFlow
import com.deondigital.states.TestContract
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Test

class RefStateTest {

    @Test
    fun test() {
        driver(DriverParameters(
                startNodesInProcess = true,
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.deondigital.states"),
                        TestCordapp.findCordapp("com.deondigital.flows")
                )

        )) {
            val alice = startNode(providedName = CordaX500Name("Alice", "Zurich", "CH")).getOrThrow()
            val bob = startNode(providedName = CordaX500Name("Bob", "Zurich", "CH")).getOrThrow()
            val notary = alice.rpc.notaryIdentities().first()

            val aliceParty = alice.nodeInfo.legalIdentities.first()
            val bobParty = bob.nodeInfo.legalIdentities.first()


            val s1 = alice.rpc.startFlowDynamic(IssueTestStateFlow::class.java, listOf(aliceParty, bobParty), notary, "s1").returnValue.getOrThrow()
            println("s1=$s1")
            val s2 = alice.rpc.startFlowDynamic(IssueTestStateFlow::class.java, listOf(aliceParty), notary, "s2").returnValue.getOrThrow()
            println("s2=$s2")
            val s3 = alice.rpc.startFlowDynamic(UpdateTestStateFlow::class.java, s2, s1, listOf(aliceParty), "s3").returnValue.getOrThrow()
            println("s3=$s3")
            val s4 = alice.rpc.startFlowDynamic(UpdateTestStateFlow::class.java, s1, null, listOf(aliceParty, bobParty), "s4").returnValue.getOrThrow()
            println("s4=$s4")
            val s5 = alice.rpc.startFlowDynamic(UpdateTestStateFlow::class.java, s3, null, listOf(aliceParty,bobParty), "s5").returnValue.getOrThrow()
            println("s5=$s5")

            val bobStates = bob.rpc.vaultQueryBy<TestContract.TestState>().states.map { it.ref }
            assert(s4.ref in bobStates)
            assert(s5.ref in bobStates)

        }
    }

}