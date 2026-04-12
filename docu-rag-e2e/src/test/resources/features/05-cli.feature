Feature: Evaluation CLI

  @cli
  Scenario: Eval CLI jar runs against the same database
    When I run the eval-cli jar for dataset "medical-rag-eval-v1"
    Then the eval-cli process exits with code 0
    And the latest evaluation API returns a run id
