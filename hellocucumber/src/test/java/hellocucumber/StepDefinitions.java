package hellocucumber;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class StepDefinitions {

    @Given("an example scenario")
    public void anExampleScenario() {
    }

    @When("all step definitions are implemented")
    public void allStepDefinitionsAreImplemented() {
    }

    @Then("the scenario passes")
    public void theScenarioPasses() {
    }

    /// ///////////////////////////////////////////
    private Account account;
    private int withdrawAmount;
    private final BankService bankService = mock(BankService.class);
    private boolean withdrawSuccess;

    @Given("口座残高が{int}円である")
    public void givenInitialBalance(int initialBalance) {
        account = spy(new Account(initialBalance, bankService));
    }

    @Given("引き出し金額が{int}円である")
    public void givenWithdrawAmount(int amount) {
        withdrawAmount = amount;
    }

    @When("お金を引き出す")
    public void when() {
        try {
            account.withdraw(withdrawAmount);
            withdrawSuccess = true;
        } catch (Exception e) {
            withdrawSuccess = false;
        }
    }

    @Then("口座残高は{int}円になる")
    public void thenBalance(int expectedBalance) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        assertThat(account.getBalance()).isEqualTo(expectedBalance);
        assertThat(withdrawSuccess).isTrue();
        System.out.printf("#### OK?%n");

        Method addAmount = Account.class.getDeclaredMethod("addAmount", int.class);
        addAmount.setAccessible(true);
        addAmount.invoke(account, 1000);
        assertThat(account.getBalance()).isEqualTo(expectedBalance + 1000);
        System.out.printf("#### OK? 2%n");

    }

    @Then("銀行サービスは引き出し処理を{int}回呼び出す")
    public void thenVerifyCall(int times) {
        verify(bankService, times(times)).withdraw(account, withdrawAmount);
    }

    @Then("エンコード結果が等しい")
    public void matchEncodeValues(@NotNull DataTable dataTable) {
        for (var row : dataTable.asMaps()) {
            var actualCharset = Charset.forName(row.get("actual"));
            var expectedCharset = Charset.forName(row.get("expected"));

            IntStream.rangeClosed(0x00, 0x7f)
                    .forEach(codepoint -> {
                        var asString = Character.isISOControl(codepoint)? "(SpecialCharacter)": String.format("%c", codepoint);
                        var chars = Character.toChars(codepoint);
                        assertThat(actualCharset.encode(CharBuffer.wrap(chars)))
                                .as(() -> String.format("U+%04X: %s", codepoint, asString))
                                .isEqualTo(expectedCharset.encode(CharBuffer.wrap(chars)));
                    });
        }
    }

    @Then("デコード結果が等しい")
    public void matchDecodeValues(@NotNull DataTable dataTable) {
        for (var row : dataTable.asMaps()) {
            var actualCharset = Charset.forName(row.get("actual"));
            var expectedCharset = Charset.forName(row.get("expected"));

            IntStream.rangeClosed(0x00, 0x7f)
                    .forEach(codepoint -> {
                        var asString = Character.isISOControl(codepoint)? "(SpecialCharacter)": String.format("%c", codepoint);
                        var bytes = new byte[] {(byte) codepoint};
                        var actual = actualCharset.decode(ByteBuffer.wrap(bytes));
                        var expected = expectedCharset.decode(ByteBuffer.wrap(bytes));
                        assertThat(actual.array())
                                .as(() -> String.format("U+%04X: %s", codepoint, asString))
                                .isEqualTo(expected.array());
                    });
        }
    }

}
