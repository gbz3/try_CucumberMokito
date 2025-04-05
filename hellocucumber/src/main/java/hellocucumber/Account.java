package hellocucumber;

public class Account {
    private int balance;
    private final BankService bankService;

    public Account(int balance, BankService bankService) {
        this.balance = balance;
        this.bankService = bankService;
    }

    public void withdraw(int amount) {
        if (amount <= 0 || balance < amount) {
            throw new IllegalArgumentException("Invalid withdrawal amount.");
        }
        bankService.withdraw(this, amount);
        this.balance -= amount;
        addAmount(0);
    }

    public int getBalance() {
        return balance;
    }

    private void addAmount(int amount) {
        this.balance += amount;
    }

}
