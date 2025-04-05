Feature: An example

  Scenario: The example
    Given an example scenario
    When all step definitions are implemented
    Then the scenario passes

  Scenario: 口座からお金を引き出す
    Given 口座残高が1000円である
    And 引き出し金額が500円である
    When お金を引き出す
    Then 口座残高は500円になる
    And 銀行サービスは引き出し処理を1回呼び出す

