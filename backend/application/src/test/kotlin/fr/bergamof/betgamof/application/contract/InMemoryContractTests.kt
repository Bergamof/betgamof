package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.application.fake.InMemoryPersistence

// The in-memory fakes must behave like the real database: they run the same contract suites.

class InMemoryBankrollRepositoryTest : BankrollRepositoryContract() {
    override fun newPersistence() = InMemoryPersistence()
}

class InMemoryBetRepositoryTest : BetRepositoryContract() {
    override fun newPersistence() = InMemoryPersistence()
}

class InMemoryMontanteRepositoryTest : MontanteRepositoryContract() {
    override fun newPersistence() = InMemoryPersistence()
}

class InMemoryRuleRepositoryTest : RuleRepositoryContract() {
    override fun newPersistence() = InMemoryPersistence()
}

class InMemoryTransactionRunnerTest : TransactionRunnerContract() {
    override fun newPersistence() = InMemoryPersistence()
}
