Feature: DocuRAG REST API

  @api
  Scenario: Ingest, index, RAG, visualizations, and evaluation
    Given actuator health is UP
    When I ingest the sample JSONL fixture
    And I ingest the tiny PDF fixture
    And I list documents and remember the first id
    Then GET document by id returns detail
    And document categories are available
    When I trigger a full index rebuild
    Then embedded chunks become available within 3 minutes
    And index status shows indexed content
    When I POST incremental index expecting not implemented
    When I ask RAG "What is hypertension?"
    Then the RAG response has an answer and retrieved chunks
    And the RAG answer body contains "Hypertension"
    When I run document analysis with defaults
    Then the analysis response is present
    When I fetch category pie visualization
    Then the pie response has chart data
    When I fetch entity graph visualization
    Then the graph response has structure
    When I run evaluation for dataset "medical-rag-eval-v1"
    Then an evaluation run id is stored
    When I list evaluation runs
    Then the stored run appears in the list
    When I fetch evaluation run detail for the stored id
    And I fetch latest evaluation with a valid id
    When I open the dashboard page
    Then the dashboard HTML contains "Dashboard"
