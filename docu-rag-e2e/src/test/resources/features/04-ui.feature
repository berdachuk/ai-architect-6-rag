Feature: DocuRAG UI smoke

  @ui
  Scenario Outline: Key pages show disclaimer
    Given I open the UI path "<path>"
    Then the page shows the medical disclaimer fragment
    And the title contains "DocuRAG"

    Examples:
      | path         |
      | /            |
      | /qa          |
      | /documents   |
      | /analysis    |
      | /evaluation  |

  @ui
  Scenario: QA form submit shows an answer
    Given I open the UI path "/qa"
    When I submit the question "What is hypertension?" on the QA page
    Then the QA page shows an answer heading
