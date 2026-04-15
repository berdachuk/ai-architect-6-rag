@ui
Feature: DocuRAG UI use cases

  # Runs after 02-api.feature (lexical order): DB already has ingested docs, index, and an eval run.

  Scenario: Dashboard shows index stats
    Given I open the UI path "/"
    Then the dashboard shows index section with document stats

  Scenario: Documents page lists ingested documents
    Given I open the UI path "/documents"
    Then the documents table has at least 1 row
    And the page body contains text "Total:"

  Scenario: Documents ingest configured paths via UI
    Given I open the UI path "/documents"
    When I submit ingest configured paths on the documents page
    Then the documents page shows ingest job completed successfully

  Scenario: Analysis page loads category pie and entity graph
    When I open the analysis page waiting for visualization APIs
    Then the pie chart area is visible
    And the entity graph shows a canvas

  Scenario: Evaluation form run shows metrics
    Given I open the UI path "/evaluation"
    When I run evaluation from the UI for dataset "medical-rag-eval-v1"
    Then the evaluation page shows run result with normalized accuracy

  Scenario: QA form shows non-empty answer body
    Given I open the UI path "/qa"
    When I submit the question "What is hypertension?" on the QA page
    Then the QA page shows an answer heading
    And the QA answer body is not empty

  Scenario: Nav link from dashboard opens documents
    Given I open the UI path "/"
    When I follow the nav link "Documents"
    Then the browser path ends with "/documents"
